package app.takeyourmeds.work

import app.takeyourmeds.data.DUE_REPOST_LIMIT
import app.takeyourmeds.data.Dose
import app.takeyourmeds.data.DoseKey
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

/**
 * [lastArmed] is the alarm the previous re-plan armed. Arming replaces an alarm that has not landed
 * yet, so once that time has passed every re-plan posts what the alarm would have, as its firing does.
 */
fun planReminders(
    trigger: Replan,
    now: Instant,
    zone: ZoneId,
    sources: DoseSources,
    lastArmed: Instant?,
): ReminderPlan {
    val (schedules, records, snoozes) = sources
    val from = listOfNotNull((trigger as? Replan.Fired)?.armedFor, lastArmed?.takeIf { it <= now }).minOrNull()
    val post =
        when {
            trigger == Replan.Repost -> dosesToRepost(now, zone, schedules, records, snoozes)
            from != null -> dosesToPost(from, now, zone, schedules, records, snoozes)
            else -> emptyList()
        }
    return ReminderPlan(post, nextReminderTime(now, zone, schedules, records, snoozes))
}

/** One Taken in the tray per Medicine: a posted Dose takes out that Medicine's earlier Reminders, which stay Due on Today. */
fun remindersReplacedBy(
    posted: List<Dose>,
    showing: List<DoseKey>,
): List<DoseKey> =
    showing.filter { shown -> posted.any { it.medicineId == shown.medicineId && it.scheduledAt > shown.scheduledAt } }

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
