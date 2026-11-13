package app.takeyourmeds.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

val DAY: LocalDate = LocalDate.of(2026, 11, 13)

fun at(
    hour: Int,
    minute: Int = 0,
    day: LocalDate = DAY,
): LocalDateTime = day.atTime(hour, minute)

fun instant(
    hour: Int,
    minute: Int = 0,
    day: LocalDate = DAY,
    second: Int = 0,
    zone: ZoneId = WARSAW,
): Instant = ZonedDateTime.of(day.atTime(hour, minute, second), zone).toInstant()

fun schedule(
    medicineId: String,
    vararg times: String,
    from: LocalDateTime = at(0).minusDays(30),
    until: LocalDateTime? = null,
) = ScheduleEntity(
    medicineId = medicineId,
    times = times.map(LocalTime::parse),
    validFrom = from,
    validUntil = until,
    createdAt = Instant.EPOCH,
)

fun taken(
    medicineId: String,
    scheduledAt: LocalDateTime,
    answeredAt: Instant = Instant.EPOCH,
) = DoseRecordEntity(
    medicineId = medicineId,
    scheduledAt = scheduledAt,
    answer = DoseAnswer.Taken,
    answeredAt = answeredAt,
    medicineName = medicineId,
    medicineAmount = "1 tablet",
)
