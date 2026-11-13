package app.takeyourmeds.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The re-post window of `docs/phase-1.md` §5. */
val DUE_REPOST_LIMIT: Duration = Duration.ofHours(2)

val SNOOZE_LENGTH: Duration = Duration.ofMinutes(10)

enum class DoseState {
    Upcoming,
    Due,
    Taken,
}

/**
 * A Dose (ADR-0009), derived for one zone: [timeAt] is its Time on that zone's clock, and
 * [reminderAt] is when its Reminder is due, the Snooze's end while one is pending.
 */
data class Dose(
    val medicineId: String,
    val scheduledAt: LocalDateTime,
    val timeAt: Instant,
    val record: DoseRecordEntity?,
    val snoozedUntil: Instant?,
) {
    val key: DoseKey get() = DoseKey(medicineId, scheduledAt)

    val reminderAt: Instant get() = snoozedUntil ?: timeAt
}

fun dosesOn(
    day: LocalDate,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): List<Dose> {
    val scheduled =
        schedules.flatMap { schedule ->
            schedule.times
                .map(day::atTime)
                .filter(schedule::covers)
                .map { DoseKey(schedule.medicineId, it) }
        }
    val recordByKey =
        records
            .filter { it.scheduledAt.toLocalDate() == day }
            .associateBy { DoseKey(it.medicineId, it.scheduledAt) }
    val snoozeByKey = snoozes.associateBy { DoseKey(it.medicineId, it.scheduledAt) }

    return (scheduled + recordByKey.keys)
        .distinct()
        .map { key ->
            val record = recordByKey[key]
            Dose(
                medicineId = key.medicineId,
                scheduledAt = key.scheduledAt,
                timeAt = ZonedDateTime.of(key.scheduledAt, zone).toInstant(),
                record = record,
                snoozedUntil = if (record == null) snoozeByKey[key]?.until else null,
            )
        }.sortedWith(compareBy({ it.scheduledAt }, { it.medicineId }))
}

fun stateAt(
    dose: Dose,
    now: Instant,
): DoseState =
    when {
        dose.record != null -> DoseState.Taken
        now >= dose.timeAt -> DoseState.Due
        else -> DoseState.Upcoming
    }

fun nextReminderTime(
    now: Instant,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): Instant? {
    val today = now.atZone(zone).toLocalDate()
    // From yesterday, because a Snooze can carry yesterday's last Dose past midnight.
    return dosesFrom(today.minusDays(1), today.plusDays(1), zone, schedules, records, snoozes)
        .filter { it.record == null && it.reminderAt > now }
        .minOfOrNull { it.reminderAt }
}

private fun dosesFrom(
    first: LocalDate,
    last: LocalDate,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): List<Dose> =
    generateSequence(first) { it.plusDays(1) }
        .takeWhile { it <= last }
        .flatMap { dosesOn(it, zone, schedules, records, snoozes) }
        .toList()

fun dosesToPost(
    armedFor: Instant,
    now: Instant,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): List<Dose> {
    val doses = dosesAround(now, zone, schedules, records, snoozes)
    return doses.filter { it.record == null && it.reminderAt in armedFor..now && it.isCurrent(now, doses) }
}

fun dosesToRepost(
    now: Instant,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): List<Dose> {
    val doses = dosesAround(now, zone, schedules, records, snoozes)
    return doses.filter { it.record == null && it.timeAt <= now && it.reminderAt <= now && it.isCurrent(now, doses) }
}

/** Every Dose a re-post bound can still reach at [now], from the day before the bound starts. */
private fun dosesAround(
    now: Instant,
    zone: ZoneId,
    schedules: List<ScheduleEntity>,
    records: List<DoseRecordEntity>,
    snoozes: List<SnoozeEntity>,
): List<Dose> {
    val first = (now - DUE_REPOST_LIMIT).atZone(zone).toLocalDate().minusDays(1)
    return dosesFrom(first, now.atZone(zone).toLocalDate(), zone, schedules, records, snoozes)
}

/**
 * Whether no derivation reads this Snooze any more: its end is past the re-post bound, so no re-plan
 * posts it, and its Dose is before today, so Today no longer shows *Snoozed until*. Only an answer
 * deletes a Snooze (ADR-0009), so the re-plan deletes these, or an unanswered Dose's row stays forever.
 */
fun SnoozeEntity.outlivedAt(
    now: Instant,
    zone: ZoneId,
): Boolean =
    Duration.between(until, now) >= DUE_REPOST_LIMIT &&
        scheduledAt.toLocalDate() < now.atZone(zone).toLocalDate()

private fun Dose.isCurrent(
    now: Instant,
    doses: List<Dose>,
): Boolean =
    Duration.between(reminderAt, now) < DUE_REPOST_LIMIT &&
        doses.none { it.medicineId == medicineId && it.scheduledAt > scheduledAt && it.timeAt <= now }
