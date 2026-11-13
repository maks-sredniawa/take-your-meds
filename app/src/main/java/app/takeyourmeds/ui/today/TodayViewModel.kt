package app.takeyourmeds.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.Dose
import app.takeyourmeds.data.DoseKey
import app.takeyourmeds.data.DoseRecordEntity
import app.takeyourmeds.data.DoseRepository
import app.takeyourmeds.data.DoseState
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.data.MedicineRepository
import app.takeyourmeds.data.stateAt
import app.takeyourmeds.tip.HomeTipAsk
import app.takeyourmeds.work.Reminders
import app.takeyourmeds.work.Replan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class DoseRow(
    val key: DoseKey,
    val name: String,
    val amount: String,
    val state: DoseState,
    val takenAt: LocalTime? = null,
    val snoozedUntil: LocalTime? = null,
)

data class TimeGroup(
    val time: LocalTime,
    val doses: List<DoseRow>,
)

/** `loading` starts true so the empty state does not flash before the first read lands. */
data class TodayUiState(
    val groups: List<TimeGroup> = emptyList(),
    val loading: Boolean = true,
    val showTipAsk: Boolean = false,
)

private data class Moment(
    val now: Instant,
    val zone: ZoneId,
) {
    val day: LocalDate get() = now.atZone(zone).toLocalDate()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val medicines: MedicineRepository,
    private val doses: DoseRepository,
    private val tipAsk: HomeTipAsk,
    private val reminders: Reminders,
    ticks: Flow<Instant>,
    private val now: () -> Instant,
    private val zone: () -> ZoneId,
) : ViewModel() {
    private val justTaken = MutableStateFlow<DoseRecordEntity?>(null)

    private val offers = Channel<DoseKey>(Channel.CONFLATED)

    /** Each Dose marked Taken here, once, for the snackbar that offers its Undo. */
    val undoOffers: Flow<DoseKey> = offers.receiveAsFlow()

    // One collection of the clock, so a Dose list is never paired with another day's minute.
    private val today: Flow<Pair<Moment, List<Dose>>> =
        ticks.flatMapLatest { now ->
            val moment = Moment(now, zone())
            doses.observeDay(moment.day, moment.zone).map { moment to it }
        }

    val state: StateFlow<TodayUiState> =
        combine(today, medicines.observeAll(), tipAsk.due) { (moment, dayDoses), all, tip ->
            TodayUiState(
                groups = groupsOf(dayDoses, all.associateBy { it.id }, moment),
                loading = false,
                showTipAsk = tip,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        viewModelScope.launch { tipAsk.refreshIfDue() }
    }

    /** Only a Due Dose: an early answer would silence its Reminder (`docs/phase-1.md` §3). */
    fun markTaken(key: DoseKey) {
        val row =
            state.value.groups
                .flatMap { it.doses }
                .find { it.key == key } ?: return
        if (row.state != DoseState.Due) return
        viewModelScope.launch {
            val medicine = medicines.observeById(key.medicineId).first() ?: return@launch
            val record = reminders.answer(key) { doses.markTaken(medicine, key.scheduledAt, now()) }
            if (record == null) return@launch
            justTaken.value = record
            offers.send(key)
        }
    }

    fun undo(key: DoseKey) {
        val record = justTaken.value?.takeIf { DoseKey(it.medicineId, it.scheduledAt) == key } ?: return
        justTaken.value = null
        viewModelScope.launch {
            doses.undo(record)
            reminders.replan(Replan.Arm)
        }
    }

    fun undoExpired(key: DoseKey) {
        justTaken.update { record -> record?.takeUnless { DoseKey(it.medicineId, it.scheduledAt) == key } }
    }

    fun answerTipAsk() {
        viewModelScope.launch { tipAsk.answer() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    TodayViewModel(
                        app.container.medicines,
                        app.container.doses,
                        app.container.homeTipAsk,
                        app.container.reminders,
                        minuteTicks(),
                        Instant::now,
                        ZoneId::systemDefault,
                    )
                }
            }
    }
}

private fun groupsOf(
    today: List<Dose>,
    medicines: Map<String, MedicineEntity>,
    moment: Moment,
): List<TimeGroup> =
    today
        .mapNotNull { dose -> rowOf(dose, medicines[dose.medicineId], moment) }
        .groupBy { it.key.scheduledAt.toLocalTime() }
        .toSortedMap()
        .map { (time, rows) ->
            TimeGroup(time, rows.sortedWith(compareBy({ it.name.lowercase() }, { it.key.medicineId })))
        }

/** A Dose of a deleted Medicine is shown only while it carries its answer, under the name it was answered with. */
private fun rowOf(
    dose: Dose,
    medicine: MedicineEntity?,
    moment: Moment,
): DoseRow? {
    val record = dose.record
    val state = stateAt(dose, moment.now)
    return DoseRow(
        key = dose.key,
        name = medicine?.name ?: record?.medicineName ?: return null,
        amount = medicine?.amount ?: record?.medicineAmount ?: return null,
        state = state,
        takenAt = record?.answeredAt?.atZone(moment.zone)?.toLocalTime(),
        snoozedUntil =
            dose.snoozedUntil
                ?.takeIf { state == DoseState.Due }
                ?.atZone(moment.zone)
                ?.toLocalTime(),
    )
}
