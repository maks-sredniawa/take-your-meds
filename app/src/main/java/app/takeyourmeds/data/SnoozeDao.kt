package app.takeyourmeds.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SnoozeDao {
    @Query("SELECT * FROM snoozes")
    fun observeAll(): Flow<List<SnoozeEntity>>

    @Query("SELECT * FROM snoozes")
    suspend fun all(): List<SnoozeEntity>

    /** A second Snooze of the same Dose replaces the first (ADR-0009). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(snooze: SnoozeEntity)

    @Query("DELETE FROM snoozes WHERE medicineId = :medicineId AND scheduledAt = :scheduledAt")
    suspend fun delete(
        medicineId: String,
        scheduledAt: LocalDateTime,
    )
}
