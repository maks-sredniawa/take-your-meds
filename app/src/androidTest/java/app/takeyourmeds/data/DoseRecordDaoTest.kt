package app.takeyourmeds.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class DoseRecordDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var records: DoseRecordDao

    private val nine = LocalDateTime.of(2026, 11, 13, 9, 0)
    private val medicine = MedicineEntity(id = "m", name = "Metformin", amount = "1 tablet")

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        records = database.doseRecordDao()
    }

    @After
    fun close() = database.close()

    private fun taken(
        at: LocalDateTime,
        answeredAt: Long,
        medicineId: String = "m",
    ) = DoseRecordEntity(
        medicineId = medicineId,
        scheduledAt = at,
        answer = DoseAnswer.Taken,
        answeredAt = Instant.ofEpochMilli(answeredAt),
        medicineName = "Metformin",
        medicineAmount = "1 tablet",
    )

    @Test
    fun aDoseAnsweredTwiceKeepsOneRecordWithItsFirstAnswerTime() =
        runTest {
            records.insert(taken(nine, answeredAt = 1_000))
            records.insert(taken(nine, answeredAt = 2_000))

            val day = records.between(nine.toLocalDate().atStartOfDay(), nine.toLocalDate().plusDays(1).atStartOfDay())
            assertEquals(1, day.size)
            assertEquals(Instant.ofEpochMilli(1_000), day.single().answeredAt)
            assertEquals(1, records.observeCount(DoseAnswer.Taken).first())
        }

    @Test
    fun deletingAMedicineDeletesItsSchedulesAndSnoozesAndKeepsItsRecords() =
        runTest {
            database.medicineDao().upsert(medicine)
            database.scheduleDao().saveTimes("m", listOf(LocalTime.of(9, 0)), nine.minusHours(1))
            database.snoozeDao().put(SnoozeEntity("m", nine, Instant.ofEpochMilli(5_000)))
            records.insert(taken(nine, answeredAt = 1_000))

            database.medicineDao().delete(medicine)

            assertEquals(emptyList<ScheduleEntity>(), database.scheduleDao().allFor("m"))
            assertEquals(emptyList<SnoozeEntity>(), database.snoozeDao().all())
            assertEquals(listOf(taken(nine, answeredAt = 1_000)), records.between(nine.minusDays(1), nine.plusDays(1)))
        }

    @Test
    fun savingAnEditedMedicineKeepsItsScheduleAndSnooze() =
        runTest {
            database.medicineDao().upsert(medicine)
            database.scheduleDao().saveTimes("m", listOf(LocalTime.of(9, 0)), nine.minusHours(1))
            database.snoozeDao().put(SnoozeEntity("m", nine, Instant.ofEpochMilli(5_000)))

            database.medicineDao().upsert(medicine.copy(amount = "2 tablets"))

            assertEquals(listOf(LocalTime.of(9, 0)), database.scheduleDao().inForce("m")?.times)
            assertEquals(1, database.snoozeDao().all().size)
        }

    @Test
    fun aDayReadsFromItsMidnightUpToButNotIncludingTheNext() =
        runTest {
            val day = nine.toLocalDate()
            records.insert(taken(day.minusDays(1).atTime(23, 59), answeredAt = 1, medicineId = "before"))
            records.insert(taken(day.atStartOfDay(), answeredAt = 1, medicineId = "first"))
            records.insert(taken(day.atTime(23, 59), answeredAt = 1, medicineId = "last"))
            records.insert(taken(day.plusDays(1).atStartOfDay(), answeredAt = 1, medicineId = "after"))

            val read = records.between(day.atStartOfDay(), day.plusDays(1).atStartOfDay())

            assertEquals(setOf("first", "last"), read.map { it.medicineId }.toSet())
        }

    @Test
    fun aSecondSnoozeReplacesTheFirst() =
        runTest {
            database.medicineDao().upsert(medicine)
            database.snoozeDao().put(SnoozeEntity("m", nine, Instant.ofEpochMilli(5_000)))
            database.snoozeDao().put(SnoozeEntity("m", nine, Instant.ofEpochMilli(9_000)))

            assertEquals(listOf(SnoozeEntity("m", nine, Instant.ofEpochMilli(9_000))), database.snoozeDao().all())

            database.snoozeDao().delete("m", nine)
            assertNull(database.snoozeDao().all().firstOrNull())
        }
}
