package mobile.dairy.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import mobile.dairy.app.MainActivity
import mobile.dairy.app.R
import mobile.dairy.app.core.Constants

/** Single place that posts notifications, honouring private-notification mode. */
object Notifier {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_CELEBRATIONS = "celebrations"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_CELEBRATIONS, "Celebrations", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun post(context: Context, id: Int, title: String, body: String, privateMode: Boolean, channel: String = CHANNEL_REMINDERS) {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (t, b) = if (privateMode) "Bloom" to Constants.PRIVATE_NOTIFICATION_BODY else title to body
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(t)
            .setContentText(b)
            .setStyle(NotificationCompat.BigTextStyle().bigText(b))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            androidx.core.app.NotificationManagerCompat.from(context).notify(id, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — never crash a background path.
        }
    }
}
