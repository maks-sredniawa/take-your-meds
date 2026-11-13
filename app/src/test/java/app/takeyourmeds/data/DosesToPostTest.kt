package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

/**
 * `dosesToPost` (`docs/phase-1.md` §3): what an alarm firing posts. Fails toward a Dose between the
 * armed time and a late landing left unposted.
 */
class DosesToPostTest {
    private fun posted(
        armedFor: Instant,
        now: Instant,
        vararg schedules: ScheduleEntity,
        records: List<DoseRecordEntity> = emptyList(),
        snoozes: List<SnoozeEntity> = emptyList(),
    ): List<Pair<String, LocalDateTime>> =
        dosesToPost(armedFor, now, WARSAW, schedules.toList(), records, snoozes)
            .map { it.medicineId to it.scheduledAt }

    @Test
    fun `the window is inclusive at both ends`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 5)),
            posted(
                instant(9),
                instant(9, 5),
                schedule("before", "08:59"),
                schedule("a", "09:00"),
                schedule("b", "09:05"),
                schedule("after", "09:06"),
            ),
        )
    }

    @Test
    fun `two Medicines at the same minute are both posted`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9)),
            posted(instant(9), instant(9, second = 2), schedule("b", "09:00"), schedule("a", "09:00")),
        )
    }

    @Test
    fun `a late landing posts every Dose it covers`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 15), "c" to at(9, 30)),
            posted(instant(9), instant(9, 40), schedule("a", "09:00"), schedule("b", "09:15"), schedule("c", "09:30")),
        )
    }

    @Test
    fun `an answered Dose is skipped`() {
        assertEquals(
            listOf("b" to at(9)),
            posted(
                instant(9),
                instant(9, 1),
                schedule("a", "09:00"),
                schedule("b", "09:00"),
                records = listOf(taken("a", at(9))),
            ),
        )
    }

    @Test
    fun `a Snooze ending inside the window is posted, and the Time it snoozed is not posted again`() {
        val snooze = SnoozeEntity("a", at(9), instant(9, 10))

        assertEquals(
            listOf("a" to at(9)),
            posted(instant(9, 10), instant(9, 10), schedule("a", "09:00"), snoozes = listOf(snooze)),
        )
        assertEquals(
            emptyList<Pair<String, LocalDateTime>>(),
            posted(instant(9), instant(9), schedule("a", "09:00"), snoozes = listOf(snooze)),
        )
    }
}
