package app.takeyourmeds.tip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TipAskTest {
    private val installedAt = Instant.parse("2026-09-01T08:00:00Z")
    private val monthLater = installedAt + TIP_ASK_AFTER

    private fun due(
        asked: Boolean = false,
        takenDoses: Int = TIP_ASK_AFTER_TAKEN_DOSES,
        now: Instant = monthLater,
    ) = tipAskDue(asked, takenDoses, installedAt, now)

    @Test
    fun `due once installed a month with enough Taken Doses`() {
        assertTrue(due())
    }

    @Test
    fun `not due a millisecond short of the month`() {
        assertFalse(due(takenDoses = 1_000, now = monthLater.minusMillis(1)))
    }

    @Test
    fun `not due one Taken Dose short`() {
        assertFalse(due(takenDoses = TIP_ASK_AFTER_TAKEN_DOSES - 1, now = monthLater.plus(TIP_ASK_AFTER)))
    }

    @Test
    fun `never again once answered`() {
        assertFalse(due(asked = true, takenDoses = 1_000, now = monthLater.plus(TIP_ASK_AFTER)))
    }
}
