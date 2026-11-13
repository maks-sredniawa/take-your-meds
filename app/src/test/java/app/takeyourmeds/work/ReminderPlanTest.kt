package app.takeyourmeds.work

import app.takeyourmeds.data.DAY
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseSources
import app.takeyourmeds.data.SnoozeEntity
import app.takeyourmeds.data.WARSAW
import app.takeyourmeds.data.at
import app.takeyourmeds.data.instant
import app.takeyourmeds.data.schedule
import app.takeyourmeds.data.taken
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

/** `docs/phase-1.md` §5's re-plan table: which trigger posts what, and where the one alarm goes next. */
class ReminderPlanTest {
    private val sources =
        DoseSources(
            schedules = listOf(schedule("a", "09:00", "21:00"), schedule("b", "09:05")),
            records = emptyList(),
            snoozes = emptyList(),
        )

    private fun plan(
        trigger: Replan,
        now: Instant,
        from: DoseSources = sources,
        lastArmed: Instant? = null,
    ): Pair<List<Pair<String, LocalDateTime>>, Instant?> {
        val plan = planReminders(trigger, now, WARSAW, from, lastArmed)
        return plan.post.map { it.medicineId to it.scheduledAt } to plan.armFor
    }

    @Test
    fun `a firing posts what lies between its armed time and now, then arms the next`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 5)) to instant(21),
            plan(Replan.Fired(instant(9)), instant(9, 6)),
        )
    }

    @Test
    fun `a boot after a Time re-posts what is still Due, then arms the next`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 5)) to instant(21),
            plan(Replan.Repost, instant(9, 30)),
        )
    }

    @Test
    fun `a re-plan that only arms posts nothing, however much is Due`() {
        assertEquals(emptyList<Pair<String, LocalDateTime>>() to instant(21), plan(Replan.Arm, instant(9, 30)))
    }

    @Test
    fun `a re-plan that only arms posts nothing while its armed alarm is still ahead`() {
        assertEquals(
            emptyList<Pair<String, LocalDateTime>>() to instant(9),
            plan(Replan.Arm, instant(8, 30), lastArmed = instant(9)),
        )
    }

    @Test
    fun `a re-plan that only arms, after its armed time and before the alarm lands, posts what the alarm would have`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 5)) to instant(21),
            plan(Replan.Arm, instant(9, 30), lastArmed = instant(9)),
        )
    }

    @Test
    fun `an overdue armed alarm is still bound by the re-post limit`() {
        assertEquals(
            listOf("b" to at(9, 5)) to instant(21),
            plan(Replan.Arm, instant(11, 4), lastArmed = instant(9)),
        )
    }

    @Test
    fun `a firing later than an alarm a re-plan replaced posts from the earlier one`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 5)) to instant(21),
            plan(Replan.Fired(instant(9, 5)), instant(9, 6), lastArmed = instant(9)),
        )
    }

    @Test
    fun `a posted Dose takes the same Medicine's earlier Reminders out of the tray, and no other's`() {
        val posted = planReminders(Replan.Fired(instant(21)), instant(21), WARSAW, sources, null).post
        val showing = listOf(DoseKey("a", at(9)), DoseKey("b", at(9, 5)), DoseKey("a", at(21)))
        assertEquals(listOf(DoseKey("a", at(9))), remindersReplacedBy(posted, showing))
    }

    @Test
    fun `no Dose ahead leaves no alarm`() {
        val ended = DoseSources(listOf(schedule("a", "09:00", until = at(10))), emptyList(), emptyList())
        assertEquals(null, plan(Replan.Arm, instant(10, 1), ended).second)
    }

    @Test
    fun `the rows the planner reads give the plan every row would`() {
        val everything =
            DoseSources(
                schedules =
                    listOf(
                        schedule("a", "00:00", "09:00", "23:30", until = at(12, day = DAY.minusDays(1))),
                        schedule("a", "00:00", "23:30", from = at(12, day = DAY.minusDays(1))),
                        schedule("b", "23:59"),
                    ),
                records =
                    (-5L..5L).flatMap { offset ->
                        val day = DAY.plusDays(offset)
                        listOf(taken("a", at(0, day = day)), taken("b", at(23, 59, day = day.minusDays(1))))
                    } - taken("a", at(0)),
                snoozes = listOf(SnoozeEntity("a", at(23, 30, day = DAY.minusDays(1)), instant(0, 20))),
            )
        val nows = listOf(instant(0, 10), instant(0, 20), instant(1, 25), instant(9), instant(23, 50), instant(23, 59))
        for (now in nows) {
            val (start, end) = sourceWindow(now, WARSAW)
            val read =
                DoseSources(
                    schedules =
                        everything.schedules.filter {
                            it.validFrom < end &&
                                (it.validUntil?.let { until -> until >= start } ?: true)
                        },
                    records = everything.records.filter { it.scheduledAt >= start && it.scheduledAt < end },
                    snoozes = everything.snoozes,
                )
            for (trigger in listOf(Replan.Fired(now.minusSeconds(3 * 3600)), Replan.Repost, Replan.Arm)) {
                assertEquals(
                    "$trigger at $now",
                    planReminders(trigger, now, WARSAW, everything, now.minusSeconds(3600)),
                    planReminders(trigger, now, WARSAW, read, now.minusSeconds(3600)),
                )
            }
        }
    }
}
