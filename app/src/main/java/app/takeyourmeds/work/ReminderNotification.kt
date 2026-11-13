package app.takeyourmeds.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import app.takeyourmeds.MainActivity
import app.takeyourmeds.R
import app.takeyourmeds.data.Dose
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.MedicineEntity
import java.time.LocalTime

internal fun reminderNotification(
    context: Context,
    medicine: MedicineEntity,
    dose: Dose,
    formatTime: (LocalTime) -> String,
): Notification {
    val time = formatTime(dose.scheduledAt.toLocalTime())
    val public =
        NotificationCompat
            .Builder(context, ReminderChannel.Reminders.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_text_public, time))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
    return NotificationCompat
        .Builder(context, ReminderChannel.Reminders.id)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(medicine.name)
        .setContentText(context.getString(R.string.reminder_text, medicine.amount, time))
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOnlyAlertOnce(true)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(public)
        .setWhen(dose.timeAt.toEpochMilli())
        .setShowWhen(true)
        .setContentIntent(openToday(context))
        .addAction(0, context.getString(R.string.reminder_action_taken), takenIntent(context, dose.key))
        .build()
}

/** A fresh task, so the app opens on its root, Today, whatever screen it was left on. */
private fun openToday(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

/** The Dose rides in the data, not an extra: `PendingIntent` equality ignores extras. */
private fun takenIntent(
    context: Context,
    key: DoseKey,
): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_TAKEN)
            .setData(key.tag.toUri()),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
