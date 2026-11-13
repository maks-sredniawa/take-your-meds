package app.takeyourmeds.tip

import app.takeyourmeds.data.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant

/** The home screen's one mention of the tip (ADR-0007). */
interface HomeTipAsk {
    /** False wherever Play will not sell a tip: nothing is asked for that cannot be bought. */
    val due: Flow<Boolean>

    /** Asks Play for prices only once the ask could otherwise be due. */
    suspend fun refreshIfDue()

    /** Either answer, *Leave a tip* or *No thanks*, ends the ask on this install. */
    suspend fun answer()
}

class PlayHomeTipAsk(
    private val preferences: AppPreferences,
    private val tipJar: TipJar,
    private val takenDoses: Flow<Int>,
    private val installedAt: Instant,
) : HomeTipAsk {
    override val due: Flow<Boolean> =
        combine(preferences.tipAsked, tipJar.offers, takenDoses) { asked, offers, taken ->
            offers.isNotEmpty() && tipAskDue(asked, taken, installedAt, Instant.now())
        }

    override suspend fun refreshIfDue() {
        if (tipAskDue(preferences.tipAsked.first(), takenDoses.first(), installedAt, Instant.now())) tipJar.refresh()
    }

    override suspend fun answer() = preferences.markTipAsked()
}
