package app.takeyourmeds.ui.medicines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.takeyourmeds.R
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.common.SectionHeader
import app.takeyourmeds.ui.common.is24Hour
import app.takeyourmeds.ui.common.rememberTimeFormatter
import java.time.LocalTime

private const val ADDING = -1

/** A Medicine's Times, every day. Saved as a Schedule with the rest of the editor. */
@Composable
internal fun TimesSection(
    times: List<LocalTime>,
    onAdd: (LocalTime) -> Unit,
    onChange: (LocalTime, LocalTime) -> Unit,
    onRemove: (LocalTime) -> Unit,
) {
    val format = rememberTimeFormatter()
    // Minute of the day being edited, or ADDING; an Int so it survives rotation and process death.
    var picking by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.schedule_times_label))
        Text(
            text = stringResource(R.string.schedule_every_day),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )
        if (times.isEmpty()) {
            Text(
                text = stringResource(R.string.schedule_no_times),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.snug),
            )
        }
        for (time in times) {
            val label = format(time)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = LocalMinimumInteractiveComponentSize.current)
                        .clickable { picking = time.toSecondOfDay() / 60 }
                        .padding(start = Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(time) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.schedule_remove_time, label))
                }
            }
        }
        TextButton(
            onClick = { picking = ADDING },
            modifier = Modifier.padding(horizontal = Spacing.tight),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.schedule_add_time), modifier = Modifier.padding(start = Spacing.tight))
        }
    }

    picking?.let { minute ->
        val editing = if (minute == ADDING) null else LocalTime.ofSecondOfDay(minute * 60L)
        TimeDialog(
            initial = editing ?: LocalTime.of(8, 0),
            onDismiss = { picking = null },
            onConfirm = { chosen ->
                if (editing == null) onAdd(chosen) else onChange(editing, chosen)
                picking = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state =
        rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour())
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_time_picker_title)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.schedule_time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        TimePicker(state = state)
    }
}
