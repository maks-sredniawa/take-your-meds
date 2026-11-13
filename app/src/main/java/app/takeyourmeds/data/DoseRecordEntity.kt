package app.takeyourmeds.data

import androidx.room.Entity
import androidx.room.Index
import java.time.Instant
import java.time.LocalDateTime

enum class DoseAnswer {
    Taken,
}

/**
 * The user's answer to one Dose (ADR-0009). No foreign key onto `medicines`, so a deleted
 * Medicine's answers survive; that is why its name and Amount are copied in.
 */
@Entity(
    tableName = "dose_records",
    primaryKeys = ["medicineId", "scheduledAt"],
    indices = [Index("scheduledAt")],
)
data class DoseRecordEntity(
    val medicineId: String,
    val scheduledAt: LocalDateTime,
    val answer: DoseAnswer,
    val answeredAt: Instant,
    val medicineName: String,
    val medicineAmount: String,
)
