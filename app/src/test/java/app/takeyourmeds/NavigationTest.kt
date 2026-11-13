package app.takeyourmeds

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

/** A Reminder's tap, `docs/phase-1.md` §5: Today without losing what was open. */
class NavigationTest {
    private fun shownToday(vararg stack: NavKey): List<NavKey> = NavBackStack(*stack).apply { showToday() }.toList()

    @Test
    fun `an editor left open stays under Today, for Back to return to`() {
        assertEquals(listOf(Medicines, MedicineEditor(), Today), shownToday(Medicines, MedicineEditor()))
    }

    @Test
    fun `another tab's root gives way to Today`() {
        assertEquals(listOf<NavKey>(Today), shownToday(Settings))
    }

    @Test
    fun `a stack holding Today drops back to it rather than holding it twice`() {
        assertEquals(listOf<NavKey>(Today), shownToday(Today, Support))
        assertEquals(listOf(Medicines, MedicineEditor(), Today), shownToday(Medicines, MedicineEditor(), Today))
    }
}
