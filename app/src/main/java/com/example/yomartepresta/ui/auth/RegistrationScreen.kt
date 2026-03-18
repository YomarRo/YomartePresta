package com.example.yomartepresta.ui.auth

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.yomartepresta.data.model.User
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.example.yomartepresta.ui.theme.GoldMate
import com.example.yomartepresta.ui.theme.GraySmoke
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    initialEmail: String = "",
    initialName: String = "",
    isGoogleAuth: Boolean = false,
    onRegistrationSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf(initialName) }
    var lastName by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val repository = FirebaseRepository()
    val auth = FirebaseAuth.getInstance()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ajuste para evitar el recorte superior
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Yomar te Presta",
                style = MaterialTheme.typography.headlineMedium,
                color = GoldMate,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isGoogleAuth) "COMPLETAR PERFIL" else "SOLICITAR ACCESO",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isGoogleAuth) {
                        CustomTextField(value = email, onValueChange = { email = it }, label = "Email Corporativo")
                        CustomTextField(value = password, onValueChange = { password = it }, label = "Contraseña", isPassword = true)
                    }
                    
                    CustomTextField(value = firstName, onValueChange = { firstName = it }, label = "Nombre")
                    CustomTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                    CustomTextField(value = cedula, onValueChange = { cedula = it }, label = "Cédula de Identidad")
                    CustomTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono Pago Móvil")
                    CustomTextField(value = bank, onValueChange = { bank = it }, label = "Banco")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("CÉDULA DE IDENTIDAD (CAPTURA)", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF050505))
                            .border(1.dp, if (error != null && selectedImageUri == null) Color.Red else Color(0xFF222222), RoundedCornerShape(12.dp))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Cédula",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("SUBIR DOCUMENTO", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }

                    Button(
                        onClick = {
                            if (firstName.isNotEmpty() && cedula.isNotEmpty() && phone.isNotEmpty() && selectedImageUri != null) {
                                isLoading = true
                                val uid = auth.currentUser?.uid
                                
                                if (isGoogleAuth && uid != null) {
                                    scope.launch {
                                        try {
                                            val downloadUrl = repository.uploadIdImage(uid, selectedImageUri!!)
                                            val user = User(
                                                uid = uid,
                                                firstName = firstName,
                                                lastName = lastName,
                                                cedula = cedula,
                                                phone = phone,
                                                bank = bank,
                                                idDocumentUrl = downloadUrl,
                                                idVerificationStatus = "pending"
                                            )
                                            repository.saveUserProfile(user)
                                            onRegistrationSuccess()
                                        } catch (e: Exception) {
                                            error = "Error: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { result ->
                                            val newUid = result.user?.uid ?: ""
                                            scope.launch {
                                                try {
                                                    val downloadUrl = repository.uploadIdImage(newUid, selectedImageUri!!)
                                                    val user = User(
                                                        uid = newUid,
                                                        firstName = firstName,
                                                        lastName = lastName,
                                                        cedula = cedula,
                                                        phone = phone,
                                                        bank = bank,
                                                        idDocumentUrl = downloadUrl,
                                                        idVerificationStatus = "pending"
                                                    )
                                                    repository.saveUserProfile(user)
                                                    onRegistrationSuccess()
                                                } catch (e: Exception) {
                                                    error = "Error: ${e.message}"
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            error = "Error: ${it.message}"
                                            isLoading = false
                                        }
                                }
                            } else {
                                error = "Todos los campos y el documento son obligatorios."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldMate),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        else Text("CONFIRMAR REGISTRO", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (!isGoogleAuth) {
                TextButton(onClick = onBack) {
                    Text("VOLVER AL LOGIN", color = GraySmoke, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GraySmoke) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldMate,
            unfocusedBorderColor = Color(0xFF222222),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
