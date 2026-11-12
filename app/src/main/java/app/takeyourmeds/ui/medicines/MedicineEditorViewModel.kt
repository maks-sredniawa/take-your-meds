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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicineEditorUiState(
    val name: String = "",
    val amount: String = "",
    val note: String = "",
    val saving: Boolean = false,
    /** Set once, when the save has landed. The screen watches it and leaves. */
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && amount.isNotBlank() && !saving
}

class MedicineEditorViewModel(
    private val medicineId: String?,
    private val repository: MedicineRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MedicineEditorUiState())
    val state: StateFlow<MedicineEditorUiState> = _state.asStateFlow()

    private var existing: MedicineEntity? = null

    init {
        // Read once rather than collected: a change arriving while the user types would overwrite
        // what they have half-written.
        if (medicineId != null) {
            viewModelScope.launch {
                val medicine = repository.observeById(medicineId).first() ?: return@launch
                existing = medicine
                _state.update {
                    it.copy(
                        name = medicine.name,
                        amount = medicine.amount,
                        note = medicine.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setAmount(value: String) = _state.update { it.copy(amount = value) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val name = snapshot.name.trim()
            val amount = snapshot.amount.trim()
            val note = snapshot.note.trim().takeIf(String::isNotBlank)
            val medicine =
                existing?.copy(name = name, amount = amount, note = note)
                    ?: MedicineEntity(name = name, amount = amount, note = note)
            repository.save(medicine)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        fun factoryFor(medicineId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicineEditorViewModel(medicineId, app.container.medicines)
                }
            }
    }
}
