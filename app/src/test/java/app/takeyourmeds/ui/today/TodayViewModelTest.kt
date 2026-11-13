package app.takeyourmeds.ui.today

import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.DoseState
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.SnoozeEntity
import app.takeyourmeds.data.WARSAW
import app.takeyourmeds.data.at
import app.takeyourmeds.data.instant
import app.takeyourmeds.data.schedule
import app.takeyourmeds.ui.FakeDoseRecordDao
import app.takeyourmeds.ui.FakeMedicineDao
import app.takeyourmeds.ui.FakeScheduleDao
import app.takeyourmeds.ui.FakeSnoozeDao
import app.takeyourmeds.ui.FakeTipAsk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val metformin = MedicineEntity(id = "metformin", name = "Metformin", amount = "1 tablet")
    private val vitaminD = MedicineEntity(id = "vitamin-d", name = "Vitamin D", amount = "1 capsule")

    private val medicines = FakeMedicineDao(metformin, vitaminD)
    private val schedules = FakeScheduleDao(schedule("vitamin-d", "08:00"), schedule("metformin", "08:00", "20:00"))
    private val records = FakeDoseRecordDao()
    private val snoozes = FakeSnoozeDao()

    private var now: Instant = instant(12)
    private val ticks = MutableStateFlow(now)

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun resetMain() = Dispatchers.resetMain()

    private fun tick(to: Instant) {
        now = to
        ticks.value = to
    }

    private fun TestScope.today(): TodayViewModel {
        val viewModel =
            TodayViewModel(
                medicines = MedicineRepository(medicines),
                doses = DoseRepository(schedules, records, snoozes),
                tipAsk = FakeTipAsk(),
                ticks = ticks,
                now = { now },
                zone = { WARSAW },
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    private fun TodayViewModel.row(
        medicineId: String,
        hour: Int,
    ): DoseRow =
        state.value.groups
            .flatMap { it.doses }
            .single { it.key == DoseKey(medicineId, at(hour)) }

    @Test
    fun `Doses are grouped under one header per Time, in Time order`() =
        runTest {
            val groups = today().state.value.groups

            assertEquals(
                listOf(
                    LocalTime.of(8, 0) to listOf("Metformin", "Vitamin D"),
                    LocalTime.of(20, 0) to listOf("Metformin"),
                ),
                groups.map { group -> group.time to group.doses.map { it.name } },
            )
        }

    @Test
    fun `a Dose ahead turns Due as the clock passes its Time`() =
        runTest {
            tick(instant(19, 59, second = 59))
            val today = today()
            assertEquals(DoseState.Upcoming, today.row("metformin", 20).state)

            tick(instant(20))

            assertEquals(DoseState.Due, today.row("metformin", 20).state)
        }

    @Test
    fun `marking a Due Dose taken writes its answer and keeps the row in place`() =
        runTest {
            val today = today()
            val before =
                today.state.value.groups
                    .map { group -> group.doses.map { it.key } }

            tick(instant(12, 5))
            today.markTaken(DoseKey("metformin", at(8)))

            val record = records.rows.value.single()
            assertEquals(DoseKey("metformin", at(8)), DoseKey(record.medicineId, record.scheduledAt))
            assertEquals(instant(12, 5), record.answeredAt)
            assertEquals(DoseState.Taken, today.row("metformin", 8).state)
            assertEquals(LocalTime.of(12, 5), today.row("metformin", 8).takenAt)
            assertEquals(
                before,
                today.state.value.groups
                    .map { group -> group.doses.map { it.key } },
            )
            assertEquals(DoseKey("metformin", at(8)), today.state.value.undoable)
        }

    @Test
    fun `a Dose ahead cannot be marked taken`() =
        runTest {
            val today = today()

            today.markTaken(DoseKey("metformin", at(20)))

            assertEquals(emptyList<Any>(), records.rows.value)
            assertEquals(null, today.state.value.undoable)
        }

    @Test
    fun `Undo deletes the answer just written and the Dose is Due again`() =
        runTest {
            val today = today()
            today.markTaken(DoseKey("metformin", at(8)))

            today.undo(DoseKey("metformin", at(8)))

            assertEquals(emptyList<Any>(), records.rows.value)
            assertEquals(DoseState.Due, today.row("metformin", 8).state)
            assertEquals(null, today.state.value.undoable)
        }

    @Test
    fun `once the snackbar is gone the answer stays`() =
        runTest {
            val today = today()
            today.markTaken(DoseKey("metformin", at(8)))

            today.undoExpired(DoseKey("metformin", at(8)))
            today.undo(DoseKey("metformin", at(8)))

            assertEquals(1, records.rows.value.size)
            assertEquals(null, today.state.value.undoable)
        }

    @Test
    fun `an answer to a deleted Medicine stays on its day under the name it was answered with`() =
        runTest {
            val today = today()
            today.markTaken(DoseKey("metformin", at(8)))
            medicines.rows.value = listOf(vitaminD)
            schedules.rows.value = schedules.rows.value.filterNot { it.medicineId == "metformin" }

            val row = today.row("metformin", 8)

            assertEquals("Metformin" to DoseState.Taken, row.name to row.state)
        }

    @Test
    fun `a snoozed Due Dose says when it is snoozed until`() =
        runTest {
            snoozes.rows.value = listOf(SnoozeEntity("vitamin-d", at(8), until = instant(12, 10)))

            assertEquals(LocalTime.of(12, 10), today().row("vitamin-d", 8).snoozedUntil)
        }
}
