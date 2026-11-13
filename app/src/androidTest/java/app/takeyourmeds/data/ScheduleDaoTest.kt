package app.takeyourmeds.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.LocalTime

/** `docs/phase-1.md` §2, *Editing a Schedule*. */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var schedules: ScheduleDao

    private val day = LocalDateTime.of(2026, 11, 13, 0, 0).toLocalDate()
    private val noon = day.atTime(12, 0)
    private val nine = LocalTime.of(9, 0)
    private val nineteen = LocalTime.of(19, 0)

    @Before
    fun open() =
        runTest {
            database =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            schedules = database.scheduleDao()
            database.medicineDao().upsert(MedicineEntity(id = "m", name = "Metformin", amount = "1 tablet"))
        }

    @After
    fun close() = database.close()

    @Test
    fun anEditEndsTheOldScheduleAndStartsTheNewOneAtTheSameMinute() =
        runTest {
            schedules.saveTimes("m", listOf(nine), day.atTime(8, 0))
            schedules.saveTimes("m", listOf(nineteen, nine), noon)

            val (old, new) = schedules.allFor("m")
            assertEquals(listOf(nine), old.times)
            assertEquals(day.atTime(8, 0), old.validFrom)
            assertEquals(noon, old.validUntil)
            assertEquals(listOf(nine, nineteen), new.times)
            assertEquals(noon, new.validFrom)
            assertNull(new.validUntil)
            assertEquals(new, schedules.inForce("m"))
        }

    @Test
    fun aSaveThatLeavesTheTimesAsTheyWereWritesNothing() =
        runTest {
            schedules.saveTimes("m", listOf(nine), day.atTime(8, 0))
            schedules.saveTimes("m", listOf(nine), noon)

            assertEquals(1, schedules.allFor("m").size)
            assertNull(schedules.inForce("m")?.validUntil)
        }

    @Test
    fun aSaveThatRemovesEveryTimeOnlyEndsTheOldSchedule() =
        runTest {
            schedules.saveTimes("m", listOf(nine), day.atTime(8, 0))
            schedules.saveTimes("m", emptyList(), noon)

            assertNull(schedules.inForce("m"))
            assertEquals(noon, schedules.allFor("m").single().validUntil)
        }

    @Test
    fun aFailedInsertLeavesTheOldScheduleInForce() =
        runTest {
            schedules.saveTimes("m", listOf(nine), day.atTime(8, 0), newScheduleId = "first")

            val failed =
                runCatching { schedules.saveTimes("m", listOf(nineteen), noon, newScheduleId = "first") }

            assertTrue(failed.exceptionOrNull() is SQLiteConstraintException)
            val only = schedules.allFor("m").single()
            assertEquals(listOf(nine), only.times)
            assertNull(only.validUntil)
        }

    @Test
    fun aClockSetBackNeverStartsTheNewScheduleInsideTheOld() =
        runTest {
            schedules.saveTimes("m", listOf(nine), noon)
            schedules.saveTimes("m", listOf(nineteen), day.atTime(10, 0))

            val (old, new) = schedules.allFor("m")
            assertEquals(noon, old.validUntil)
            assertEquals(noon, new.validFrom)
        }

    @Test
    fun validFromAndValidUntilAreStoredAtTheSameFixedWidth() =
        runTest {
            schedules.saveTimes("m", listOf(nine), day.atTime(8, 0, 30))
            schedules.saveTimes("m", listOf(nineteen), day.atTime(12, 0, 0, 1))

            val stored = mutableListOf<Pair<String, String?>>()
            database.query(SimpleSQLiteQuery("SELECT validFrom, validUntil FROM schedules ORDER BY validFrom")).use {
                while (it.moveToNext()) stored += it.getString(0) to (if (it.isNull(1)) null else it.getString(1))
            }
            assertEquals(
                listOf("2026-11-13T08:00" to "2026-11-13T12:00", "2026-11-13T12:00" to null),
                stored,
            )
        }

    @Test
    fun aDayReadsEveryScheduleThatCoversAMinuteOfIt() =
        runTest {
            database.medicineDao().upsert(MedicineEntity(id = "ended-before", name = "a", amount = "a"))
            database.medicineDao().upsert(MedicineEntity(id = "ended-at-midnight", name = "b", amount = "b"))
            database.medicineDao().upsert(MedicineEntity(id = "starts-tomorrow", name = "c", amount = "c"))
            schedules.saveTimes("ended-before", listOf(nine), day.minusDays(2).atTime(8, 0))
            schedules.saveTimes("ended-before", emptyList(), day.minusDays(1).atTime(23, 59))
            schedules.saveTimes("ended-at-midnight", listOf(LocalTime.MIDNIGHT), day.minusDays(2).atTime(8, 0))
            schedules.saveTimes("ended-at-midnight", emptyList(), day.atStartOfDay())
            schedules.saveTimes("starts-tomorrow", listOf(nine), day.plusDays(1).atStartOfDay())
            schedules.saveTimes("m", listOf(nine), noon)

            val read = schedules.covering(day.atStartOfDay(), day.plusDays(1).atStartOfDay())

            assertEquals(setOf("ended-at-midnight", "m"), read.map { it.medicineId }.toSet())
        }
}
