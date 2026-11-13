package app.takeyourmeds.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Dao
abstract class ScheduleDao {
    @Query("SELECT * FROM schedules WHERE validFrom < :end AND (validUntil IS NULL OR validUntil >= :start)")
    abstract fun observeCovering(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE validFrom < :end AND (validUntil IS NULL OR validUntil >= :start)")
    abstract suspend fun covering(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId AND validUntil IS NULL")
    abstract fun observeInForce(medicineId: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId AND validUntil IS NULL")
    abstract suspend fun inForce(medicineId: String): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY validFrom")
    abstract suspend fun allFor(medicineId: String): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET validUntil = :at WHERE id = :id")
    protected abstract suspend fun end(
        id: String,
        at: LocalDateTime,
    )

    @Transaction
    open suspend fun saveTimes(
        medicineId: String,
        times: Collection<LocalTime>,
        at: LocalDateTime,
        newScheduleId: String = UUID.randomUUID().toString(),
    ) {
        val wanted = normalisedTimes(times)
        val current = inForce(medicineId)
        if (current?.times.orEmpty() == wanted) return
        // A clock set back must not start the new Schedule inside the old one's span (phase-1.md §2).
        val boundary = if (current == null) at else maxOf(at, current.validFrom)
        if (current != null) end(current.id, boundary)
        if (wanted.isNotEmpty()) {
            insert(ScheduleEntity(id = newScheduleId, medicineId = medicineId, times = wanted, validFrom = boundary))
        }
    }
}
