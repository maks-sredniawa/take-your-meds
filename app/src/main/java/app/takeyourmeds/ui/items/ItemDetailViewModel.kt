package app.takeyourmeds.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.takeyourmeds.MainApplication
import app.takeyourmeds.data.ItemEntity
import app.takeyourmeds.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemDetailUiState(
    val item: ItemEntity? = null,
    /**
     * The row is confirmed absent, as opposed to not read yet.
     *
     * Two different things that both render as "no item", and conflating them is what makes a detail
     * screen bounce back to the list for a frame on every open. `loaded` is what tells them apart.
     */
    val gone: Boolean = false,
)

class ItemDetailViewModel(
    itemId: String,
    private val repository: ItemRepository,
) : ViewModel() {
    private var current: ItemEntity? = null

    val state: StateFlow<ItemDetailUiState> =
        repository
            .observeById(itemId)
            .map { item ->
                current = item
                ItemDetailUiState(item = item, gone = item == null)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState())

    fun delete() {
        val item = current ?: return
        viewModelScope.launch { repository.delete(item) }
    }

    companion object {
        /**
         * A factory *per id*, because the id is a constructor argument and a `ViewModelStore` keys
         * only on the class. Nav3 gives each entry its own store, so two detail screens on the stack
         * do not share one — but the factory still has to know which id it is building for.
         */
        fun factoryFor(itemId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    ItemDetailViewModel(itemId, app.container.items)
                }
            }
    }
}
