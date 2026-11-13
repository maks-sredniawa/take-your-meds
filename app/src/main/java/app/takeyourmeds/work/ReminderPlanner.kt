package app.takeyourmeds.work

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.AppPreferences
import app.takeyourmeds.data.Dose
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.schemaBlocksBackgroundWork
import app.takeyourmeds.ui.common.timeFormatter
import app.takeyourmeds.ui.settings.inAppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/** What a screen asks of the Reminders; the [ReminderPlanner] is the one implementation. */
interface Reminders {
    /** Runs on the application's scope, so a screen closing straight after its save cannot cut it short. */
    fun replan(trigger: Replan)

    /**
     * Writes a Dose's answer with [write] and takes its Reminder out of the tray, then re-plans. Both
     * hold the re-plan lock, so a re-plan that read the Dose before the answer cannot post it after.
     */
    suspend fun <T> answer(
        key: DoseKey,
        write: suspend () -> T,
    ): T

    fun cancelMedicine(medicineId: String)
}

/** The one alarm and the Reminders it posts (`docs/phase-1.md` §5, ADR-0010). */
class ReminderPlanner(
    context: Context,
    private val doses: DoseRepository,
    private val medicines: MedicineRepository,
    private val preferences: AppPreferences,
    private val scope: CoroutineScope,
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : Reminders {
    private val context = context.applicationContext
    private val notifications = NotificationManagerCompat.from(this.context)

    // Two triggers at once would each arm from a read the other's write has made stale.
    private val lock = Mutex()

    override fun replan(trigger: Replan) {
        scope.launch { replanNow(trigger) }
    }

    suspend fun replanNow(trigger: Replan) = lock.withLock { replanLocked(trigger) }

    override suspend fun <T> answer(
        key: DoseKey,
        write: suspend () -> T,
    ): T {
        val written = withdrawing(key, write)
        replan(Replan.Arm)
        return written
    }

    /** The Reminder's Taken: the answer is written before the Reminder goes, so a failed write leaves it in the tray. */
    suspend fun markTaken(key: DoseKey) {
        withdrawing(key) {
            val medicine = medicines.observeById(key.medicineId).first()
            if (medicine != null) doses.markTaken(medicine, key.scheduledAt, now())
        }
        replanNow(Replan.Arm)
    }

    override fun cancelMedicine(medicineId: String) {
        notifications.activeNotifications
            .filter { it.id == REMINDER_ID && it.tag?.let(DoseKey::parse)?.medicineId == medicineId }
            .forEach { notifications.cancel(it.tag, it.id) }
    }

    private suspend fun <T> withdrawing(
        key: DoseKey,
        write: suspend () -> T,
    ): T =
        lock.withLock {
            write().also { notifications.cancel(key.tag, REMINDER_ID) }
        }

    // Armed before posting, so a Reminder that fails to post cannot also stop the chain.
    private suspend fun replanLocked(trigger: Replan) {
        val at = now()
        val zone = zone()
        val (start, end) = sourceWindow(at, zone)
        val lastArmed = preferences.reminderArmedFor.first()
        val postedThrough = preferences.reminderPostedThrough.first()
        val sources = doses.sourcesBetween(start, end)
        val plan = planReminders(trigger, at, zone, sources, lastArmed, postedThrough)
        val exact = arm(plan.armFor)
        preferences.setReminderPlan(plan.armFor, plan.postedThrough)
        val posted =
            try {
                post(plan.post)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "posting failed: ${e.javaClass.name}")
                0
            }
        val armedFor = (trigger as? Replan.Fired)?.armedFor?.toEpochMilli()
        Log.i(
            TAG,
            "trigger=${trigger.label} armedFor=${armedFor ?: "-"} lastArmed=${lastArmed?.toEpochMilli() ?: "-"} " +
                "firedAt=${at.toEpochMilli()} lateMs=${armedFor?.let { at.toEpochMilli() - it } ?: "-"} " +
                "posted=$posted next=${plan.armFor?.toEpochMilli() ?: "none"} exact=$exact",
        )
    }

    private suspend fun post(due: List<Dose>): Int {
        if (due.isEmpty() || !notifications.areNotificationsEnabled()) return 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return 0
        }
        val localised = context.inAppLanguage()
        localised.ensureReminderChannels()
        val formatTime = localised.timeFormatter()
        val byId = medicines.all().associateBy { it.id }
        val posted =
            due.filter { dose ->
                val medicine = byId[dose.medicineId] ?: return@filter false
                try {
                    notifications.notify(
                        dose.key.tag,
                        REMINDER_ID,
                        reminderNotification(localised, medicine, dose, formatTime),
                    )
                    true
                } catch (_: SecurityException) {
                    false
                }
            }
        val showing =
            notifications.activeNotifications
                .filter {
                    it.id == REMINDER_ID
                }.mapNotNull { it.tag?.let(DoseKey::parse) }
        remindersReplacedBy(posted, showing).forEach { notifications.cancel(it.tag, REMINDER_ID) }
        return posted.size
    }

    /** @return whether the alarm armed is exact. */
    private fun arm(at: Instant?): Boolean {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return false
        if (at == null) {
            alarms.cancel(alarmIntent(Instant.EPOCH))
            return false
        }
        val operation = alarmIntent(at)
        if (context.canScheduleExactAlarms()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation)
                return true
            } catch (_: SecurityException) {
                // Revoked between the read and the arm.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation)
        return false
    }

    /** One request code, so arming again replaces the alarm; the extra is not part of its identity. */
    private fun alarmIntent(armedFor: Instant): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, ReminderAlarmReceiver::class.java)
                .setAction(ReminderAlarmReceiver.ACTION_FIRE)
                .putExtra(ReminderAlarmReceiver.EXTRA_ARMED_FOR, armedFor.toEpochMilli()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        /** Logs times and counts only, never a Medicine (ADR-0008). */
        const val TAG = "Reminders"
        const val ALARM_REQUEST_CODE = 1
    }
}

/** Every Reminder shares this id; its Dose is in the tag, so no hash can make two Doses collide. */
internal const val REMINDER_ID = 1

private val Replan.label: String
    get() =
        when (this) {
            is Replan.Fired -> "fired"
            Replan.Repost -> "repost"
            Replan.Arm -> "arm"
        }

/** The planner, or null while the database is not this build's to open (ADR-0001). */
internal fun Context.reminderPlanner(): ReminderPlanner? =
    if (schemaBlocksBackgroundWork()) null else (applicationContext as MainApplication).container.reminders
