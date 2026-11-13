package app.takeyourmeds.ui.medicines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.takeyourmeds.R
import app.takeyourmeds.data.MedicineEntity
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.appViewModelExtras
import app.takeyourmeds.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    onOpenMedicine: (String) -> Unit,
    onAddMedicine: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicinesViewModel =
        viewModel(factory = MedicinesViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_medicines)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedicine) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.medicine_add))
            }
        },
    ) { insets ->
        when {
            state.loading -> Unit

            state.medicines.isEmpty() ->
                EmptyState(
                    message = stringResource(R.string.medicines_empty),
                    modifier = Modifier.padding(insets),
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(insets),
                    contentPadding = PaddingValues(Spacing.base),
                ) {
                    item(key = "count") {
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.medicines_count,
                                    state.medicines.size,
                                    state.medicines.size,
                                ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.tight),
                        )
                    }
                    items(state.medicines, key = { it.id }) { medicine ->
                        MedicineCard(medicine, onClick = { onOpenMedicine(medicine.id) })
                    }
                }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: MedicineEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.tight)
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(medicine.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = medicine.amount,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.hair),
            )
            medicine.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.hair),
                )
            }
        }
    }
}
