package net.ganjoorak.app.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.ganjoorak.app.data.local.DailyPoem
import net.ganjoorak.shared.R

/** Local daily notification (no server, no FCM). */
object DailyPoemNotifier {
    const val CHANNEL_ID = "daily_poem"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "شعر روزانه", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "شعر روزانهٔ گنجورک"
        }
        manager.createNotificationChannel(channel)
    }

    /** Posts the day's poem; silently skips when the notification permission is missing. */
    fun post(context: Context, poem: DailyPoem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val pending = PendingIntent.getActivity(
            context,
            1,
            openPoemIntent(context, poem.ref),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_poem)
            .setContentTitle("${poem.poetName} — ${poem.title}")
            .setContentText(poem.lines.joinToString(" / "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(poem.lines.joinToString("\n")))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
