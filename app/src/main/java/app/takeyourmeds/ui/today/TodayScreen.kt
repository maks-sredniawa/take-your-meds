package app.takeyourmeds.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.takeyourmeds.R
import app.takeyourmeds.data.DoseState
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.appViewModelExtras
import app.takeyourmeds.ui.common.BannerTone
import app.takeyourmeds.ui.common.EmptyState
import app.takeyourmeds.ui.common.WarningBanner
import app.takeyourmeds.ui.common.rememberTimeFormatter
import app.takeyourmeds.work.openAppNotificationSettings
import app.takeyourmeds.work.openChannelNotificationSettings
import app.takeyourmeds.work.openExactAlarmSettings
import app.takeyourmeds.work.readGrants
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reread(context.readGrants()) }
    val snackbar = remember { SnackbarHostState() }
    val marked = stringResource(R.string.dose_marked_taken)
    val undo = stringResource(R.string.dose_undo)

    LaunchedEffect(viewModel) {
        viewModel.undoOffers.collectLatest { key ->
            when (snackbar.showSnackbar(marked, actionLabel = undo, duration = SnackbarDuration.Long)) {
                SnackbarResult.ActionPerformed -> viewModel.undo(key)
                SnackbarResult.Dismissed -> viewModel.undoExpired(key)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_today)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        when {
            state.loading -> Unit

            state.groups.isEmpty() && !state.showTipAsk && !state.blocked && !state.late ->
                EmptyState(message = stringResource(R.string.today_empty), modifier = Modifier.padding(insets))

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(insets),
                    contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.tight),
                ) {
                    if (state.blocked) {
                        item(key = "blocked") {
                            WarningBanner(
                                title = stringResource(R.string.today_blocked_title),
                                body = stringResource(R.string.today_blocked_body),
                                actionLabel = stringResource(R.string.today_blocked_action),
                                onAct = {
                                    val channel = state.blockedChannel
                                    if (channel == null) {
                                        context.openAppNotificationSettings()
                                    } else {
                                        context.openChannelNotificationSettings(channel)
                                    }
                                },
                                modifier = Modifier.padding(top = Spacing.tight),
                            )
                        }
                    }
                    if (state.late) {
                        item(key = "late") {
                            WarningBanner(
                                title = stringResource(R.string.today_late_title),
                                body = stringResource(R.string.today_late_body_minutes),
                                actionLabel = stringResource(R.string.today_late_action),
                                onAct = { context.openExactAlarmSettings() },
                                modifier = Modifier.padding(top = Spacing.tight),
                                tone = BannerTone.Tertiary,
                            )
                        }
                    }
                    if (state.showTipAsk) {
                        item(key = "tip-ask") {
                            TipAskCard(
                                onTip = {
                                    viewModel.answerTipAsk()
                                    onOpenSupport()
                                },
                                onDecline = viewModel::answerTipAsk,
                            )
                        }
                    }
                    if (state.groups.isEmpty()) {
                        item(key = "empty") { EmptyState(stringResource(R.string.today_empty)) }
                    }
                    for (group in state.groups) {
                        item(key = "time-${group.time}") { TimeHeader(group) }
                        items(group.doses, key = { it.key.tag }) { dose ->
                            DoseCard(dose, onMarkTaken = { viewModel.markTaken(dose.key) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun TimeHeader(group: TimeGroup) {
    val format = rememberTimeFormatter()
    Text(
        text = format(group.time),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .padding(top = Spacing.base, bottom = Spacing.tight)
                .semantics { heading() },
    )
}

@Composable
private fun DoseCard(
    dose: DoseRow,
    onMarkTaken: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.tight)
    when (dose.state) {
        DoseState.Due ->
            Card(modifier = modifier) {
                DoseContent(dose) {
                    Text(
                        text = stringResource(R.string.dose_due),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    dose.snoozedUntil?.let { until ->
                        val format = rememberTimeFormatter()
                        Text(
                            text = stringResource(R.string.dose_snoozed_until, format(until)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onMarkTaken,
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .heightIn(min = LocalMinimumInteractiveComponentSize.current),
                    ) {
                        Text(stringResource(R.string.dose_mark_taken))
                    }
                }
            }

        DoseState.Taken ->
            OutlinedCard(modifier = modifier) {
                DoseContent(dose) {
                    val format = rememberTimeFormatter()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.dose_taken_at, dose.takenAt?.let(format).orEmpty()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

        DoseState.Upcoming -> OutlinedCard(modifier = modifier) { DoseContent(dose) {} }
    }
}

@Composable
private fun DoseContent(
    dose: DoseRow,
    state: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.hair),
    ) {
        Text(dose.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = dose.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state()
    }
}
