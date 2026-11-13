package app.takeyourmeds.ui.medicines

import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.at
import app.takeyourmeds.data.schedule
import app.takeyourmeds.ui.FakeDoseRecordDao
import app.takeyourmeds.ui.FakeMedicineDao
import app.takeyourmeds.ui.FakeScheduleDao
import app.takeyourmeds.ui.FakeSnoozeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineEditorViewModelTest {
    private val metformin = MedicineEntity(id = "metformin", name = "Metformin", amount = "1 tablet")
    private val medicines = FakeMedicineDao(metformin)
    private val schedules = FakeScheduleDao(schedule("metformin", "08:00", "20:00"))

    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun resetMain() = Dispatchers.resetMain()

    private fun editor(medicineId: String? = "metformin") =
        MedicineEditorViewModel(
            medicineId = medicineId,
            repository = MedicineRepository(medicines),
            doses = DoseRepository(schedules, FakeDoseRecordDao(), FakeSnoozeDao()),
            now = { at(12, 30) },
        )

    private fun times(vararg text: String) = text.map(LocalTime::parse)

    @Test
    fun `an edited Medicine starts with its Times`() =
        runTest {
            assertEquals(times("08:00", "20:00"), editor().state.value.times)
        }

    @Test
    fun `an added Time takes its place in order, and a duplicate is ignored`() =
        runTest {
            val editor = editor()

            editor.addTime(LocalTime.of(13, 0))
            editor.addTime(LocalTime.of(8, 0))

            assertEquals(times("08:00", "13:00", "20:00"), editor.state.value.times)
        }

    @Test
    fun `a changed Time moves into order, and one changed onto another merges with it`() =
        runTest {
            val editor = editor()

            editor.changeTime(LocalTime.of(20, 0), LocalTime.of(7, 0))
            assertEquals(times("07:00", "08:00"), editor.state.value.times)

            editor.changeTime(LocalTime.of(7, 0), LocalTime.of(8, 0))
            assertEquals(times("08:00"), editor.state.value.times)
        }

    @Test
    fun `a removed Time is gone`() =
        runTest {
            val editor = editor()

            editor.removeTime(LocalTime.of(8, 0))

            assertEquals(times("20:00"), editor.state.value.times)
        }

    @Test
    fun `saving ends the old Schedule and starts the new one at the save's minute`() =
        runTest {
            val editor = editor()
            editor.addTime(LocalTime.of(13, 0))

            editor.save()

            val (old, new) = schedules.rows.value
            assertEquals(at(12, 30), old.validUntil)
            assertEquals(times("08:00", "13:00", "20:00") to at(12, 30), new.times to new.validFrom)
            assertEquals(null, new.validUntil)
            assertTrue(editor.state.value.saved)
        }

    @Test
    fun `saving with no Times leaves the Medicine with no Schedule in force`() =
        runTest {
            val editor = editor()
            editor.removeTime(LocalTime.of(8, 0))
            editor.removeTime(LocalTime.of(20, 0))

            editor.save()

            assertEquals(listOf(at(12, 30)), schedules.rows.value.map { it.validUntil })
        }

    @Test
    fun `a new Medicine is saved with its Times`() =
        runTest {
            val editor = editor(medicineId = null)
            editor.setName("Vitamin D")
            editor.setAmount("1 capsule")
            editor.addTime(LocalTime.of(9, 0))

            editor.save()

            val added = medicines.rows.value.single { it.name == "Vitamin D" }
            assertEquals(
                times("09:00"),
                schedules.rows.value
                    .single { it.medicineId == added.id }
                    .times,
            )
        }
}
