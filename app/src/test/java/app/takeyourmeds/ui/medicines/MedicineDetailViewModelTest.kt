package app.takeyourmeds.ui.medicines

import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.schedule
import app.takeyourmeds.ui.FakeDoseRecordDao
import app.takeyourmeds.ui.FakeMedicineDao
import app.takeyourmeds.ui.FakeReminders
import app.takeyourmeds.ui.FakeScheduleDao
import app.takeyourmeds.ui.FakeSnoozeDao
import app.takeyourmeds.work.Replan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineDetailViewModelTest {
    private val metformin = MedicineEntity(id = "metformin", name = "Metformin", amount = "1 tablet")
    private val medicines = FakeMedicineDao(metformin)
    private val reminders = FakeReminders()

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun resetMain() = Dispatchers.resetMain()

    @Test
    fun `deleting a Medicine takes its Reminders out of the tray and re-plans`() =
        runTest {
            val detail =
                MedicineDetailViewModel(
                    "metformin",
                    MedicineRepository(medicines),
                    DoseRepository(
                        FakeScheduleDao(schedule("metformin", "08:00")),
                        FakeDoseRecordDao(),
                        FakeSnoozeDao(),
                    ),
                    reminders,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { detail.state.collect {} }

            detail.delete()

            assertEquals(emptyList<MedicineEntity>(), medicines.rows.value)
            assertEquals(listOf("metformin"), reminders.cancelledMedicines)
            assertEquals(listOf<Replan>(Replan.Arm), reminders.replans)
        }
}
