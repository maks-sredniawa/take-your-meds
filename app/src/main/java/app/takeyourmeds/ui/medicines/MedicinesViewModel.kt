package app.takeyourmeds.ui.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.AppPreferences
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.tip.TipJar
import app.takeyourmeds.tip.tipAskDue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** `loading` starts true so the empty state does not flash before the first read lands. */
data class MedicinesUiState(
    val medicines: List<MedicineEntity> = emptyList(),
    val loading: Boolean = true,
    /** The tip's one ask (ADR-0007). False wherever Play will not sell a tip: nothing is asked for that cannot be bought. */
    val showTipAsk: Boolean = false,
)

class MedicinesViewModel(
    repository: MedicineRepository,
    private val preferences: AppPreferences,
    tipJar: TipJar,
    takenDoseCount: Flow<Int>,
    installedAt: Instant,
) : ViewModel() {
    val state: StateFlow<MedicinesUiState> =
        repository
            .observeAll()
            .map { medicines -> MedicinesUiState(medicines = medicines, loading = false) }
            .withTipAsk(preferences.tipAsked, tipJar.offers, takenDoseCount, installedAt)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicinesUiState())

    init {
        // Prices are asked for only once the ask could otherwise be due — one product query saved
        // on every other visit. The binding itself happens on every foreground anyway, to settle tips.
        viewModelScope.launch {
            val due = tipAskDue(preferences.tipAsked.first(), takenDoseCount.first(), installedAt, Instant.now())
            if (due) tipJar.refresh()
        }
    }

    /** Either answer — *Leave a tip* or *No thanks* — ends the ask on this install. */
    fun answerTipAsk() {
        viewModelScope.launch { preferences.markTipAsked() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicinesViewModel(
                        app.container.medicines,
                        app.container.preferences,
                        app.container.tipJar,
                        app.container.takenDoseCount,
                        app.container.installedAt,
                    )
                }
            }
    }
}
