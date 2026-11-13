package app.takeyourmeds.ui.asks

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import app.takeyourmeds.theme.Spacing
import app.takeyourmeds.ui.appViewModelExtras
import app.takeyourmeds.work.Ask
import app.takeyourmeds.work.openExactAlarmSettings
import app.takeyourmeds.work.readGrants
import app.takeyourmeds.work.rememberNotificationPermissionAsk

/**
 * The app's own explanation before each system ask, made once the first Schedule is saved
 * (`docs/phase-1.md` §4). The system dialog is never the first thing the user sees (ADR-0003).
 */
@Composable
fun AsksScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AsksViewModel = viewModel(factory = AsksViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reread(context.readGrants()) }

    val askNotifications =
        rememberNotificationPermissionAsk { viewModel.answered(Ask.Notifications, context.readGrants()) }

    LaunchedEffect(state.ask) {
        if (state.ask == null) onDone()
    }

    Scaffold(modifier = modifier) { insets ->
        when (state.ask) {
            Ask.Notifications ->
                AskContent(
                    title = R.string.ask_notifications_title,
                    body = R.string.ask_notifications_body,
                    action = R.string.ask_notifications_action,
                    onAct = askNotifications,
                    onNotNow = viewModel::notNow,
                    modifier = Modifier.padding(insets),
                )

            Ask.ExactAlarms ->
                AskContent(
                    title = R.string.ask_exact_title,
                    body = R.string.ask_exact_body,
                    action = R.string.ask_exact_action,
                    onAct = {
                        context.openExactAlarmSettings()
                        viewModel.answered(Ask.ExactAlarms, context.readGrants())
                    },
                    onNotNow = viewModel::notNow,
                    modifier = Modifier.padding(insets),
                )

            null -> Unit
        }
    }
}

@Composable
private fun AskContent(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes action: Int,
    onAct: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.base, vertical = Spacing.block),
        verticalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(body), style = MaterialTheme.typography.bodyLarge)
        Column(
            modifier = Modifier.padding(top = Spacing.section),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        ) {
            Button(onClick = onAct, modifier = Modifier.fillMaxWidth()) { Text(stringResource(action)) }
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ask_not_now))
            }
        }
    }
}
