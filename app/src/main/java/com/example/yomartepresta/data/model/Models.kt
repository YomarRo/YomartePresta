package com.example.yomartepresta.data.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val cedula: String = "",
    val phone: String = "",
    val bank: String = "",
    val pagoMovilId: String = "",
    val pagoMovilBank: String = "", // Nuevo campo
    val pagoMovilPhone: String = "", // Nuevo campo
    val level: Int = 1,
    val idVerificationStatus: String = "pending",
    val idDocumentUrl: String? = null,
    val idUploadedAt: String? = null,
    val fcmToken: String? = null,
    val role: String = "user"
)

data class Loan(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amountUsd: Double = 0.0,
    val amountBs: Double = 0.0,
    val totalToPayUsd: Double = 0.0,
    val status: String = "pending",
    val dueDate: String = "",
    val createdAt: Any? = "",
    val approvedAt: String? = null,
    val captureUrl: String? = null,
    val contractSigned: ContractSigned? = null,
    val repaymentCaptureUrl: String? = null,
    val repaymentAmountUsd: Double? = null,
    val repaymentAmountBs: Double? = null,
    val repaidAt: Any? = null,
    val adminProofUrl: String? = null
)

data class ContractSigned(
    val signature: String = "",
    val signedAt: String = ""
)

data class Settings(
    val bcvRate: Double = 0.0,
    val adminPhone: String = "",
    val adminId: String = "",
    val adminBank: String = "",
    val adminName: String = ""
)

fun Any?.toIsoString(): String {
    if (this == null) return ""
    if (this is String) return this
    if (this is Timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(this.toDate())
    }
    return this.toString()
}

fun Any?.toDisplayDate(): String {
    if (this == null || this == "") return "N/A"
    if (this is Timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(this.toDate())
    }
    val str = this.toString()
    if (str.contains("T")) {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(str)
            val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            str
        }
    }
    return str
}
