package app.takeyourmeds.debug

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.takeyourmeds.R
import app.takeyourmeds.work.ReminderChannel
import app.takeyourmeds.work.canScheduleExactAlarms
import app.takeyourmeds.work.ensureReminderChannels

/** Phase 1's gate probe; the apparatus and its readings are `docs/phase-1.md` §1 and G1–G3. */
class AlarmProbeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val firedAt = System.currentTimeMillis()
        val exact = intent.getBooleanExtra(EXTRA_EXACT, true)
        val chain = intent.getBooleanExtra(EXTRA_CHAIN, false)
        if (intent.action == ACTION_FIRE) {
            fire(context, intent.getLongExtra(EXTRA_ARMED_FOR, 0L), firedAt, exact, chain)
        } else {
            val armedFor = firedAt + intent.getIntExtra(EXTRA_MINUTES, 3) * 60_000L
            val error = arm(context, armedFor, exact, chain)
            setResult(if (error == null) 0 else 1, error ?: "armedFor=$armedFor", null)
        }
    }

    private fun fire(
        context: Context,
        armedFor: Long,
        firedAt: Long,
        exact: Boolean,
        chain: Boolean,
    ) {
        val posted = post(context, armedFor)
        Log.i(TAG, "armedFor=$armedFor firedAt=$firedAt lateMs=${firedAt - armedFor} exact=$exact posted=$posted")
        if (chain) arm(context, armedFor + CHAIN_GAP_MS, exact, chain = false)
    }

    private fun arm(
        context: Context,
        armedFor: Long,
        exact: Boolean,
        chain: Boolean,
    ): String? {
        if (exact && !context.canScheduleExactAlarms()) {
            Log.w(TAG, "not armed: exact requested and the grant is refused")
            return "exact alarm grant refused"
        }
        val alarms = context.getSystemService(AlarmManager::class.java)
        val operation =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, AlarmProbeReceiver::class.java)
                    .setAction(ACTION_FIRE)
                    .putExtra(EXTRA_ARMED_FOR, armedFor)
                    .putExtra(EXTRA_EXACT, exact)
                    .putExtra(EXTRA_CHAIN, chain),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, armedFor, operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, armedFor, operation)
        }
        Log.i(TAG, "armed armedFor=$armedFor exact=$exact chain=$chain")
        return null
    }

    private fun post(
        context: Context,
        armedFor: Long,
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        context.ensureReminderChannels()
        val notification =
            NotificationCompat
                .Builder(context, ReminderChannel.Reminders.id)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.debug_alarm_probe_title))
                .setContentText(context.getString(R.string.debug_alarm_probe_text, armedFor))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        return try {
            manager.notify(NOTIFICATION_TAG, (armedFor / 1000).toInt(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private companion object {
        const val TAG = "AlarmProbe"
        const val ACTION_FIRE = "app.takeyourmeds.debug.ALARM_PROBE_FIRE"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_EXACT = "exact"
        const val EXTRA_CHAIN = "chain"
        const val EXTRA_ARMED_FOR = "armedFor"
        const val NOTIFICATION_TAG = "alarm-probe"
        const val REQUEST_CODE = 0x7a1a
        const val CHAIN_GAP_MS = 5 * 60_000L
    }
}
