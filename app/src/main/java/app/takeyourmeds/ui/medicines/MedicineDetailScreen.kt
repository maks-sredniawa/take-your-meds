package app.takeyourmeds.ui.medicines

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
fun MedicineDetailScreen(
    medicineId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicineDetailViewModel =
        viewModel(factory = MedicineDetailViewModel.factoryFor(medicineId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigating is a side effect, and Compose may run the body more than once.
    LaunchedEffect(state.gone) {
        if (state.gone) onDeleted()
    }

    val medicine = state.medicine
    DetailScaffold(
        title = medicine?.name ?: stringResource(R.string.medicine_detail_title),
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
        if (medicine != null) {
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()).padding(Spacing.base),
            ) {
                LabelledValue(stringResource(R.string.medicine_amount_label), medicine.amount)
                medicine.note?.let { note ->
                    LabelledValue(
                        stringResource(R.string.medicine_note_label),
                        note,
                        Modifier.padding(top = Spacing.base),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = Spacing.hair),
        )
    }
}
