package app.takeyourmeds.ui.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** `loading` starts true so the empty state does not flash before the first read lands. */
data class MedicinesUiState(
    val medicines: List<MedicineEntity> = emptyList(),
    val loading: Boolean = true,
)

class MedicinesViewModel(
    repository: MedicineRepository,
) : ViewModel() {
    val state: StateFlow<MedicinesUiState> =
        repository
            .observeAll()
            .map { medicines -> MedicinesUiState(medicines = medicines, loading = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicinesUiState())

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicinesViewModel(app.container.medicines)
                }
            }
    }
}
