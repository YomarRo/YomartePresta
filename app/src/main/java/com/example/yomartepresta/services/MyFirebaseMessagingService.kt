package com.example.yomartepresta.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.yomartepresta.MainActivity
import com.example.yomartepresta.R
import com.example.yomartepresta.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Actualizar el token en Firestore si el usuario está autenticado
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            scope.launch {
                FirebaseRepository().updateFcmToken(uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Manejo de notificaciones con texto (UI visible)
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Yomar te Presta", it.body ?: "", remoteMessage.data)
        }
        
        // Si no tiene notificación pero tiene datos (Silent push para Admin o Usuario)
        if (remoteMessage.notification == null && remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Yomar te Presta"
            val body = remoteMessage.data["body"] ?: "Nueva actualización del sistema"
            sendNotification(title, body, remoteMessage.data)
        }
    }

    private fun sendNotification(title: String, messageBody: String, dataMap: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Pasar datos de redirección si existen (userId o loanId)
            dataMap.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val userChannelId = "yomar_alerts"
        val adminChannelId = "admin_notifications"

        // Usamos el canal de admin si el payload trae la bandera isAdminAlert
        val channelId = if (dataMap.containsKey("isAdminAlert")) adminChannelId else userChannelId

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para Usuarios
            if (notificationManager.getNotificationChannel(userChannelId) == null) {
                val userChannel = NotificationChannel(
                    userChannelId,
                    "Alertas de Vales",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones para empleados sobre sus vales"
                }
                notificationManager.createNotificationChannel(userChannel)
            }
            
            // Canal para Administrador (Importance HIGH)
            if (notificationManager.getNotificationChannel(adminChannelId) == null) {
                val adminChannel = NotificationChannel(
                    adminChannelId,
                    "Alertas de Administrador",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones críticas de nuevas solicitudes y pagos"
                }
                notificationManager.createNotificationChannel(adminChannel)
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
