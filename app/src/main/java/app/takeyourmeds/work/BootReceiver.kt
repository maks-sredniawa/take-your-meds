package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager restores nothing across a restart, so the one alarm is re-armed here, after re-posting
 * what fell Due while the phone was off (ADR-0010).
 *
 * On Xiaomi's HyperOS this receiver runs only if *autostart* is granted, which the app cannot read
 * (`scripts/device-gate.py`).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        rebuildInBackground(context) {
            ensureSweepEnqueued(it)
            it.reminderPlanner()?.replanNow(Replan.Repost)
        }
    }
}
