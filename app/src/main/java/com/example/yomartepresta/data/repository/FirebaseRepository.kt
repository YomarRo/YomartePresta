package com.example.yomartepresta.data.repository

import android.net.Uri
import com.example.yomartepresta.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    // Utility for ISO 8601 Dates (Web Compatible)
    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // Auth
    val currentUser get() = auth.currentUser
    fun logout() = auth.signOut()

    // User Profile
    suspend fun saveUserProfile(user: User) {
        val userWithDate = user.copy(idUploadedAt = getIsoTimestamp())
        // Usamos set con merge para guardar el perfil completo sin borrar campos extra (como fcmToken)
        firestore.collection("users").document(user.uid).set(userWithDate, SetOptions.merge()).await()
    }

    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val subscription = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        // Protocolo Crítico: Usar .update() para no borrar datos de la Web
        try {
            firestore.collection("users").document(uid)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Si el documento no existe (usuario nuevo), el token se guardará
            // durante el proceso de registro/saveUserProfile.
        }
    }

    // ID Verification (Web Protocol Sync)
    suspend fun uploadIdImage(uid: String, uri: Uri): String {
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference.child("id_documents/${uid}_$timestamp")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // Loans
    fun getLoansFlow(userId: String): Flow<List<Loan>> = callbackFlow {
        val subscription = firestore.collection("loans")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val loans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(loans.sortedByDescending { it.createdAt.toIsoString() })
            }
        awaitClose { subscription.remove() }
    }

    fun getAllLoansFlow(): Flow<List<Loan>> = callbackFlow {
        val subscription = firestore.collection("loans")
            .addSnapshotListener { snapshot, _ ->
                val loans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(loans.sortedByDescending { it.createdAt.toIsoString() })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun requestLoan(loan: Loan): String {
        val loanRef = firestore.collection("loans").document()
        val finalLoan = loan.copy(
            id = loanRef.id,
            createdAt = getIsoTimestamp(),
            contractSigned = loan.contractSigned?.copy(signedAt = getIsoTimestamp())
        )
        loanRef.set(finalLoan).await()
        return loanRef.id
    }

    // Repayments (Web Sync)
    suspend fun uploadRepaymentCapture(loanId: String, uri: Uri): String {
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference.child("repayments/${loanId}_$timestamp")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun updateLoanRepayment(loanId: String, updates: Map<String, Any>) {
        val finalUpdates = updates.toMutableMap()
        finalUpdates["repaidAt"] = getIsoTimestamp()
        finalUpdates["status"] = "repayment_pending"
        firestore.collection("loans").document(loanId).update(finalUpdates).await()
    }

    // Settings
    fun getSettingsFlow(): Flow<Settings?> = callbackFlow {
        val subscription = firestore.collection("settings").document("general")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(Settings::class.java))
            }
        awaitClose { subscription.remove() }
    }

    // Admin Methods
    fun getAllPendingLoans(): Flow<List<Loan>> = callbackFlow {
        val subscription = firestore.collection("loans")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val loans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(loans.sortedByDescending { it.createdAt.toIsoString() })
            }
        awaitClose { subscription.remove() }
    }

    fun getAllRepaymentPendingLoans(): Flow<List<Loan>> = callbackFlow {
        val subscription = firestore.collection("loans")
            .whereEqualTo("status", "repayment_pending")
            .addSnapshotListener { snapshot, _ ->
                val loans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(loans.sortedByDescending { it.repaidAt.toIsoString() })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateLoanStatus(loanId: String, status: String, loanData: Loan? = null) {
        if (loanId.isEmpty()) throw IllegalArgumentException("ID vacío")

        val updates = mutableMapOf<String, Any>()
        
        if (status == "approved") {
            updates["status"] = "approved"
            updates["approvedAt"] = getIsoTimestamp()
        } else if (status == "paid" && loanData != null) {
            val amountPaid = loanData.repaymentAmountUsd ?: 0.0
            val newDebt = (loanData.totalToPayUsd - amountPaid).coerceAtLeast(0.0)
            
            if (newDebt > 0.01) {
                updates["status"] = "approved"
                updates["totalToPayUsd"] = newDebt
                updates["repaymentCaptureUrl"] = FieldValue.delete()
                updates["repaymentAmountUsd"] = FieldValue.delete()
                updates["repaymentAmountBs"] = FieldValue.delete()
            } else {
                updates["status"] = "paid"
                updates["totalToPayUsd"] = 0.0
            }
        } else {
            updates["status"] = status
        }
        
        firestore.collection("loans").document(loanId).update(updates).await()

        if (updates["status"] == "paid" && loanData != null) {
            updateUserLevel(loanData.userId)
        }
    }

    private suspend fun updateUserLevel(userId: String) {
        val paidLoans = firestore.collection("loans")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "paid")
            .get().await()
        
        val count = paidLoans.size()
        val newLevel = (count / 2) + 1
        
        firestore.collection("users").document(userId)
            .update("level", newLevel).await()
    }

    suspend fun updateBcvRate(rate: Double) {
        firestore.collection("settings").document("general")
            .update("bcvRate", rate).await()
    }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection("users")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(User::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateUserVerification(uid: String, status: String) {
        firestore.collection("users").document(uid)
            .update("idVerificationStatus", status).await()
    }

    suspend fun requestLoan(loan: Loan, signatureUri: Uri): String {
        val loanRef = firestore.collection("loans").document()
        val signatureUrl = uploadSignature(loanRef.id, signatureUri)
        val finalLoan = loan.copy(
            id = loanRef.id,
            contractSigned = ContractSigned(signature = signatureUrl)
        )
        loanRef.set(finalLoan).await()
        return loanRef.id
    }

    private suspend fun uploadSignature(loanId: String, uri: Uri): String {
        val ref = storage.reference.child("signatures/$loanId.png")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
