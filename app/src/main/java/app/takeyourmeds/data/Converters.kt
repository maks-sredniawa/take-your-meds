package app.takeyourmeds.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The one shape every local date-time column takes (`scheduledAt`, `validFrom`, `validUntil`).
 * Never `LocalDateTime.toString()`: it drops `:00` seconds on some values and keeps them on others,
 * and the day query is a string range that sorts as time only at a fixed width (phase-1.md §2).
 */
val LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.ROOT)

private val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun formatLocalDateTime(value: LocalDateTime): String = value.truncatedTo(ChronoUnit.MINUTES).format(LOCAL_DATE_TIME)

fun parseLocalDateTime(text: String): LocalDateTime = LocalDateTime.parse(text, LOCAL_DATE_TIME)

/**
 * How non-primitive types cross into SQLite.
 *
 * **Two rules here are load-bearing, and both are about not being able to change your mind later.**
 *
 * 1. **An `Instant` is stored as epoch millis**, so the column is a number SQLite can sort and
 *    range-query. A formatted string would sort wrong across timezones and need parsing to compare.
 *
 * 2. **An enum is stored by `name`, never by `ordinal`.** An ordinal is the enum's *position*, so
 *    inserting a new constant anywhere but the end silently rewrites the meaning of every row
 *    already on disk — a data-loss bug with no error and no migration to hang a fix on. Storing the
 *    name costs a few bytes per row and makes adding a constant free. Renaming one then becomes the
 *    breaking change, which is the right trade: a rename is a decision, a reorder is an accident.
 *
 * Kotlin note: `enumValueOf<T>(name)` throws on an unknown name. That is deliberate — a value in
 * the database that this build has never heard of means the file is from a newer version, which the
 * schema gate should have caught before Room ever opened it.
 */
class Converters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateTimeToText(value: LocalDateTime?): String? = value?.let(::formatLocalDateTime)

    @TypeConverter
    fun textToLocalDateTime(value: String?): LocalDateTime? = value?.let(::parseLocalDateTime)

    @TypeConverter
    fun timesToText(value: List<LocalTime>?): String? = value?.joinToString(",") { it.format(TIME_OF_DAY) }

    @TypeConverter
    fun textToTimes(value: String?): List<LocalTime>? = value?.split(',')?.map { LocalTime.parse(it, TIME_OF_DAY) }

    @TypeConverter
    fun doseAnswerToName(value: DoseAnswer?): String? = value?.name

    @TypeConverter
    fun nameToDoseAnswer(value: String?): DoseAnswer? = value?.let { enumValueOf<DoseAnswer>(it) }

    @TypeConverter
    fun themeModeToName(value: ThemeMode?): String? = value?.name

    @TypeConverter
    fun nameToThemeMode(value: String?): ThemeMode? = value?.let { enumValueOf<ThemeMode>(it) }
}
