package app.takeyourmeds.work

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.takeyourmeds.data.AppDatabase
import app.takeyourmeds.data.AppPreferences
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseRecordDao
import app.takeyourmeds.data.DoseRecordEntity
import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.MedicineDao
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.SNOOZE_LENGTH
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class ReminderPlannerLockTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notifications = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val storeFile = File(context.cacheDir, "reminder-lock-test.preferences_pb")
    private lateinit var database: AppDatabase
    private lateinit var records: GatedRecords

    private val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    private val due = LocalDateTime.ofInstant(now.minus(10, ChronoUnit.MINUTES), ZoneOffset.UTC)
    private val key = DoseKey("m", due)

    /** Lets its first caller read, then holds it until released: a re-plan caught mid-read. */
    private class Gate {
        val reading = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private var gated = true

        suspend fun <T> hold(read: T): T {
            if (gated) {
                gated = false
                reading.complete(Unit)
                release.await()
            }
            return read
        }
    }

    /** Gates the re-plan's read of the Doses' sources, before it plans. */
    private class GatedRecords(
        private val real: DoseRecordDao,
        val gate: Gate = Gate(),
    ) : DoseRecordDao by real {
        override suspend fun between(
            start: LocalDateTime,
            end: LocalDateTime,
        ): List<DoseRecordEntity> = gate.hold(real.between(start, end))
    }

    /** Gates the re-plan's read of the Medicines, after it planned and just before it posts. */
    private class GatedMedicines(
        private val real: MedicineDao,
        val gate: Gate = Gate(),
    ) : MedicineDao by real {
        override suspend fun all(): List<MedicineEntity> = gate.hold(real.all())
    }

    private val medicine = MedicineEntity(id = "m", name = "Metformin", amount = "1 tablet")

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        storeFile.delete()
        notifications.cancelAll()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        records = GatedRecords(database.doseRecordDao())
        assertTrue("notifications must be allowed for the re-plan to post", notifications.areNotificationsEnabled())
    }

    private suspend fun planner(medicines: MedicineDao = database.medicineDao()): ReminderPlanner {
        database.medicineDao().upsert(medicine)
        val doses = DoseRepository(database.scheduleDao(), records, database.snoozeDao())
        doses.saveTimes("m", listOf(due.toLocalTime()), due.minusDays(2))
        return ReminderPlanner(
            context,
            doses,
            MedicineRepository(medicines),
            AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { storeFile }),
            scope,
            now = { now },
            zone = { ZoneOffset.UTC },
        )
    }

    private fun reminderShowing() = notifications.activeNotifications.any { it.id == REMINDER_ID && it.tag == key.tag }

    @After
    fun tearDown() {
        notifications.cancelAll()
        scope.cancel()
        database.close()
        storeFile.delete()
    }

    @Test
    fun aTakenWaitsForAReplanAlreadyReadingSoItsReminderIsNotPostedAfterIt() =
        runBlocking {
            val planner = planner()

            val replan = launch(Dispatchers.Default) { planner.replanNow(Replan.Repost) }
            records.gate.reading.await()
            val taken = launch(Dispatchers.Default) { planner.markTaken(key) }
            delay(500)
            assertTrue("the Taken waits for the re-plan's lock", answers().isEmpty())

            records.gate.release.complete(Unit)
            joinAll(replan, taken)
            delay(1_000)

            assertEquals(1, answers().size)
            assertFalse("a Taken Dose's Reminder is not left in the tray", reminderShowing())
        }

    @Test
    fun aSnoozeWaitsForAReplanAlreadyReadingSoItsReminderIsNotPostedAfterIt() =
        runBlocking {
            val planner = planner()

            val replan = launch(Dispatchers.Default) { planner.replanNow(Replan.Repost) }
            records.gate.reading.await()
            val snoozed = launch(Dispatchers.Default) { planner.snooze(key) }
            delay(500)
            assertTrue("the Snooze waits for the re-plan's lock", database.snoozeDao().all().isEmpty())

            records.gate.release.complete(Unit)
            joinAll(replan, snoozed)
            delay(1_000)

            assertEquals(listOf(now.plus(SNOOZE_LENGTH)), database.snoozeDao().all().map { it.until })
            assertFalse("a snoozed Dose's Reminder is not left in the tray", reminderShowing())
        }

    @Test
    fun aDeleteWaitsForAReplanAboutToPostSoNoReminderOutlivesItsMedicine() =
        runBlocking {
            val medicines = GatedMedicines(database.medicineDao())
            records.gate.release.complete(Unit)
            val planner = planner(medicines)

            val replan = launch(Dispatchers.Default) { planner.replanNow(Replan.Repost) }
            medicines.gate.reading.await()
            val deleted =
                launch(Dispatchers.Default) { planner.deleteMedicine("m") { database.medicineDao().delete(medicine) } }
            delay(500)
            assertEquals("the delete waits for the re-plan's lock", 1, database.medicineDao().count())

            medicines.gate.release.complete(Unit)
            joinAll(replan, deleted)
            delay(1_000)

            assertEquals(0, database.medicineDao().count())
            assertFalse("a deleted Medicine's Reminder is not left in the tray", reminderShowing())
        }

    private suspend fun answers() = database.doseRecordDao().between(due, due.plusMinutes(1))
}
