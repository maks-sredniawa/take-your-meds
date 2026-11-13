package app.takeyourmeds.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDateTime

@Dao
interface DoseRecordDao {
    @Query("SELECT * FROM dose_records WHERE scheduledAt >= :start AND scheduledAt < :end")
    fun observeBetween(
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<DoseRecordEntity>>

    @Query("SELECT * FROM dose_records WHERE scheduledAt >= :start AND scheduledAt < :end")
    suspend fun between(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<DoseRecordEntity>

    @Query("SELECT COUNT(*) FROM dose_records WHERE answer = :answer")
    fun observeCount(answer: DoseAnswer): Flow<Int>

    /** IGNORE: a Dose answered twice keeps its first answer and its first time (ADR-0009). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: DoseRecordEntity): Long

    /** Matches [answeredAt] too, so an Undo never removes an answer it did not write. */
    @Query(
        "DELETE FROM dose_records WHERE medicineId = :medicineId AND scheduledAt = :scheduledAt AND answeredAt = :answeredAt",
    )
    suspend fun delete(
        medicineId: String,
        scheduledAt: LocalDateTime,
        answeredAt: Instant,
    )
}
