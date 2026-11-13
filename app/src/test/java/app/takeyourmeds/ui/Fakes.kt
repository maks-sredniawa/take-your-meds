package app.takeyourmeds.ui

import app.takeyourmeds.data.DoseAnswer
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseRecordDao
import app.takeyourmeds.data.DoseRecordEntity
import app.takeyourmeds.data.MedicineDao
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.ScheduleDao
import app.takeyourmeds.data.ScheduleEntity
import app.takeyourmeds.data.SnoozeDao
import app.takeyourmeds.data.SnoozeEntity
import app.takeyourmeds.tip.HomeTipAsk
import app.takeyourmeds.work.Reminders
import app.takeyourmeds.work.Replan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDateTime

class FakeMedicineDao(
    vararg initial: MedicineEntity,
) : MedicineDao {
    val rows = MutableStateFlow(initial.toList())

    override fun observeAll(): Flow<List<MedicineEntity>> = rows

    override fun observeById(id: String): Flow<MedicineEntity?> = rows.map { all -> all.find { it.id == id } }

    override suspend fun all(): List<MedicineEntity> = rows.value

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsert(medicine: MedicineEntity) =
        rows.update { all ->
            all.filterNot { it.id == medicine.id } +
                medicine
        }

    override suspend fun update(medicine: MedicineEntity) = upsert(medicine)

    override suspend fun delete(medicine: MedicineEntity) =
        rows.update { all -> all.filterNot { it.id == medicine.id } }

    override suspend fun deleteAll() = rows.update { emptyList() }
}

class FakeScheduleDao(
    vararg initial: ScheduleEntity,
) : ScheduleDao() {
    val rows = MutableStateFlow(initial.toList())

    private fun List<ScheduleEntity>.covering(
        start: LocalDateTime,
        end: LocalDateTime,
    ) = filter { schedule -> schedule.validFrom < end && schedule.validUntil.let { it == null || it >= start } }

    override fun observeCovering(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<ScheduleEntity>> = rows.map { it.covering(start, end) }

    override suspend fun covering(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleEntity> = rows.value.covering(start, end)

    override fun observeInForce(medicineId: String): Flow<ScheduleEntity?> =
        rows.map { all -> all.find { it.medicineId == medicineId && it.validUntil == null } }

    override suspend fun inForce(medicineId: String): ScheduleEntity? =
        rows.value.find { it.medicineId == medicineId && it.validUntil == null }

    override fun observeAnyInForce(): Flow<Boolean> = rows.map { all -> all.any { it.validUntil == null } }

    override suspend fun anyInForce(): Boolean = rows.value.any { it.validUntil == null }

    override suspend fun allFor(medicineId: String): List<ScheduleEntity> =
        rows.value.filter { it.medicineId == medicineId }.sortedBy { it.validFrom }

    override suspend fun insert(schedule: ScheduleEntity) = rows.update { it + schedule }

    override suspend fun end(
        id: String,
        at: LocalDateTime,
    ) = rows.update { all -> all.map { if (it.id == id) it.copy(validUntil = at) else it } }
}

class FakeDoseRecordDao(
    vararg initial: DoseRecordEntity,
) : DoseRecordDao {
    val rows = MutableStateFlow(initial.toList())

    private fun List<DoseRecordEntity>.between(
        start: LocalDateTime,
        end: LocalDateTime,
    ) = filter { it.scheduledAt >= start && it.scheduledAt < end }

    override fun observeBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<DoseRecordEntity>> = rows.map { it.between(start, end) }

    override suspend fun between(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<DoseRecordEntity> = rows.value.between(start, end)

    override fun observeCount(answer: DoseAnswer): Flow<Int> = rows.map { all -> all.count { it.answer == answer } }

    override suspend fun insert(record: DoseRecordEntity): Long {
        if (rows.value.any { it.medicineId == record.medicineId && it.scheduledAt == record.scheduledAt }) return -1L
        rows.update { it + record }
        return rows.value.size.toLong()
    }

    override suspend fun delete(
        medicineId: String,
        scheduledAt: LocalDateTime,
        answeredAt: Instant,
    ) = rows.update { all ->
        all.filterNot { it.medicineId == medicineId && it.scheduledAt == scheduledAt && it.answeredAt == answeredAt }
    }
}

class FakeSnoozeDao(
    vararg initial: SnoozeEntity,
) : SnoozeDao {
    val rows = MutableStateFlow(initial.toList())

    override fun observeAll(): Flow<List<SnoozeEntity>> = rows

    override suspend fun all(): List<SnoozeEntity> = rows.value

    override suspend fun put(snooze: SnoozeEntity) =
        rows.update { all ->
            all.filterNot { it.medicineId == snooze.medicineId && it.scheduledAt == snooze.scheduledAt } + snooze
        }

    override suspend fun delete(
        medicineId: String,
        scheduledAt: LocalDateTime,
    ) = rows.update { all -> all.filterNot { it.medicineId == medicineId && it.scheduledAt == scheduledAt } }
}

class FakeTipAsk(
    due: Boolean = false,
) : HomeTipAsk {
    override val due = MutableStateFlow(due)
    var answered = false

    override suspend fun refreshIfDue() = Unit

    override suspend fun answer() {
        answered = true
        due.value = false
    }
}

class FakeReminders : Reminders {
    val replans = mutableListOf<Replan>()
    val answered = mutableListOf<DoseKey>()
    val deletedMedicines = mutableListOf<String>()

    override fun replan(trigger: Replan) {
        replans += trigger
    }

    override suspend fun <T> answer(
        key: DoseKey,
        write: suspend () -> T,
    ): T = write().also { answered += key }

    override suspend fun deleteMedicine(
        medicineId: String,
        delete: suspend () -> Unit,
    ) {
        delete()
        deletedMedicines += medicineId
    }
}
