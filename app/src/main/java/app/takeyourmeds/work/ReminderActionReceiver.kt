package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.takeyourmeds.data.DoseKey

/** A Reminder's Taken and Snooze, answered without opening the app. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val key = intent.data?.toString()?.let(DoseKey::parse) ?: return
        when (intent.action) {
            ACTION_TAKEN -> rebuildInBackground(context) { it.reminderPlanner()?.markTaken(key) }
            ACTION_SNOOZE -> rebuildInBackground(context) { it.reminderPlanner()?.snooze(key) }
        }
    }

    internal companion object {
        const val ACTION_TAKEN = "app.takeyourmeds.action.REMINDER_TAKEN"
        const val ACTION_SNOOZE = "app.takeyourmeds.action.REMINDER_SNOOZE"
    }
}
