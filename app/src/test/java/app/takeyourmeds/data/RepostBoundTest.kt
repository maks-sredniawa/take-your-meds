package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

/**
 * `dosesToRepost` and the same bound inside `dosesToPost` (`docs/phase-1.md` §5). Fails **chiefly**
 * toward a stale Reminder posted as current, which can become a second dose of the same Medicine;
 * secondarily toward a phone off over 9:00 left silent.
 */
class RepostBoundTest {
    private fun reposted(
        now: Instant,
        vararg schedules: ScheduleEntity,
        records: List<DoseRecordEntity> = emptyList(),
        snoozes: List<SnoozeEntity> = emptyList(),
    ): List<Pair<String, LocalDateTime>> =
        dosesToRepost(now, WARSAW, schedules.toList(), records, snoozes).map { it.medicineId to it.scheduledAt }

    private val none = emptyList<Pair<String, LocalDateTime>>()

    @Test
    fun `the limit is two hours`() {
        assertEquals(java.time.Duration.ofHours(2), DUE_REPOST_LIMIT)
    }

    @Test
    fun `a phone off over 9_00 and on at 9_30 re-posts it`() {
        assertEquals(listOf("a" to at(9)), reposted(instant(9, 30), schedule("a", "09:00")))
    }

    @Test
    fun `re-posted just inside the limit, not at it or past it`() {
        assertEquals(listOf("a" to at(9)), reposted(instant(10, 59, second = 59), schedule("a", "09:00")))
        assertEquals(none, reposted(instant(11), schedule("a", "09:00")))
        assertEquals(none, reposted(instant(20), schedule("a", "09:00")))
    }

    @Test
    fun `not once a later Dose of the same Medicine has reached its Time`() {
        val twice = schedule("a", "09:00", "10:00")

        assertEquals(listOf("a" to at(9)), reposted(instant(9, 59), twice))
        assertEquals(listOf("a" to at(10)), reposted(instant(10), twice))
    }

    @Test
    fun `a later Dose that is already answered still ends the earlier one`() {
        assertEquals(
            none,
            reposted(instant(10, 5), schedule("a", "09:00", "10:00"), records = listOf(taken("a", at(10)))),
        )
    }

    @Test
    fun `another Medicine's later Dose ends nothing`() {
        assertEquals(
            listOf("a" to at(9), "b" to at(9, 30)),
            reposted(instant(9, 45), schedule("a", "09:00"), schedule("b", "09:30")),
        )
    }

    @Test
    fun `measured from the Reminder time, so a Snooze's end counts`() {
        val snooze = SnoozeEntity("a", at(9), instant(9) + SNOOZE_LENGTH)

        assertEquals(listOf("a" to at(9)), reposted(instant(11, 9), schedule("a", "09:00"), snoozes = listOf(snooze)))
        assertEquals(none, reposted(instant(11, 10), schedule("a", "09:00"), snoozes = listOf(snooze)))
    }

    @Test
    fun `a Snooze still pending is not re-posted before it ends`() {
        val snooze = SnoozeEntity("a", at(9), instant(9, 10))

        assertEquals(none, reposted(instant(9, 5), schedule("a", "09:00"), snoozes = listOf(snooze)))
    }

    @Test
    fun `answered and upcoming Doses are not re-posted`() {
        assertEquals(
            none,
            reposted(
                instant(9, 30),
                schedule("a", "09:00"),
                schedule("b", "10:00"),
                records = listOf(taken("a", at(9))),
            ),
        )
    }

    @Test
    fun `the snooze length is ten minutes`() {
        assertEquals(java.time.Duration.ofMinutes(10), SNOOZE_LENGTH)
    }

    @Test
    fun `a fallback alarm landing at or past the limit posts nothing stale`() {
        assertEquals(
            listOf("a" to at(9)),
            dosesToPost(instant(9), instant(10, 59), WARSAW, listOf(schedule("a", "09:00")), emptyList(), emptyList())
                .map { it.medicineId to it.scheduledAt },
        )
        assertEquals(
            none,
            dosesToPost(instant(9), instant(11), WARSAW, listOf(schedule("a", "09:00")), emptyList(), emptyList())
                .map { it.medicineId to it.scheduledAt },
        )
    }

    @Test
    fun `a late landing does not post an earlier Dose a later one of the same Medicine has replaced`() {
        assertEquals(
            listOf("a" to at(9, 30)),
            dosesToPost(
                instant(9),
                instant(9, 40),
                WARSAW,
                listOf(schedule("a", "09:00", "09:30")),
                emptyList(),
                emptyList(),
            ).map { it.medicineId to it.scheduledAt },
        )
    }
}
