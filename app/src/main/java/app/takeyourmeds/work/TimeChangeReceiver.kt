package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** A clock or zone change moves the alarm's instant, and can carry the clock past a Time unreminded. */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        rebuildInBackground(context) { it.reminderPlanner()?.replanNow(Replan.Repost) }
    }
}
