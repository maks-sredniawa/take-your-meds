package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

/**
 * The Reminder's tag and its *Taken* action's `dose:` URI (`docs/phase-1.md` §5). Fails toward
 * *Taken* recorded against the wrong Dose: the app saying a medicine was taken that was not.
 */
class DoseKeyTest {
    private val medicineId = "6f1c2a9e-0b7d-4c35-9a51-2f8e4d7b3c10"
    private val key = DoseKey(medicineId, at(9))

    @Test
    fun `the tag is the documented shape`() {
        assertEquals("dose:$medicineId:2026-11-13T09:00", key.tag)
    }

    @Test
    fun `the tag round-trips to the same Medicine and scheduledAt`() {
        assertEquals(key, DoseKey.parse(key.tag))
    }

    @Test
    fun `the tag is a dose URI that round-trips through URI parsing`() {
        val uri = URI(key.tag)

        assertEquals("dose", uri.scheme)
        assertEquals(key, DoseKey.parse(uri.toString()))
    }

    @Test
    fun `two Medicines at one minute have different keys`() {
        assertNotEquals(key.tag, DoseKey("another-medicine", at(9)).tag)
    }

    @Test
    fun `one Medicine at two Times has different keys`() {
        assertNotEquals(key.tag, DoseKey(medicineId, at(21)).tag)
        assertNotEquals(key.tag, DoseKey(medicineId, at(9, day = DAY.plusDays(1))).tag)
    }

    @Test
    fun `anything else parses to nothing rather than to a Dose`() {
        assertNull(DoseKey.parse("dose:$medicineId"))
        assertNull(DoseKey.parse("other:$medicineId:2026-11-13T09:00"))
        assertNull(DoseKey.parse("dose::2026-11-13T09:00"))
        assertNull(DoseKey.parse("dose:$medicineId:2026-11-13T9:00"))
    }
}
