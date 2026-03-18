package com.example.yomartepresta.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.yomartepresta.data.model.User
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    onBack: () -> Unit,
    repository: FirebaseRepository = FirebaseRepository(),
    isAdminEdit: Boolean = false
) {
    val userKey = user.uid 
    
    var firstName by remember(userKey) { mutableStateOf(user.firstName) }
    var lastName by remember(userKey) { mutableStateOf(user.lastName) }
    var pagoMovilId by remember(userKey) { mutableStateOf(user.pagoMovilId) }
    var pagoMovilBank by remember(userKey) { mutableStateOf(user.pagoMovilBank) }
    var pagoMovilPhone by remember(userKey) { mutableStateOf(user.pagoMovilPhone) }
    
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(if (isAdminEdit) "Editar Usuario" else "Mi Perfil", color = GoldMate) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = GoldMate) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileField("Nombre", firstName) { firstName = it }
            ProfileField("Apellido", lastName) { lastName = it }
            ProfileField("Pago Móvil ID", pagoMovilId) { pagoMovilId = it }
            ProfileField("Banco Pago Móvil", pagoMovilBank) { pagoMovilBank = it }
            ProfileField("Teléfono Pago Móvil", pagoMovilPhone, keyboardType = KeyboardType.Phone) { 
                if (it.all { char -> char.isDigit() || char == '+' || char == ' ' }) pagoMovilPhone = it 
            }

            error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isSaving = true
                    error = null
                    scope.launch {
                        val updatedUser = user.copy(
                            firstName = firstName, lastName = lastName,
                            pagoMovilId = pagoMovilId, pagoMovilBank = pagoMovilBank, 
                            pagoMovilPhone = pagoMovilPhone
                        )
                        repository.saveUserProfile(updatedUser)
                        isSaving = false
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldMate),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "GUARDANDO..." else "GUARDAR CAMBIOS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String, 
    value: String, 
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GraySmoke) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldMate,
            unfocusedBorderColor = Color(0xFF222222),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
