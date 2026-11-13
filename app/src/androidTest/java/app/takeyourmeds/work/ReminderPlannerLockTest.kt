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
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
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

    /** Returns its read, then holds the re-plan that made it until released: a re-plan caught mid-read. */
    private class GatedRecords(
        private val real: DoseRecordDao,
    ) : DoseRecordDao by real {
        val reading = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private var gated = true

        override suspend fun between(
            start: LocalDateTime,
            end: LocalDateTime,
        ): List<DoseRecordEntity> {
            val read = real.between(start, end)
            if (gated) {
                gated = false
                reading.complete(Unit)
                release.await()
            }
            return read
        }
    }

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
    }

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
            assertTrue("notifications must be allowed for the re-plan to post", notifications.areNotificationsEnabled())
            val medicine = MedicineEntity(id = "m", name = "Metformin", amount = "1 tablet")
            database.medicineDao().upsert(medicine)
            val doses = DoseRepository(database.scheduleDao(), records, database.snoozeDao())
            doses.saveTimes("m", listOf(due.toLocalTime()), due.minusDays(2))
            val planner =
                ReminderPlanner(
                    context,
                    doses,
                    MedicineRepository(database.medicineDao()),
                    AppPreferences(PreferenceDataStoreFactory.create(scope = scope) { storeFile }),
                    scope,
                    now = { now },
                    zone = { ZoneOffset.UTC },
                )

            val replan = launch(Dispatchers.Default) { planner.replanNow(Replan.Repost) }
            records.reading.await()
            val taken = launch(Dispatchers.Default) { planner.markTaken(key) }
            delay(500)
            assertTrue("the Taken waits for the re-plan's lock", answers().isEmpty())

            records.release.complete(Unit)
            joinAll(replan, taken)
            delay(1_000)

            assertEquals(1, answers().size)
            assertFalse(
                "a Taken Dose's Reminder is not left in the tray",
                notifications.activeNotifications.any { it.id == REMINDER_ID && it.tag == key.tag },
            )
        }

    private suspend fun answers() = database.doseRecordDao().between(due, due.plusMinutes(1))
}
