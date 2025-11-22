package ru.izhxx.aichallenge.domain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.izhxx.aichallenge.common.Logger

/**
 * Android-реализация отправки системных уведомлений о выполнении напоминаний.
 *
 * Создаёт NotificationChannel (Android 8+) и показывает уведомления
 * с заголовком (имя задачи) и превью результата.
 */
class AndroidReminderNotifier(
    private val context: Context
) : ReminderNotifier {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val logger = Logger.forClass(this)

    init {
        ensureChannel()
    }

    override fun notifyResult(taskId: Long, resultId: Long, title: String, preview: String) {
        // Проверка включения уведомлений пользователем
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        // Проверка runtime‑разрешения на API 33+
        if (!hasPostNotificationsPermission()) {
            return
        }

        val notificationId = ((taskId % Int.MAX_VALUE).toInt().coerceAtLeast(1)) + (resultId % 1000).toInt()

        val smallIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title.ifBlank { "Готов результат напоминания" })
            .setContentText(preview.ifBlank { "Результат без текста" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview.ifBlank { "Результат без текста" }))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (se: SecurityException) {
            logger.w("SecurityException while posting notification", se)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания AIChallenge",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления о результатах выполнения задач-напоминаний"
        }

        manager.createNotificationChannel(channel)
    }

    private fun hasPostNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private companion object {
        const val CHANNEL_ID = "aichallenge.reminder.results"
    }
}
