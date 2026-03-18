package com.example.yomartepresta.ui.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yomartepresta.ui.MainViewModel
import com.example.yomartepresta.ui.components.SignatureCanvas
import com.example.yomartepresta.ui.components.createSignatureBitmap
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LoanRequestScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val loans by viewModel.loans.collectAsState()
    
    val availableLimit = viewModel.getAvailableLimit(user?.level ?: 1, loans).toFloat()
    
    var amountUsd by remember { mutableFloatStateOf(10f) }
    var step by remember { mutableIntStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = GoldMate)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Solicitar Vale",
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
        ) {
            if (step == 1) {
                AmountSelector(
                    amount = amountUsd,
                    limit = availableLimit,
                    bcvRate = settings?.bcvRate ?: 1.0,
                    onAmountChange = { amountUsd = it },
                    onNext = { step = 2 }
                )
            } else {
                ContractSignatureScreen(
                    amountUsd = amountUsd.toDouble(),
                    isLoading = isSubmitting,
                    onComplete = { signatureBytes ->
                        isSubmitting = true
                        scope.launch {
                            try {
                                viewModel.requestLoan(amountUsd.toDouble(), signatureBytes)
                                onBack()
                            } catch (e: Exception) {
                                // Error handling
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AmountSelector(
    amount: Float,
    limit: Float,
    bcvRate: Double,
    onAmountChange: (Float) -> Unit,
    onNext: () -> Unit
) {
    var termsAccepted by remember { mutableStateOf(false) } // Requerido para el Test de QA

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111), RoundedCornerShape(24.dp))
                .border(1.dp, GoldMate.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MONTO A SOLICITAR",
                    color = GraySmoke,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$${amount.roundToInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = GoldMate,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.testTag("amount_text")
                )
                Text(
                    text = "≈ Bs. ${"%.2f".format(amount.roundToInt() * bcvRate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GraySmoke,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (limit > 10f) {
                    Slider(
                        value = amount.coerceIn(10f, limit),
                        onValueChange = { onAmountChange(it.roundToInt().toFloat()) },
                        valueRange = 10f..limit,
                        steps = (limit - 10).toInt() - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldMate,
                            activeTrackColor = GoldMate,
                            inactiveTrackColor = Color(0xFF222222)
                        ),
                        modifier = Modifier.testTag("amount_slider")
                    )
                } else if (limit == 10f) {
                    Text("Límite máximo disponible: $10", color = GoldMate, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Has alcanzado tu límite de crédito", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$10", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                    Text("Disponible: $${limit.toInt()}", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Checkbox de Términos (Requerimiento de QA)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(checkedColor = GoldMate, uncheckedColor = GraySmoke),
                modifier = Modifier.testTag("terms_checkbox")
            )
            Text(
                "Acepto los términos y condiciones del contrato digital.",
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("next_step_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldMate,
                disabledContainerColor = Color(0xFF222222)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = limit >= 10f && termsAccepted // Lógica de habilitación probada en test
        ) {
            Text("CONTINUAR AL CONTRATO", color = if (termsAccepted) Color.Black else GraySmoke, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ContractSignatureScreen(
    amountUsd: Double,
    isLoading: Boolean,
    onComplete: (ByteArray) -> Unit
) {
    val points = remember { mutableStateListOf<Offset?>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "CONTRATO DIGITAL",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Al firmar este documento, el empleado se compromete a pagar la suma de $${"%.2f".format(amountUsd)} " +
                        "más un 15% de interés fijo. El pago debe realizarse antes de la fecha de vencimiento " +
                        "estipulada. El incumplimiento generará una mora del 5% semanal.",
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 24.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "FIRMA DIGITAL REQUERIDA",
                color = GoldMate,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, GraySmoke, RoundedCornerShape(12.dp))
                    .onGloballyPositioned { canvasSize = it.size }
                    .testTag("signature_canvas")
            ) {
                SignatureCanvas(
                    modifier = Modifier.fillMaxSize(),
                    points = points
                )
            }
        }

        Button(
            onClick = {
                val bitmapBytes = createSignatureBitmap(points, canvasSize.width, canvasSize.height)
                if (bitmapBytes != null) {
                    onComplete(bitmapBytes)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_loan_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GoldMate),
            shape = RoundedCornerShape(12.dp),
            enabled = points.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("FIRMAR Y SOLICITAR VALE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}
