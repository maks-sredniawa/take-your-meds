package app.takeyourmeds.work

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Sent when the grant is given, never on a revoke: the sweep is the backstop for that (ADR-0003). */
class ExactAlarmGrantReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        rebuildInBackground(context) { it.reminderPlanner()?.replanNow(Replan.Arm) }
    }
}
