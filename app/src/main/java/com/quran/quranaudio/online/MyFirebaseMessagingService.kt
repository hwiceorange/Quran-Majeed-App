package com.quran.quranaudio.online

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.quran.quranaudio.online.analytics.RetentionFunnel
import com.quran.quranaudio.online.prayertimes.ui.MainActivity

/** Receives retention campaigns sent as FCM data messages. */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val campaign = data[KEY_CAMPAIGN] ?: message.messageId ?: "unspecified"
        val target = normalizeTarget(data[KEY_TARGET])
        val title = data[KEY_TITLE] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = data[KEY_BODY] ?: message.notification?.body ?: return

        RetentionFunnel.push(this, "received", campaign, target, "fcm")
        if (!canPostNotifications()) {
            RetentionFunnel.push(this, "blocked", campaign, target, "permission")
            return
        }

        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_PUSH_CAMPAIGN, campaign)
            putExtra(MainActivity.EXTRA_PUSH_TARGET, target)
        }
        val requestCode = (campaign + target).hashCode() and Int.MAX_VALUE
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_RETENTION)
            .setSmallIcon(R.drawable.notification_ic)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(requestCode, notification)
        RetentionFunnel.push(this, "displayed", campaign, target, "fcm")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RETENTION,
                getString(R.string.fcm_retention_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.fcm_retention_channel_description)
                enableVibration(false)
            }
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun normalizeTarget(value: String?): String = when (value) {
        "quran", "prayer", "tasbih", "subscription" -> value
        else -> "quran"
    }

    companion object {
        private const val CHANNEL_RETENTION = "retention_reminders_v1"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_CAMPAIGN = "campaign_id"
        private const val KEY_TARGET = "target"
    }
}
