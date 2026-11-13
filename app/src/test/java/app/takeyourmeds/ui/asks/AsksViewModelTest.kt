package app.takeyourmeds.ui.asks

import app.takeyourmeds.work.Ask
import app.takeyourmeds.work.Grants
import org.junit.Assert.assertEquals
import org.junit.Test

class AsksViewModelTest {
    private fun grants(
        notifications: Boolean,
        exact: Boolean,
    ) = Grants(sdk = 34, notificationsAllowed = notifications, mutedChannel = null, exactAlarmsAllowed = exact)

    @Test
    fun `granting notifications in the system dialog moves on to the exact hand-off`() {
        val asks = AsksViewModel(grants(notifications = false, exact = false))
        assertEquals(Ask.Notifications, asks.state.value.ask)

        asks.answered(Ask.Notifications, grants(notifications = true, exact = false))

        assertEquals(Ask.ExactAlarms, asks.state.value.ask)
    }

    @Test
    fun `Not now ends the sequence, and a later resume does not start it again`() {
        val asks = AsksViewModel(grants(notifications = false, exact = false))

        asks.notNow()
        asks.reread(grants(notifications = false, exact = false))

        assertEquals(null, asks.state.value.ask)
    }

    @Test
    fun `a resume that finds the step already granted moves past it`() {
        val asks = AsksViewModel(grants(notifications = true, exact = false))

        asks.reread(grants(notifications = true, exact = true))

        assertEquals(null, asks.state.value.ask)
    }
}
