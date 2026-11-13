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

/** [postedThrough] is the mark to store: every Reminder time up to it has had its posting pass. */
data class ReminderPlan(
    val post: List<Dose>,
    val armFor: Instant?,
    val postedThrough: Instant?,
)

/**
 * [lastArmed] is the alarm the previous re-plan armed. Arming replaces an alarm that has not landed
 * yet, so once that time has passed every re-plan posts what the alarm would have, as its firing does.
 *
 * [postedThrough] is the last re-plan's mark. A firing already queued when a re-plan replaced its
 * alarm still arrives, and posts only what came after that mark, so a Reminder the user swiped away
 * does not come back. A re-post ignores the mark: it runs where the tray was lost or the clock moved.
 */
fun planReminders(
    trigger: Replan,
    now: Instant,
    zone: ZoneId,
    sources: DoseSources,
    lastArmed: Instant?,
    postedThrough: Instant? = null,
): ReminderPlan {
    val (schedules, records, snoozes) = sources
    val from = listOfNotNull((trigger as? Replan.Fired)?.armedFor, lastArmed?.takeIf { it <= now }).minOrNull()
    val post =
        when {
            trigger == Replan.Repost -> dosesToRepost(now, zone, schedules, records, snoozes)
            from != null -> {
                val after = postedThrough?.plusMillis(1)?.let { maxOf(it, from) } ?: from
                dosesToPost(after, now, zone, schedules, records, snoozes)
            }
            else -> emptyList()
        }
    val passed = trigger == Replan.Repost || from != null
    return ReminderPlan(
        post = post,
        armFor = nextReminderTime(now, zone, schedules, records, snoozes),
        postedThrough = if (passed) now else postedThrough,
    )
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
