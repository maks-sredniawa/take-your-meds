package app.takeyourmeds.data

import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.Instant
import java.time.LocalDateTime

/** [until] is an instant, not a local date-time: ten minutes stay ten across a clock change. */
@Entity(
    tableName = "snoozes",
    primaryKeys = ["medicineId", "scheduledAt"],
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SnoozeEntity(
    val medicineId: String,
    val scheduledAt: LocalDateTime,
    val until: Instant,
)
