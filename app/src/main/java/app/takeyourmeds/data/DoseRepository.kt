package app.takeyourmeds.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class DoseSources(
    val schedules: List<ScheduleEntity>,
    val records: List<DoseRecordEntity>,
    val snoozes: List<SnoozeEntity>,
)

/** The Schedules, answers and Snoozes a Dose is derived from (ADR-0009). */
class DoseRepository(
    private val schedules: ScheduleDao,
    private val records: DoseRecordDao,
    private val snoozes: SnoozeDao,
) {
    fun observeDay(
        day: LocalDate,
        zone: ZoneId,
    ): Flow<List<Dose>> {
        val start = day.atStartOfDay()
        val end = day.plusDays(1).atStartOfDay()
        return combine(
            schedules.observeCovering(start, end),
            records.observeBetween(start, end),
            snoozes.observeAll(),
        ) { covering, answered, snoozed -> dosesOn(day, zone, covering, answered, snoozed) }
    }

    fun observeTakenCount(): Flow<Int> = records.observeCount(DoseAnswer.Taken)

    fun observeTimes(medicineId: String): Flow<List<LocalTime>> =
        schedules.observeInForce(medicineId).map { it?.times.orEmpty() }

    /** Whether any Medicine has a Schedule in force: the asks wait for the first (`PLAN.md` rule 4). */
    fun observeAnyScheduled(): Flow<Boolean> = schedules.observeAnyInForce()

    suspend fun anyScheduled(): Boolean = schedules.anyInForce()

    suspend fun timesOf(medicineId: String): List<LocalTime> = schedules.inForce(medicineId)?.times.orEmpty()

    suspend fun saveTimes(
        medicineId: String,
        times: Collection<LocalTime>,
        at: LocalDateTime,
    ) = schedules.saveTimes(medicineId, times, at)

    suspend fun sourcesBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ) = DoseSources(schedules.covering(start, end), records.between(start, end), snoozes.all())

    /** The record written, or null when the Dose was already answered and keeps that answer. */
    suspend fun markTaken(
        medicine: MedicineEntity,
        scheduledAt: LocalDateTime,
        at: Instant,
    ): DoseRecordEntity? {
        val record =
            DoseRecordEntity(
                medicineId = medicine.id,
                scheduledAt = scheduledAt,
                answer = DoseAnswer.Taken,
                answeredAt = at,
                medicineName = medicine.name,
                medicineAmount = medicine.amount,
            )
        val written = record.takeIf { records.insert(it) != -1L }
        snoozes.delete(medicine.id, scheduledAt)
        return written
    }

    suspend fun undo(record: DoseRecordEntity) =
        records.delete(record.medicineId, record.scheduledAt, record.answeredAt)
}
