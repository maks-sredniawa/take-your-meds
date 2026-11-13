package app.takeyourmeds.ui.asks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.work.Ask
import app.takeyourmeds.work.Grants
import app.takeyourmeds.work.nextAsk
import app.takeyourmeds.work.readGrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A null [ask] means the sequence is over and the screen leaves for Today. */
data class AsksUiState(
    val ask: Ask?,
)

class AsksViewModel(
    initial: Grants,
) : ViewModel() {
    private var asked = emptySet<Ask>()

    private val _state = MutableStateFlow(AsksUiState(nextAsk(initial, asked)))
    val state: StateFlow<AsksUiState> = _state.asStateFlow()

    fun answered(
        ask: Ask,
        grants: Grants,
    ) {
        asked = asked + ask
        reread(grants)
    }

    fun reread(grants: Grants) {
        if (_state.value.ask == null) return
        _state.value = AsksUiState(nextAsk(grants, asked))
    }

    fun notNow() {
        _state.value = AsksUiState(null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AsksViewModel(this[APPLICATION_KEY]!!.readGrants()) }
            }
    }
}
