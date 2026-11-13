package app.takeyourmeds.ui.today

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/** Now, and again at the start of every minute, the resolution a Dose turns Due at. */
fun minuteTicks(now: () -> Instant = Instant::now): Flow<Instant> =
    flow {
        while (true) {
            val instant = now()
            emit(instant)
            delay(MINUTE_MILLIS - instant.toEpochMilli() % MINUTE_MILLIS)
        }
    }

private const val MINUTE_MILLIS = 60_000L
