package app.takeyourmeds.ui.medicines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.takeyourmeds.R
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.appViewModelExtras
import app.takeyourmeds.ui.common.DetailScaffold
import app.takeyourmeds.work.nextAsk
import app.takeyourmeds.work.readGrants

@Composable
fun MedicineEditorScreen(
    medicineId: String?,
    onDone: () -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicineEditorViewModel =
        viewModel(factory = MedicineEditorViewModel.factoryFor(medicineId), extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Leave once the save has landed, not when the button is pressed: navigating first would cancel
    // the `viewModelScope` coroutine doing the write.
    LaunchedEffect(state.saved) {
        if (!state.saved) return@LaunchedEffect
        if (state.firstSchedule && nextAsk(context.readGrants(), asked = emptySet()) != null) onAsk() else onDone()
    }

    DetailScaffold(
        title = stringResource(if (medicineId == null) R.string.medicine_add else R.string.action_edit),
        onBack = onDone,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState()).padding(vertical = Spacing.base),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.medicine_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base),
            )
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::setAmount,
                label = { Text(stringResource(R.string.medicine_amount_label)) },
                supportingText = { Text(stringResource(R.string.medicine_amount_hint)) },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = Spacing.base,
                        end = Spacing.base,
                        top = Spacing.snug,
                    ),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.medicine_note_label)) },
                supportingText = { Text(stringResource(R.string.medicine_note_hint)) },
                minLines = 3,
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = Spacing.base,
                        end = Spacing.base,
                        top = Spacing.snug,
                    ),
            )
            TimesSection(
                times = state.times,
                onAdd = viewModel::addTime,
                onChange = viewModel::changeTime,
                onRemove = viewModel::removeTime,
            )
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = Spacing.base,
                        end = Spacing.base,
                        top = Spacing.section,
                    ),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
