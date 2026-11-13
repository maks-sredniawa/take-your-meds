package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant

/** The one alarm firing: posts what lies between its armed time and now, then arms the next. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_FIRE) return
        val armedFor = intent.getLongExtra(EXTRA_ARMED_FOR, -1L).takeIf { it >= 0 } ?: return
        rebuildInBackground(context) { it.reminderPlanner()?.replanNow(Replan.Fired(Instant.ofEpochMilli(armedFor))) }
    }

    internal companion object {
        const val ACTION_FIRE = "app.takeyourmeds.action.REMINDER_ALARM"
        const val EXTRA_ARMED_FOR = "armedFor"
    }
}
