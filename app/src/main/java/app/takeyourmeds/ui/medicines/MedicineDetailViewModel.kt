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
import kotlinx.coroutines.launch

data class MedicineDetailUiState(
    val medicine: MedicineEntity? = null,
    /**
     * The row is confirmed absent, as opposed to not read yet. Conflating the two is what makes a
     * detail screen bounce back to the list for a frame on every open.
     */
    val gone: Boolean = false,
)

class MedicineDetailViewModel(
    medicineId: String,
    private val repository: MedicineRepository,
) : ViewModel() {
    private var current: MedicineEntity? = null

    val state: StateFlow<MedicineDetailUiState> =
        repository
            .observeById(medicineId)
            .map { medicine ->
                current = medicine
                MedicineDetailUiState(medicine = medicine, gone = medicine == null)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicineDetailUiState())

    fun delete() {
        val medicine = current ?: return
        viewModelScope.launch { repository.delete(medicine) }
    }

    companion object {
        /** A factory *per id*: the id is a constructor argument and a `ViewModelStore` keys only on the class. */
        fun factoryFor(medicineId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicineDetailViewModel(medicineId, app.container.medicines)
                }
            }
    }
}
