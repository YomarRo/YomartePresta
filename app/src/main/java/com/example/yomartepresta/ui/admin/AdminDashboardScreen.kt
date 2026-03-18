package com.example.yomartepresta.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.yomartepresta.data.model.Loan
import com.example.yomartepresta.data.model.User
import com.example.yomartepresta.data.model.toDisplayDate
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.example.yomartepresta.ui.components.GlassCard
import com.example.yomartepresta.ui.dashboard.StatusBadge
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit = {},
    onEditUser: (String) -> Unit = {},
    repository: FirebaseRepository = FirebaseRepository()
) {
    val scope = rememberCoroutineScope()
    val allLoans by repository.getAllLoansFlow().collectAsState(initial = emptyList())
    
    val pendingLoans = allLoans.filter { it.status == "pending" }
    
    val paymentsToVerify by remember { 
        repository.getAllLoansFlow().map { loans -> loans.filter { it.status == "repayment_pending" } } 
    }.collectAsState(initial = emptyList())

    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val settings by repository.getSettingsFlow().collectAsState(initial = null)
    
    var bcvInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showImageDialogUrl by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = GoldMate) }
                    Spacer(Modifier.width(8.dp))
                    Text("Control Maestro", color = GoldMate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = GoldMate,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = GoldMate)
                }
            ) {
                listOf("VALES", "PAGOS", "HISTORIAL", "TASAS", "USUARIOS").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                        Text(title, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                when (selectedTab) {
                    0 -> { // VALES PENDIENTES
                        item { AdminTabTitle("SOLICITUDES POR APROBAR") }
                        items(pendingLoans) { loan ->
                            AdminLoanCard(loan, 
                                onApprove = { scope.launch { repository.updateLoanStatus(loan.id, "approved") } }, 
                                onReject = { scope.launch { repository.updateLoanStatus(loan.id, "rejected") } }
                            )
                        }
                    }
                    1 -> { // DEUDAS ACTIVAS Y PAGOS
                        item { AdminTabTitle("DEUDAS ACTIVAS Y PAGOS") }
                        val activeLoans = allLoans.filter { it.status == "approved" || it.status == "overdue" || it.status == "repayment_pending" }
                        if (activeLoans.isEmpty()) item { EmptyStateAdmin("No hay deudas activas") }
                        else items(activeLoans) { loan ->
                            AdminInfoCard(loan, isVerifyTab = (loan.status == "repayment_pending"), onConfirm = { scope.launch { repository.updateLoanStatus(loan.id, "paid", loan) } }, onReject = { scope.launch { repository.updateLoanStatus(loan.id, "approved") } }, onViewDoc = { showImageDialogUrl = it })
                        }
                    }
                    2 -> { // HISTORIAL
                        item { AdminTabTitle("HISTORIAL COMPLETO") }
                        items(allLoans.filter { it.status == "paid" || it.status == "rejected" }) { loan -> HistoryLoanCard(loan) }
                    }
                    3 -> { // TASAS
                        item { AdminTabTitle("TASA BCV") }
                        item { 
                            GlassCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AdminInputField(value = bcvInput, onValueChange = { bcvInput = it }, label = "Tasa Actual: ${settings?.bcvRate} Bs.")
                                    Button(onClick = { bcvInput.toDoubleOrNull()?.let { scope.launch { repository.updateBcvRate(it) }; bcvInput = "" } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldMate)) {
                                        Text("ACTUALIZAR", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // USUARIOS
                        item { AdminTabTitle("USUARIOS") }
                        items(allUsers) { user -> 
                            UserCardAdmin(
                                user = user, 
                                onVerify = { scope.launch { repository.updateUserVerification(user.uid, "approved") } }, 
                                onReject = { scope.launch { repository.updateUserVerification(user.uid, "rejected") } },
                                onViewDoc = { showImageDialogUrl = it },
                                onEdit = { onEditUser(user.uid) }
                            ) 
                        }
                    }
                }
            }
        }
    }
    showImageDialogUrl?.let { url -> ImageDialog(url = url, onDismiss = { showImageDialogUrl = null }) }
}

@Composable
fun AdminInfoCard(loan: Loan, isVerifyTab: Boolean, onConfirm: () -> Unit = {}, onReject: () -> Unit = {}, onViewDoc: (String) -> Unit = {}) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(loan.userName, color = Color.White, fontWeight = FontWeight.Bold)
                StatusBadge(loan.status)
            }
            Text("Vencimiento: ${loan.dueDate}", color = GraySmoke, fontSize = 12.sp)
            if (isVerifyTab && !loan.repaymentCaptureUrl.isNullOrEmpty()) {
                IconButton(onClick = { onViewDoc(loan.repaymentCaptureUrl!!) }) { Icon(Icons.Default.Visibility, "Ver Pago", tint = GoldMate) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("INVALIDAR", color = Color.Red, fontSize = 10.sp) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("CONFIRMAR", fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
fun HistoryLoanCard(loan: Loan) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(loan.userName, color = Color.White, fontWeight = FontWeight.Bold)
                StatusBadge(loan.status)
            }
            Text("Creado: ${loan.createdAt.toDisplayDate()} | Vence: ${loan.dueDate}", color = GraySmoke, fontSize = 10.sp)
        }
    }
}

@Composable
fun AdminTabTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelSmall, color = GraySmoke, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun UserCardAdmin(user: User, onVerify: () -> Unit, onReject: () -> Unit, onViewDoc: (String) -> Unit, onEdit: () -> Unit) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${user.firstName} ${user.lastName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            !user.cedula.isNullOrBlank() -> "C.I: ${user.cedula}"
                            !user.pagoMovilId.isNullOrBlank() -> "Pago Móvil: ${user.pagoMovilId}"
                            else -> "Sin ID registrado"
                        },
                        color = GoldMate, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
                Row {
                    if (!user.idDocumentUrl.isNullOrEmpty()) {
                        IconButton(onClick = { onViewDoc(user.idDocumentUrl!!) }) { Icon(Icons.Default.Visibility, "Ver ID", tint = GoldMate) }
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = GraySmoke) }
                }
            }
            if (user.idVerificationStatus == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("RECHAZAR", color = Color.Red, fontSize = 11.sp) }
                    Button(onClick = onVerify, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("APROBAR", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
fun AdminLoanCard(loan: Loan, onApprove: () -> Unit, onReject: () -> Unit) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(loan.userName, color = Color.White, fontWeight = FontWeight.Bold)
            Text("$${loan.amountUsd}", color = GoldMate, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("NO", color = Color.Red, fontSize = 11.sp) }
                Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("SÍ", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
fun AdminInputField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label, color = GraySmoke) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldMate, focusedTextColor = Color.White))
}

@Composable
fun EmptyStateAdmin(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { Text(message, color = GraySmoke) }
}

@Composable
fun ImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.Black, RoundedCornerShape(16.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
            AsyncImage(model = url, contentDescription = "Imagen", modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
        }
    }
}
