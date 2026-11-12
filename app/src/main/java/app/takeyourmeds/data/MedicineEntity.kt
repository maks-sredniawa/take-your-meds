package app.takeyourmeds.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A UUID rather than an autoincrementing id so the rows Auto Backup restores (ADR-0005) never collide
 * with rows a phone already holds. [amount] is free text the app never parses (ADR-0011).
 */
@Entity(
    tableName = "medicines",
    indices = [Index("createdAt")],
)
data class MedicineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: String,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
)
