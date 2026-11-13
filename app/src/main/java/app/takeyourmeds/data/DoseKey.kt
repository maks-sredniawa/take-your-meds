package app.takeyourmeds.data

import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * A Dose's identity outside the database: the Reminder's notification tag and, as the same string,
 * the `dose:` URI its *Taken* action carries as `Intent` data (`docs/phase-1.md` §5).
 */
data class DoseKey(
    val medicineId: String,
    val scheduledAt: LocalDateTime,
) {
    val tag: String get() = "$SCHEME:$medicineId:${formatLocalDateTime(scheduledAt)}"

    companion object {
        private const val SCHEME = "dose"
        private val WIDTH = "yyyy-MM-ddTHH:mm".length

        /** The `scheduledAt` is read from the end, by its fixed width, because it has a colon of its own. */
        fun parse(text: String): DoseKey? {
            val body = text.removePrefix("$SCHEME:").takeIf { it.length < text.length } ?: return null
            val separator = body.length - WIDTH - 1
            if (separator < 1 || body[separator] != ':') return null
            val scheduledAt =
                try {
                    parseLocalDateTime(body.substring(separator + 1))
                } catch (_: DateTimeParseException) {
                    return null
                }
            return DoseKey(body.substring(0, separator), scheduledAt)
        }
    }
}
