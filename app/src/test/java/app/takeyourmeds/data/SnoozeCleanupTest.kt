package app.takeyourmeds.data

import app.takeyourmeds.work.Replan
import app.takeyourmeds.work.planReminders
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A Snooze nobody answers is never deleted by an answer, so the re-plan deletes it once nothing can
 * read it. Fails toward a Snooze deleted while its Reminder or Today's *Snoozed until* still needs it.
 */
class SnoozeCleanupTest {
    private val yesterdayLate = SnoozeEntity("a", at(23, 30, day = DAY.minusDays(1)), instant(0, 20))
    private val thisMorning = SnoozeEntity("b", at(8), instant(8, 10))

    @Test
    fun `a Snooze carried past midnight is kept while its end is inside the re-post bound`() {
        assertEquals(false, yesterdayLate.outlivedAt(instant(2, 19), WARSAW))
        assertEquals(true, yesterdayLate.outlivedAt(instant(2, 20), WARSAW))
    }

    @Test
    fun `a Snooze of a Dose on Today is kept all day, for its Snoozed until`() {
        assertEquals(false, thisMorning.outlivedAt(instant(23, 59), WARSAW))
        assertEquals(true, thisMorning.outlivedAt(instant(0, day = DAY.plusDays(1)), WARSAW))
    }

    @Test
    fun `deleting every outlived Snooze changes no plan and no Today`() {
        val schedules = listOf(schedule("a", "09:00", "23:30"), schedule("b", "08:00", "21:00"))
        val snoozes =
            listOf(
                yesterdayLate,
                thisMorning,
                SnoozeEntity("a", at(9), instant(10, 50)),
                SnoozeEntity("b", at(21, day = DAY.minusDays(1)), instant(21, 10, day = DAY.minusDays(1))),
            )
        val nows =
            (0..23).flatMap { hour -> listOf(0, 19, 20, 50).map { instant(hour, it) } } +
                instant(0, 5, day = DAY.plusDays(1))
        for (now in nows) {
            val kept = snoozes.filterNot { it.outlivedAt(now, WARSAW) }
            val all = DoseSources(schedules, emptyList(), snoozes)
            val cleaned = all.copy(snoozes = kept)
            for (trigger in listOf(Replan.Fired(now.minusSeconds(3 * 3600)), Replan.Repost, Replan.Arm)) {
                assertEquals(
                    "$trigger at $now",
                    planReminders(trigger, now, WARSAW, all, null),
                    planReminders(trigger, now, WARSAW, cleaned, null),
                )
            }
            val day = now.atZone(WARSAW).toLocalDate()
            assertEquals(
                "Today at $now",
                dosesOn(day, WARSAW, schedules, emptyList(), snoozes),
                dosesOn(day, WARSAW, schedules, emptyList(), kept),
            )
        }
    }
}
