package app.takeyourmeds.work

import app.takeyourmeds.data.DUE_REPOST_LIMIT
import app.takeyourmeds.data.Dose
import app.takeyourmeds.data.DoseSources
import app.takeyourmeds.data.dosesToPost
import app.takeyourmeds.data.dosesToRepost
import app.takeyourmeds.data.nextReminderTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface Replan {
    data class Fired(
        val armedFor: Instant,
    ) : Replan

    data object Repost : Replan

    data object Arm : Replan
}

data class ReminderPlan(
    val post: List<Dose>,
    val armFor: Instant?,
)

fun planReminders(
    trigger: Replan,
    now: Instant,
    zone: ZoneId,
    sources: DoseSources,
): ReminderPlan {
    val (schedules, records, snoozes) = sources
    val post =
        when (trigger) {
            is Replan.Fired -> dosesToPost(trigger.armedFor, now, zone, schedules, records, snoozes)
            Replan.Repost -> dosesToRepost(now, zone, schedules, records, snoozes)
            Replan.Arm -> emptyList()
        }
    return ReminderPlan(post, nextReminderTime(now, zone, schedules, records, snoozes))
}

/**
 * The span of Schedules and records [planReminders] derives from: back to the day before the re-post
 * bound starts, which also covers yesterday's Snoozes, and forward through tomorrow.
 */
fun sourceWindow(
    now: Instant,
    zone: ZoneId,
): Pair<LocalDateTime, LocalDateTime> {
    val first = (now - DUE_REPOST_LIMIT).atZone(zone).toLocalDate().minusDays(1)
    val last = now.atZone(zone).toLocalDate().plusDays(1)
    return first.atStartOfDay() to last.plusDays(1).atStartOfDay()
}
