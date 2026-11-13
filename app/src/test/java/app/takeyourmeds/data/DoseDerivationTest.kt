package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * `dosesOn` and `stateAt` (`docs/phase-1.md` §3). Each case fails toward a Dose that never exists,
 * so never reminds, or one that exists twice, so reminds twice.
 */
class DoseDerivationTest {
    private fun doses(
        vararg schedules: ScheduleEntity,
        records: List<DoseRecordEntity> = emptyList(),
        snoozes: List<SnoozeEntity> = emptyList(),
        day: LocalDate = DAY,
        zone: ZoneId = WARSAW,
    ) = dosesOn(day, zone, schedules.toList(), records, snoozes)

    @Test
    fun `two Times make two Doses in Time order`() {
        assertEquals(listOf(at(9), at(21)), doses(schedule("m", "09:00", "21:00")).map { it.scheduledAt })
    }

    @Test
    fun `a Schedule saved mid-day has no Dose before its validFrom`() {
        val saved = schedule("m", "09:00", "21:00", from = at(10))

        assertEquals(listOf(at(21)), doses(saved).map { it.scheduledAt })
        assertEquals(
            listOf(at(9, day = DAY.plusDays(1)), at(21, day = DAY.plusDays(1))),
            doses(saved, day = DAY.plusDays(1)).map { it.scheduledAt },
        )
    }

    @Test
    fun `a Schedule saved in the minute of its Time starts that Time tomorrow, so nothing is Due at once`() {
        val saved = schedule("m", "09:00", from = at(9))

        assertEquals(emptyList<Dose>(), doses(saved))
        assertEquals(listOf(at(9, day = DAY.plusDays(1))), doses(saved, day = DAY.plusDays(1)).map { it.scheduledAt })
    }

    @Test
    fun `an edit in the minute of a Time keeps the Dose already Due under the old Schedule`() {
        val old = schedule("m", "09:00", until = at(9))
        val new = schedule("m", "21:00", from = at(9))

        val today = doses(old, new)

        assertEquals(listOf(at(9), at(21)), today.map { it.scheduledAt })
        assertEquals(DoseState.Due, stateAt(today.first(), instant(9, second = 30)))
    }

    @Test
    fun `an edit in the minute of a Time that keeps it derives that Dose once`() {
        val old = schedule("m", "09:00", until = at(9))
        val new = schedule("m", "09:00", "21:00", from = at(9))

        assertEquals(listOf(at(9), at(21)), doses(old, new).map { it.scheduledAt })
    }

    @Test
    fun `an edit mid-day keeps earlier Doses, adds a later Time today and starts an earlier one tomorrow`() {
        val old = schedule("m", "09:00", until = at(12))
        val new = schedule("m", "08:00", "09:00", "21:00", from = at(12))
        val nineTaken = taken("m", at(9))

        val today = doses(old, new, records = listOf(nineTaken))

        assertEquals(listOf(at(9), at(21)), today.map { it.scheduledAt })
        assertEquals(nineTaken, today.first().record)
        assertEquals(
            listOf(8, 9, 21).map { at(it, day = DAY.plusDays(1)) },
            doses(old, new, day = DAY.plusDays(1)).map { it.scheduledAt },
        )
    }

    @Test
    fun `removing a Time at noon removes only today's later Dose`() {
        val old = schedule("m", "09:00", "21:00", until = at(12))
        val new = schedule("m", "09:00", from = at(12))

        assertEquals(listOf(at(9)), doses(old, new).map { it.scheduledAt })
        assertEquals(
            listOf(at(9, day = DAY.minusDays(1)), at(21, day = DAY.minusDays(1))),
            doses(old, new, day = DAY.minusDays(1)).map { it.scheduledAt },
        )
    }

    @Test
    fun `two Schedules covering the same minute still derive one Dose`() {
        assertEquals(listOf(at(9)), doses(schedule("m", "09:00"), schedule("m", "09:00")).map { it.scheduledAt })
    }

    @Test
    fun `00_00 and 23_59 each fall on their own day`() {
        val midnights = schedule("m", "00:00", "23:59", from = at(23, 59, day = DAY.minusDays(1)))

        assertEquals(listOf(at(0), at(23, 59)), doses(midnights).map { it.scheduledAt })
        assertEquals(
            listOf(at(0, day = DAY.minusDays(1)), at(23, 59, day = DAY.minusDays(1))),
            doses(schedule("m", "00:00", "23:59"), day = DAY.minusDays(1)).map { it.scheduledAt },
        )
    }

    @Test
    fun `a Schedule ended at midnight keeps that midnight's Dose`() {
        assertEquals(listOf(at(0)), doses(schedule("m", "00:00", until = at(0))).map { it.scheduledAt })
    }

    @Test
    fun `a Time in the spring-forward gap is one Dose at the shifted instant`() {
        val springForward = LocalDate.of(2026, 3, 29)

        val gap = doses(schedule("m", "02:30", from = at(0, day = springForward.minusDays(1))), day = springForward)

        assertEquals(1, gap.size)
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), gap.single().timeAt)
    }

    @Test
    fun `a Time in the fall-back overlap is one Dose at the first of its two instants`() {
        val fallBack = LocalDate.of(2026, 10, 25)

        val overlap = doses(schedule("m", "02:30", from = at(0, day = fallBack.minusDays(1))), day = fallBack)

        assertEquals(1, overlap.size)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), overlap.single().timeAt)
    }

    @Test
    fun `a time-zone change moves no Dose`() {
        val nineTaken = taken("m", at(9))
        val newYork = ZoneId.of("America/New_York")

        val there = doses(schedule("m", "09:00", "21:00"), records = listOf(nineTaken), zone = newYork)

        assertEquals(listOf(at(9), at(21)), there.map { it.scheduledAt })
        assertEquals(nineTaken, there.first().record)
        assertEquals(Instant.parse("2026-11-14T02:00:00Z"), there.last().timeAt)
    }

    @Test
    fun `a record whose Time was removed still shows Taken on its day`() {
        val today = doses(schedule("m", "21:00"), records = listOf(taken("m", at(9))))

        assertEquals(listOf(at(9), at(21)), today.map { it.scheduledAt })
        assertEquals(DoseState.Taken, stateAt(today.first(), instant(22)))
    }

    @Test
    fun `a record on another day does not appear`() {
        val today = doses(schedule("m", "09:00"), records = listOf(taken("m", at(9, day = DAY.minusDays(1)))))

        assertEquals(listOf(null), today.map { it.record })
    }

    @Test
    fun `a pending Snooze sets the Reminder time and leaves the Dose under its Time`() {
        val until = instant(9, 10)

        val dose = doses(schedule("m", "09:00"), snoozes = listOf(SnoozeEntity("m", at(9), until))).single()

        assertEquals(at(9), dose.scheduledAt)
        assertEquals(until, dose.reminderAt)
        assertEquals(DoseState.Due, stateAt(dose, instant(9, 5)))
    }

    @Test
    fun `a Dose ahead of its Time is neither Due nor Taken, and Due from its minute`() {
        val dose = doses(schedule("m", "09:00")).single()

        assertEquals(DoseState.Upcoming, stateAt(dose, instant(8, 59, second = 59)))
        assertEquals(DoseState.Due, stateAt(dose, instant(9)))
        assertEquals(DoseState.Due, stateAt(dose, instant(23, 59)))
    }
}
