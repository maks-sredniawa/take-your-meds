package app.takeyourmeds.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * **Reads return `Flow`; writes are `suspend`.** Screens collect the flow and never call a refresh.
 *
 * Kotlin note for a JS background: a `Flow` is closest to an RxJS `Observable` or an async iterator —
 * a stream you subscribe to, not a promise you await once. Room re-runs the query and re-emits
 * whenever any table the query touches changes, so a write anywhere in the app updates every screen
 * showing the affected rows with no wiring between them. That is the whole reason not to hand-roll
 * a `refresh()`: a hand-rolled one is a list of call sites somebody will forget to add to.
 *
 * `suspend` on the writes is closest to `async` — it can wait without blocking the thread, and Room
 * uses that to move the actual disk write off the main thread for you. Calling a `suspend` function
 * requires a coroutine scope, which is why the repository takes one.
 */
@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun observeById(id: String): Flow<MedicineEntity?>

    /**
     * A one-shot read, for code with no UI to update — a worker deciding whether
     * anything is due. Deliberately separate from [observeAll] rather than a `.first()` on it: a
     * flow's first emission is a subscription that has to be torn down again, and in a worker that
     * is a lifecycle to get wrong.
     */
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    suspend fun all(): List<MedicineEntity>

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun count(): Int

    // REPLACE rather than ABORT because the editor saves an edited row through here too. With a
    // UUID primary key a conflict only happens when it is genuinely the same row.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MedicineEntity)

    @Update
    suspend fun update(medicine: MedicineEntity)

    @Delete
    suspend fun delete(medicine: MedicineEntity)

    @Query("DELETE FROM medicines")
    suspend fun deleteAll()
}
