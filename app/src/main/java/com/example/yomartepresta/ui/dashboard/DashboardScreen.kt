package com.example.yomartepresta.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.yomartepresta.data.model.Loan
import com.example.yomartepresta.data.model.toDisplayDate
import com.example.yomartepresta.ui.MainViewModel
import com.example.yomartepresta.ui.components.GlassCard
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onRequestLoan: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    onPayLoan: (Loan) -> Unit = {},
    onEditProfile: () -> Unit = {}
) {
    val user by viewModel.user.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    val availableLimit = viewModel.getAvailableLimit(user?.level ?: 1, loans)
    val isVerified = user?.idVerificationStatus == "approved"
    
    val paidCount = loans.count { it.status == "paid" }
    val progressInLevel = paidCount % 2
    
    var showImageDialogUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            DashboardTopBar(
                userName = user?.firstName ?: "Yomar",
                showAdmin = isAdmin,
                onAdminClick = onNavigateToAdmin,
                onLogoutClick = {
                    viewModel.logout()
                    onLogout()
                },
                onProfileClick = onEditProfile
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isAdmin) {
                AdminRedirectScreen(
                    padding = padding, 
                    onNavigateToAdmin = onNavigateToAdmin,
                    bcvRate = settings?.bcvRate ?: 0.0
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = padding.calculateTopPadding() + 10.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { HeaderSection(user?.firstName ?: "Yomar", user?.level ?: 1, viewModel.calculateLoanLimit(user?.level ?: 1), onRequestLoan, isVerified) }
                    item { StatsGrid(settings?.bcvRate ?: 0.0, availableLimit, "$progressInLevel/2") }
                    item { InfoBanner(if (isVerified) "¡Paga 2 vales más para subir de nivel!" else "Tu cuenta está en proceso de verificación.") }
                    item { Text("Tus Vales", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) }
                    
                    if (!isVerified) {
                        item { InfoBanner("Debes esperar a que el administrador verifique tu identidad.") }
                    } else if (loans.isEmpty()) {
                        item { Text("No tienes vales activos o pendientes.", color = GraySmoke) }
                    } else {
                        items(loans) { loan ->
                            LoanCard(loan, settings?.bcvRate ?: 1.0, { onPayLoan(loan) }, { showImageDialogUrl = it })
                        }
                    }
                }
            }

            showImageDialogUrl?.let { url ->
                ImageDialog(url = url, onDismiss = { showImageDialogUrl = null })
            }
        }
    }
}

@Composable
fun AdminRedirectScreen(padding: PaddingValues, onNavigateToAdmin: () -> Unit, bcvRate: Double) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AdminPanelSettings, null, tint = GoldMate, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("MODO ADMINISTRADOR", color = GoldMate, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNavigateToAdmin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldMate)
        ) { Text("IR AL CONTROL MAESTRO", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DashboardTopBar(userName: String, showAdmin: Boolean, onAdminClick: () -> Unit, onLogoutClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(color = Color.Black, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Yomar te Presta", color = GoldMate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showAdmin) {
                    Icon(Icons.Default.AdminPanelSettings, "Admin", tint = GraySmoke, modifier = Modifier.size(20.dp).clickable { onAdminClick() })
                }
                Icon(Icons.Default.Person, "Perfil", tint = GraySmoke, modifier = Modifier.size(20.dp).clickable { onProfileClick() })
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = GraySmoke, modifier = Modifier.size(20.dp).clickable { onLogoutClick() })
            }
        }
    }
}

@Composable
fun ImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.Black, RoundedCornerShape(16.dp)).border(1.dp, GoldMate.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = url, contentDescription = "Comprobante", modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GoldMate)) { Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun HeaderSection(userName: String, level: Int, totalLimit: Double, onRequestLoan: () -> Unit, canRequest: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Hola, $userName", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("Nivel $level • Límite Total: $$totalLimit", style = MaterialTheme.typography.bodyMedium, color = GraySmoke)
            }
            Button(
                onClick = onRequestLoan,
                colors = ButtonDefaults.buttonColors(containerColor = if (canRequest) GoldMate else Color(0xFF222222)),
                enabled = canRequest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Solicitar Vale", color = if (canRequest) Color.Black else GraySmoke, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatsGrid(bcvRate: Double, availableLimit: Double, nextLevelProgress: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBox(Modifier.weight(1f), "TASA BCV", "${"%.2f".format(bcvRate)} Bs.")
        StatBox(Modifier.weight(1f), "DISPONIBLE", "$${"%.2f".format(availableLimit)}", true)
        StatBox(Modifier.weight(0.8f), "NIVEL", nextLevelProgress)
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, label: String, value: String, isHighlighted: Boolean = false) {
    Box(modifier = modifier.height(90.dp).background(Color(0xFF111111), RoundedCornerShape(12.dp)).padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = GraySmoke, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = if (isHighlighted) GoldMate else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun InfoBanner(message: String) {
    Box(modifier = Modifier.fillMaxWidth().border(1.dp, GoldMate.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).background(GoldMate.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsNone, null, tint = GoldMate, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = message, color = GoldMate.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LoanCard(loan: Loan, bcvRate: Double, onPayClick: () -> Unit, onViewImage: (String) -> Unit) {
    val dueDateStr = loan.dueDate.toDisplayDate()
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SALDO RESTANTE", style = MaterialTheme.typography.labelSmall, color = GraySmoke, fontSize = 10.sp)
                    Text("$${"%.2f".format(loan.totalToPayUsd)}", style = MaterialTheme.typography.headlineSmall, color = GoldMate, fontWeight = FontWeight.Bold)
                }
                StatusBadge(loan.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Vence: $dueDateStr", style = MaterialTheme.typography.labelSmall, color = GraySmoke)
                    Text("≈ Bs. ${"%.2f".format(loan.totalToPayUsd * bcvRate)}", style = MaterialTheme.typography.labelSmall, color = GraySmoke)
                }
                if (loan.status == "approved" || loan.status == "overdue") {
                    Button(onClick = onPayClick, colors = ButtonDefaults.buttonColors(containerColor = GoldMate), shape = RoundedCornerShape(8.dp)) {
                        Text("PAGAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "approved" -> Color(0xFF4CAF50) to "APROBADO"
        "pending" -> Color(0xFFFFC107) to "PENDIENTE"
        "paid" -> Color(0xFF2196F3) to "PAGADO"
        "overdue" -> Color(0xFFF44336) to "VENCIDO"
        "repayment_pending" -> Color(0xFF9C27B0) to "VERIFICANDO PAGO"
        "rejected" -> Color.Red to "RECHAZADO"
        else -> GraySmoke to status.uppercase()
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
