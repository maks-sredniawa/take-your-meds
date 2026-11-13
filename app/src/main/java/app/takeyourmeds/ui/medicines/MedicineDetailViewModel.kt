package app.takeyourmeds.ui.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.work.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class MedicineDetailUiState(
    val medicine: MedicineEntity? = null,
    val times: List<LocalTime> = emptyList(),
    /**
     * The row is confirmed absent, as opposed to not read yet. Conflating the two is what makes a
     * detail screen bounce back to the list for a frame on every open.
     */
    val gone: Boolean = false,
)

class MedicineDetailViewModel(
    medicineId: String,
    private val repository: MedicineRepository,
    doses: DoseRepository,
    private val reminders: Reminders,
) : ViewModel() {
    private var current: MedicineEntity? = null

    val state: StateFlow<MedicineDetailUiState> =
        combine(repository.observeById(medicineId), doses.observeTimes(medicineId)) { medicine, times ->
            current = medicine
            MedicineDetailUiState(medicine = medicine, times = times, gone = medicine == null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicineDetailUiState())

    fun delete() {
        val medicine = current ?: return
        viewModelScope.launch { reminders.deleteMedicine(medicine.id) { repository.delete(medicine) } }
    }

    companion object {
        /** A factory *per id*: the id is a constructor argument and a `ViewModelStore` keys only on the class. */
        fun factoryFor(medicineId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicineDetailViewModel(
                        medicineId,
                        app.container.medicines,
                        app.container.doses,
                        app.container.reminders,
                    )
                }
            }
    }
}
