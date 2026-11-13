package app.takeyourmeds.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicineId")],
)
data class ScheduleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val times: List<LocalTime>,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime? = null,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(times.isNotEmpty()) { "a Schedule has at least one Time" }
        require(times == normalisedTimes(times)) { "Times are whole minutes, sorted and distinct" }
    }

    fun covers(scheduledAt: LocalDateTime): Boolean =
        scheduledAt > validFrom && (validUntil == null || scheduledAt <= validUntil)
}

fun normalisedTimes(times: Collection<LocalTime>): List<LocalTime> =
    times.map { it.withSecond(0).withNano(0) }.distinct().sorted()
