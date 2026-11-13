package app.takeyourmeds.work

import android.content.Context
import android.os.Build

/** The two asks the first Schedule's save makes, in the order it makes them (`PLAN.md` rule 4). */
enum class Ask {
    Notifications,
    ExactAlarms,
}

/** What the phone allows right now. Read live on every resume, never remembered from an ask. */
data class Grants(
    val sdk: Int,
    val notificationsAllowed: Boolean,
    val mutedChannel: ReminderChannel?,
    val exactAlarmsAllowed: Boolean,
)

fun Context.readGrants(): Grants =
    Grants(
        sdk = Build.VERSION.SDK_INT,
        notificationsAllowed = notificationsAllowed(),
        mutedChannel = ReminderChannel.entries.firstOrNull { !channelCanAppear(it) },
        exactAlarmsAllowed = canScheduleExactAlarms(),
    )

/** `docs/phase-1.md` §4: the next ask to make, or null when there is nothing left worth asking. */
fun nextAsk(
    grants: Grants,
    asked: Set<Ask>,
): Ask? =
    when {
        !grants.notificationsAllowed ->
            Ask.Notifications.takeIf { grants.sdk >= Build.VERSION_CODES.TIRAMISU && it !in asked }
        !grants.exactAlarmsAllowed -> Ask.ExactAlarms.takeUnless { it in asked }
        else -> null
    }
