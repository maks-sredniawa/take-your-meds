package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.takeyourmeds.data.DoseKey

/** A Reminder's Taken, answered without opening the app. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_TAKEN) return
        val key = intent.data?.toString()?.let(DoseKey::parse) ?: return
        rebuildInBackground(context) { it.reminderPlanner()?.markTaken(key) }
    }

    internal companion object {
        const val ACTION_TAKEN = "app.takeyourmeds.action.REMINDER_TAKEN"
    }
}
