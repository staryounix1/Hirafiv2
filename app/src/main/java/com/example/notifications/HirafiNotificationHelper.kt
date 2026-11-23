package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object HirafiNotificationHelper {
    private const val TAG = "HirafiNotifications"

    const val CHANNEL_ORDERS = "hirafi_orders"
    const val CHANNEL_MESSAGES = "hirafi_messages"
    const val CHANNEL_ADMIN = "hirafi_admin"
    const val EXTRA_TARGET_ROUTE = "hirafi_target_route"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_ORDERS,
                "طلبات وعروض حرفي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية للطلبات الجديدة وعروض الأسعار"
            },
            NotificationChannel(
                CHANNEL_MESSAGES,
                "محادثات حرفي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "رسائل الدردشة المباشرة بين الزبون والحرفي"
            },
            NotificationChannel(
                CHANNEL_ADMIN,
                "التوثيق والإدارة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تحديثات توثيق الهوية وإشعارات الإدارة"
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        targetRoute: String? = null
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!targetRoute.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }.onFailure { err ->
            Log.w(TAG, "Unable to show notification: ${err.message}")
        }
    }
}
