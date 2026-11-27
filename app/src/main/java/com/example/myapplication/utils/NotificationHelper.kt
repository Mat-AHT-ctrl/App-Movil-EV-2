package com.example.myapplication.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.remote.NotificacionDto

object NotificationHelper {

    private const val CANAL_ID = "alertas_reservas"
    private const val NOMBRE_CANAL = "Alertas de reservas"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                NOMBRE_CANAL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de aprobación, rechazo y cambios de reservas"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    fun mostrar(context: Context, notificacion: NotificacionDto) {
        crearCanal(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val aviso = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificacion.titulo)
            .setContentText(notificacion.mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificacion.id.hashCode(), aviso)
    }
}
