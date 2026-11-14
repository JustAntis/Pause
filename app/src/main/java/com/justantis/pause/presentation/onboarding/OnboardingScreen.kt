package com.justantis.pause.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justantis.pause.R
import com.justantis.pause.presentation.components.InfoBanner
import com.justantis.pause.presentation.components.ObserveEffects
import com.justantis.pause.presentation.components.ValueStepper
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.components.openAccessibilitySettings
import com.justantis.pause.presentation.components.openUsageAccessSettings
import com.justantis.pause.presentation.theme.PauseTheme

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(OnboardingIntent.RefreshPermissions)
    }
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.OpenUsageAccessSettings -> context.openUsageAccessSettings()
            OnboardingEffect.OpenAccessibilitySettings -> context.openAccessibilitySettings()
            OnboardingEffect.Finished -> onFinished()
        }
    }
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) {
        viewModel.onIntent(OnboardingIntent.Back)
    }

    OnboardingScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep(state, onIntent)
                OnboardingStep.PERMISSIONS -> PermissionsStep(state, onIntent)
                OnboardingStep.GOALS -> GoalsStep(state, onIntent)
            }
        }
        Button(
            onClick = { onIntent(OnboardingIntent.Next) },
            enabled = state.canContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (state.isLastStep) R.string.onboarding_finish else R.string.onboarding_next
                )
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    StepTitle(
        title = stringResource(R.string.onboarding_welcome_title),
        subtitle = stringResource(R.string.onboarding_welcome_subtitle)
    )
    WelcomePoint(
        icon = Icons.Rounded.Visibility,
        title = stringResource(R.string.onboarding_point_awareness_title),
        text = stringResource(R.string.onboarding_point_awareness_text)
    )
    WelcomePoint(
        icon = Icons.Rounded.LocalFireDepartment,
        title = stringResource(R.string.onboarding_point_motivation_title),
        text = stringResource(R.string.onboarding_point_motivation_text)
    )
    WelcomePoint(
        icon = Icons.Rounded.SelfImprovement,
        title = stringResource(R.string.onboarding_point_autonomy_title),
        text = stringResource(R.string.onboarding_point_autonomy_text)
    )
    OutlinedTextField(
        value = state.userName,
        onValueChange = { onIntent(OnboardingIntent.NameChanged(it)) },
        label = { Text(text = stringResource(R.string.onboarding_name_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WelcomePoint(icon: ImageVector, title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PermissionsStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    StepTitle(
        title = stringResource(R.string.onboarding_permissions_title),
        subtitle = stringResource(R.string.onboarding_permissions_subtitle)
    )
    PermissionCard(
        icon = Icons.Rounded.QueryStats,
        title = stringResource(R.string.permission_usage_access_title),
        text = stringResource(R.string.permission_usage_access_text),
        granted = state.usageAccessGranted,
        onGrant = { onIntent(OnboardingIntent.GrantUsageAccess) }
    )
    PermissionCard(
        icon = Icons.Rounded.AccessibilityNew,
        title = stringResource(R.string.permission_accessibility_title),
        text = stringResource(R.string.permission_accessibility_text),
        granted = state.accessibilityGranted,
        onGrant = { onIntent(OnboardingIntent.GrantAccessibility) }
    )
    InfoBanner(
        text = stringResource(R.string.onboarding_privacy_info),
        icon = Icons.Rounded.Lock
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    text: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            if (granted) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.permission_granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                OutlinedButton(onClick = onGrant, shape = RoundedCornerShape(8.dp)) {
                    Text(text = stringResource(R.string.permission_grant))
                }
            }
        }
    }
}

@Composable
private fun GoalsStep(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    StepTitle(
        title = stringResource(R.string.onboarding_goals_title),
        subtitle = stringResource(R.string.onboarding_goals_subtitle)
    )
    GoalCard(
        title = stringResource(R.string.goal_screen_time_title),
        value = formatDuration(state.screenTimeGoalMinutes),
        onDecrease = { onIntent(OnboardingIntent.DecreaseScreenTimeGoal) },
        onIncrease = { onIntent(OnboardingIntent.IncreaseScreenTimeGoal) },
        decreaseEnabled = state.canDecreaseScreenTimeGoal,
        increaseEnabled = state.canIncreaseScreenTimeGoal
    )
    GoalCard(
        title = stringResource(R.string.goal_unlocks_title),
        value = state.unlockGoal.toString(),
        onDecrease = { onIntent(OnboardingIntent.DecreaseUnlockGoal) },
        onIncrease = { onIntent(OnboardingIntent.IncreaseUnlockGoal) },
        decreaseEnabled = state.canDecreaseUnlockGoal,
        increaseEnabled = state.canIncreaseUnlockGoal
    )
}

@Composable
private fun GoalCard(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ValueStepper(
                value = value,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                decreaseEnabled = decreaseEnabled,
                increaseEnabled = increaseEnabled
            )
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun OnboardingWelcomePreview() {
    PauseTheme {
        OnboardingScreen(state = OnboardingState(userName = "Roman"), onIntent = {})
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun OnboardingPermissionsPreview() {
    PauseTheme {
        OnboardingScreen(
            state = OnboardingState(step = OnboardingStep.PERMISSIONS, usageAccessGranted = true),
            onIntent = {}
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun OnboardingGoalsPreview() {
    PauseTheme {
        OnboardingScreen(state = OnboardingState(step = OnboardingStep.GOALS), onIntent = {})
    }
}
