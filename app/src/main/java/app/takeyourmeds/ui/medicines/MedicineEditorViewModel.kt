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
import app.takeyourmeds.data.normalisedTimes
import app.takeyourmeds.work.Reminders
import app.takeyourmeds.work.Replan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

data class MedicineEditorUiState(
    val name: String = "",
    val amount: String = "",
    val note: String = "",
    /** Sorted and distinct, as a Schedule stores them. Empty means the Medicine is not reminded. */
    val times: List<LocalTime> = emptyList(),
    val saving: Boolean = false,
    /** Set once, when the save has landed. The screen watches it and leaves. */
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && amount.isNotBlank() && !saving
}

class MedicineEditorViewModel(
    private val medicineId: String?,
    private val repository: MedicineRepository,
    private val doses: DoseRepository,
    private val reminders: Reminders,
    private val now: () -> LocalDateTime = LocalDateTime::now,
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
                val times = doses.timesOf(medicineId)
                existing = medicine
                _state.update {
                    it.copy(
                        name = medicine.name,
                        amount = medicine.amount,
                        note = medicine.note.orEmpty(),
                        times = times,
                    )
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setAmount(value: String) = _state.update { it.copy(amount = value) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun addTime(time: LocalTime) = _state.update { it.copy(times = normalisedTimes(it.times + time)) }

    fun changeTime(
        from: LocalTime,
        to: LocalTime,
    ) = _state.update { it.copy(times = normalisedTimes(it.times - from + to)) }

    fun removeTime(time: LocalTime) = _state.update { it.copy(times = it.times - time) }

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
            doses.saveTimes(medicine.id, snapshot.times, now())
            reminders.replan(Replan.Arm)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        fun factoryFor(medicineId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    MedicineEditorViewModel(
                        medicineId,
                        app.container.medicines,
                        app.container.doses,
                        app.container.reminders,
                    )
                }
            }
    }
}
