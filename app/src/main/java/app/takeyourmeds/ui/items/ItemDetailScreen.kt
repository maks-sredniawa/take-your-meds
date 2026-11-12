package app.takeyourmeds.ui.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.takeyourmeds.R
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.appViewModelExtras
import app.takeyourmeds.ui.common.DetailScaffold

@Composable
fun ItemDetailScreen(
    itemId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemDetailViewModel =
        viewModel(factory = ItemDetailViewModel.factoryFor(itemId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // A row can vanish underneath this screen when it is deleted here. Leaving rather than rendering
    // an empty detail screen is the honest response, and it is a `LaunchedEffect` rather than an
    // `if` in the body because navigating during composition is a side effect and Compose will run
    // the body more than once.
    LaunchedEffect(state.gone) {
        if (state.gone) onDeleted()
    }

    val item = state.item
    DetailScaffold(
        title = item?.title ?: stringResource(R.string.item_detail_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = viewModel::delete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        },
    ) { contentModifier ->
        if (item != null) {
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()).padding(Spacing.base),
            ) {
                item.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    Text(notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
