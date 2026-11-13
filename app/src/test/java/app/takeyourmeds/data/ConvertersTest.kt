package app.takeyourmeds.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The day query is a string range over `scheduledAt`, `validFrom` and `validUntil`, so every one of
 * them must be written at one fixed width (`docs/phase-1.md` §2).
 */
class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `a local date-time is written at a fixed width whatever its seconds`() {
        assertEquals("2026-11-13T09:00", converters.localDateTimeToText(LocalDateTime.of(2026, 11, 13, 9, 0)))
        assertEquals(
            "2026-11-13T09:00",
            converters.localDateTimeToText(LocalDateTime.of(2026, 11, 13, 9, 0, 30, 500_000_000)),
        )
        assertEquals("0999-01-02T00:05", converters.localDateTimeToText(LocalDateTime.of(999, 1, 2, 0, 5)))
    }

    @Test
    fun `a local date-time round-trips at minute resolution`() {
        val nine = LocalDateTime.of(2026, 11, 13, 9, 0)
        assertEquals(nine, converters.textToLocalDateTime(converters.localDateTimeToText(nine)))
    }

    @Test
    fun `Times are HH_mm, comma-joined, and round-trip`() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(21, 30))
        assertEquals("08:00,21:30", converters.timesToText(times))
        assertEquals(times, converters.textToTimes("08:00,21:30"))
    }

    @Test
    fun `an answer is stored by its name`() {
        assertEquals("Taken", converters.doseAnswerToName(DoseAnswer.Taken))
        assertEquals(DoseAnswer.Taken, converters.nameToDoseAnswer("Taken"))
    }
}
