package app.takeyourmeds.ui.common

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Whether the phone shows 24-hour time, the choice a Time and the time picker both follow. */
@Composable
fun is24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

@Composable
fun rememberTimeFormatter(): (LocalTime) -> String {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val is24Hour = is24Hour()
    return remember(locale, is24Hour) { timeFormatter(locale, is24Hour) }
}

fun Context.timeFormatter(): (LocalTime) -> String =
    timeFormatter(
        ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault(),
        DateFormat.is24HourFormat(this),
    )

private fun timeFormatter(
    locale: Locale,
    is24Hour: Boolean,
): (LocalTime) -> String {
    val formatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    return { time -> time.format(formatter) }
}
