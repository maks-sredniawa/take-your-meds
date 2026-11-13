package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** `nextReminderTime` (`docs/phase-1.md` §3). Fails toward an alarm armed past a Dose, which then never reminds. */
class NextReminderTest {
    private fun next(
        now: Instant,
        vararg schedules: ScheduleEntity,
        records: List<DoseRecordEntity> = emptyList(),
        snoozes: List<SnoozeEntity> = emptyList(),
    ) = nextReminderTime(now, WARSAW, schedules.toList(), records, snoozes)

    @Test
    fun `the earliest Reminder across Medicines`() {
        assertEquals(instant(8, 30), next(instant(7), schedule("a", "09:00"), schedule("b", "08:30")))
    }

    @Test
    fun `an answered Dose is not reminded`() {
        assertEquals(
            instant(12),
            next(instant(8), schedule("a", "09:00"), schedule("b", "12:00"), records = listOf(taken("a", at(9)))),
        )
    }

    @Test
    fun `a Snooze ending before the next Time wins`() {
        assertEquals(
            instant(9, 10),
            next(
                instant(9, 5),
                schedule("a", "09:00"),
                schedule("b", "12:00"),
                snoozes = listOf(SnoozeEntity("a", at(9), instant(9, 10))),
            ),
        )
    }

    @Test
    fun `a Snooze already past arms the next Time instead`() {
        assertEquals(
            instant(12),
            next(
                instant(9, 30),
                schedule("a", "09:00"),
                schedule("b", "12:00"),
                snoozes = listOf(SnoozeEntity("a", at(9), instant(9, 10))),
            ),
        )
    }

    @Test
    fun `a Snooze of yesterday's late Dose that ends after midnight is reminded`() {
        assertEquals(
            instant(0, 5),
            next(
                instant(0, 1),
                schedule("a", "23:55"),
                snoozes = listOf(SnoozeEntity("a", at(23, 55, day = DAY.minusDays(1)), instant(0, 5))),
            ),
        )
    }

    @Test
    fun `after the day's last Time, tomorrow's first`() {
        assertEquals(instant(9, day = DAY.plusDays(1)), next(instant(22), schedule("a", "09:00", "21:00")))
    }

    @Test
    fun `strictly after now, so a firing never re-arms its own minute`() {
        assertEquals(instant(21), next(instant(9), schedule("a", "09:00", "21:00")))
    }

    @Test
    fun `a Schedule saved in the minute of its Time is not armed for it`() {
        assertEquals(
            instant(9, day = DAY.plusDays(1)),
            next(instant(9, second = 20), schedule("a", "09:00", from = at(9))),
        )
    }

    @Test
    fun `no Doses ahead means no alarm`() {
        assertNull(next(instant(8)))
        assertNull(next(instant(8), schedule("a", "09:00", until = at(7))))
    }
}
