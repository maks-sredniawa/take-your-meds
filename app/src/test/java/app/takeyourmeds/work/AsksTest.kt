package app.takeyourmeds.work

import org.junit.Assert.assertEquals
import org.junit.Test

/** `docs/phase-1.md` §4's table: which ask the first Schedule's save makes next, per API level. */
class AsksTest {
    private fun grants(
        sdk: Int,
        notifications: Boolean,
        exact: Boolean,
    ) = Grants(sdk = sdk, notificationsAllowed = notifications, mutedChannel = null, exactAlarmsAllowed = exact)

    @Test
    fun `on API 34 a fresh install is asked for notifications first`() {
        assertEquals(Ask.Notifications, nextAsk(grants(34, notifications = false, exact = false), asked = emptySet()))
    }

    @Test
    fun `once notifications are allowed, a refused exact grant is handed off to settings`() {
        assertEquals(
            Ask.ExactAlarms,
            nextAsk(grants(34, notifications = true, exact = false), asked = setOf(Ask.Notifications)),
        )
    }

    @Test
    fun `a refused notifications ask ends the sequence without the exact hand-off`() {
        assertEquals(null, nextAsk(grants(34, notifications = false, exact = false), asked = setOf(Ask.Notifications)))
    }

    @Test
    fun `below API 33 there is no notifications permission to ask for, and nothing follows it`() {
        assertEquals(null, nextAsk(grants(32, notifications = false, exact = false), asked = emptySet()))
    }

    @Test
    fun `the exact hand-off is made once, whatever the user did in settings`() {
        assertEquals(
            null,
            nextAsk(grants(34, notifications = true, exact = false), asked = setOf(Ask.Notifications, Ask.ExactAlarms)),
        )
    }

    @Test
    fun `on API 31 and 32 the exact grant is on by default, so nothing is asked`() {
        assertEquals(null, nextAsk(grants(31, notifications = true, exact = true), asked = emptySet()))
    }

    @Test
    fun `on API 31 and 32 a revoked exact grant is still handed off`() {
        assertEquals(Ask.ExactAlarms, nextAsk(grants(32, notifications = true, exact = false), asked = emptySet()))
    }
}
