package com.example.yomartepresta.ui.payment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.yomartepresta.data.model.Loan
import com.example.yomartepresta.ui.MainViewModel
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke
import kotlinx.coroutines.launch

@Composable
fun PaymentScreen(loan: Loan, viewModel: MainViewModel, onBack: () -> Unit) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var amountInput by remember { mutableStateOf(loan.totalToPayUsd.toString()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    val bcvRate = settings?.bcvRate ?: 1.0
    val amountUsd = amountInput.toDoubleOrNull() ?: 0.0
    val amountBs = amountUsd * bcvRate
    val remaining = (loan.totalToPayUsd - amountUsd).coerceAtLeast(0.0)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color.Black) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = GoldMate)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Registrar Pago",
                        color = GoldMate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "DETALLES DE TRANSFERENCIA",
                style = MaterialTheme.typography.labelSmall,
                color = GraySmoke,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentDetailRow("Banco", settings?.adminBank ?: "N/A")
                    PaymentDetailRow("Titular", settings?.adminName ?: "N/A")
                    PaymentDetailRow("Cédula/RIF", settings?.adminId ?: "N/A")
                    PaymentDetailRow("Teléfono", settings?.adminPhone ?: "N/A")
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Monto a abonar (USD)", color = GraySmoke) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldMate,
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text("$", color = GoldMate) }
                    )

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL EN BS.", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                            Text("Bs. ${"%.2f".format(amountBs)}", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        
                        if (remaining > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Saldo restante tras este pago: $${"%.2f".format(remaining)}",
                                color = GoldMate,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COMPROBANTE DE PAGO", color = GraySmoke, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF050505))
                        .border(1.dp, if (error != null && selectedImageUri == null) Color.Red else Color(0xFF222222), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else {
                        Text("SUBIR CAPTURA", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                    }
                }
                error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
            }

            Button(
                onClick = {
                    if (selectedImageUri != null && amountUsd > 0 && amountUsd <= loan.totalToPayUsd) {
                        isLoading = true
                        error = null
                        scope.launch {
                            try {
                                viewModel.reportPayment(loan.id, selectedImageUri!!, amountUsd)
                                onBack()
                            } catch (e: Exception) {
                                error = "Error: ${e.message}"
                                isLoading = false
                            }
                        }
                    } else {
                        error = if (selectedImageUri == null) "Falta el comprobante" else "Monto inválido"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GoldMate),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("REPORTAR ABONO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PaymentDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GraySmoke, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
