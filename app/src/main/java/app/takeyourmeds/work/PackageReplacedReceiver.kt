package app.takeyourmeds.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A schema-bumping update leaves the database at the old version until something opens it. The
 * re-plan opens it through the registered migration, so the first Reminder after an update does not
 * wait for the user to open the app (ADR-0003).
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        rebuildInBackground(context) {
            ensureSweepEnqueued(it)
            it.reminderPlanner()?.replanNow(Replan.Repost)
        }
    }
}
