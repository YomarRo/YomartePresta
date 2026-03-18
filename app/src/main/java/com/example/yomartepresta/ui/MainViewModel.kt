package com.example.yomartepresta.ui

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomartepresta.data.model.*
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(val repository: FirebaseRepository = FirebaseRepository()) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> = _loans.asStateFlow()

    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private var userDataJob: Job? = null
    private var loansJob: Job? = null

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                _isAdmin.value = firebaseUser.email == "yomarhdg34@gmail.com"
                fetchUserData(firebaseUser.uid)
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    viewModelScope.launch {
                        repository.updateFcmToken(firebaseUser.uid, token)
                    }
                }
            } else {
                _isAdmin.value = false
                _user.value = null
                _loans.value = emptyList()
                userDataJob?.cancel()
                loansJob?.cancel()
            }
        }
        fetchSettings()
    }

    private fun fetchUserData(uid: String) {
        userDataJob?.cancel()
        userDataJob = viewModelScope.launch {
            repository.getUserFlow(uid).collect { _user.value = it }
        }
        
        loansJob?.cancel()
        loansJob = viewModelScope.launch {
            repository.getLoansFlow(uid).collect { _loans.value = it }
        }
    }

    private fun fetchSettings() {
        viewModelScope.launch {
            repository.getSettingsFlow().collect { _settings.value = it }
        }
    }

    fun calculateLoanLimit(level: Int): Double {
        return when (level) {
            1 -> 10.0
            2 -> 20.0
            3 -> 30.0
            4 -> 40.0
            else -> 50.0
        }
    }

    fun getAvailableLimit(level: Int, activeLoans: List<Loan>): Double {
        val maxLimit = calculateLoanLimit(level)
        val usedAmount = activeLoans
            .filter { it.status == "pending" || it.status == "approved" || it.status == "repayment_pending" || it.status == "overdue" }
            .sumOf { it.amountUsd }
        return (maxLimit - usedAmount).coerceAtLeast(0.0)
    }

    private fun calculateDueDateStr(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        if (day < 15) {
            calendar.set(Calendar.DAY_OF_MONTH, 15)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    suspend fun requestLoan(amountUsd: Double, signatureBytes: ByteArray) {
        val currentUser = _user.value ?: throw Exception("Perfil no cargado")
        val base64Signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        val bcvRate = _settings.value?.bcvRate ?: 1.0
        
        val loan = Loan(
            userId = currentUser.uid,
            userName = "${currentUser.firstName} ${currentUser.lastName}",
            amountUsd = amountUsd,
            amountBs = amountUsd * bcvRate,
            totalToPayUsd = amountUsd * 1.15,
            status = "pending",
            dueDate = calculateDueDateStr(),
            contractSigned = ContractSigned(
                signature = base64Signature,
                signedAt = "" 
            )
        )
        repository.requestLoan(loan)
    }

    suspend fun reportPayment(loanId: String, imageUri: Uri, amountUsd: Double) {
        val bcvRate = _settings.value?.bcvRate ?: 1.0
        val proofUrl = repository.uploadRepaymentCapture(loanId, imageUri)
        val updates = mapOf(
            "repaymentCaptureUrl" to proofUrl,
            "repaymentAmountUsd" to amountUsd,
            "repaymentAmountBs" to amountUsd * bcvRate
        )
        repository.updateLoanRepayment(loanId, updates)
    }

    fun logout() {
        repository.logout()
    }
}
