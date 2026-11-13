package app.takeyourmeds.data

import app.takeyourmeds.ui.FakeDoseRecordDao
import app.takeyourmeds.ui.FakeScheduleDao
import app.takeyourmeds.ui.FakeSnoozeDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DoseRepositorySnoozeTest {
    private val snoozes = FakeSnoozeDao()

    private fun repository(vararg answered: DoseRecordEntity) =
        DoseRepository(FakeScheduleDao(schedule("metformin", "08:00")), FakeDoseRecordDao(*answered), snoozes)

    @Test
    fun `a Snooze of an unanswered Dose is written`() =
        runTest {
            val written = repository().snooze(DoseKey("metformin", at(8)), instant(8, 10))

            assertEquals(true, written)
            assertEquals(listOf(SnoozeEntity("metformin", at(8), instant(8, 10))), snoozes.rows.value)
        }

    @Test
    fun `a Snooze of a Dose already answered is refused and writes nothing`() =
        runTest {
            val doses = repository(taken("metformin", at(8), answeredAt = instant(8, 5)))

            val written = doses.snooze(DoseKey("metformin", at(8)), instant(8, 10))

            assertEquals(false, written)
            assertEquals(emptyList<SnoozeEntity>(), snoozes.rows.value)
        }

    @Test
    fun `another Medicine's answer at the same Time does not refuse the Snooze`() =
        runTest {
            val written =
                repository(taken("vitamin-d", at(8))).snooze(DoseKey("metformin", at(8)), instant(8, 10))

            assertEquals(true, written)
        }
}
