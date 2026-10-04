package app.protein.tracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.protein.tracker.data.db.DayActivity
import app.protein.tracker.domain.ActivityInput
import app.protein.tracker.domain.Energy
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.GymLevel
import app.protein.tracker.domain.UserSettings
import app.protein.tracker.ui.components.SectionCard
import app.protein.tracker.ui.theme.NumberStyle
import kotlin.math.roundToInt

/**
 * Daily check-in for the automatic calorie target: did you work, how long, gym or not.
 * Shown open until you answer, then folds into one line with a "Change" button.
 */
@Composable
fun CheckInCard(
    settings: UserSettings,
    activity: DayActivity?,
    usualDay: ActivityInput,
    kcalTarget: Double,
    onSave: (ActivityInput) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!settings.profileComplete) {
        SectionCard(modifier) {
            Text("Set up your calorie target", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add your weight, height, age and sex in Settings. The app then adjusts your calories to your work and gym each day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onOpenSettings) { Text("Open Settings") }
        }
        return
    }

    var editing by remember(activity) { mutableStateOf(activity == null) }
    if (!editing && activity != null) {
        SectionCard(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(summary(activity.toInput()), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Today's calorie target: ${Fmt.kcal(kcalTarget)} kcal",
                        style = MaterialTheme.typography.bodyMedium.merge(NumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = { editing = true }) { Text("Change") }
            }
        }
        return
    }

    val start = activity?.toInput() ?: usualDay
    var worked by remember(activity) { mutableStateOf(start.worked) }
    var hours by remember(activity) { mutableStateOf(start.workHours.toFloat()) }
    var gym by remember(activity) { mutableStateOf(start.gym) }
    var minutes by remember(activity) { mutableStateOf(start.gymMinutes.toFloat()) }
    val input = ActivityInput(worked, hours.toDouble(), gym, minutes.roundToInt())
    val preview = Energy.dayTarget(settings, input) ?: kcalTarget

    SectionCard(modifier) {
        Text("Plan your day", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your calorie target follows your work and gym.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Text("Work today (${settings.jobType.label.lowercase()})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = worked,
                onClick = { worked = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Working") }
            SegmentedButton(
                selected = !worked,
                onClick = { worked = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Day off") }
        }
        if (worked) {
            Spacer(Modifier.height(8.dp))
            Text("${Fmt.amount(hours.toDouble())} hours", style = MaterialTheme.typography.bodyLarge.merge(NumberStyle))
            Slider(value = hours, onValueChange = { hours = it }, valueRange = 1f..12f, steps = 21)
        }

        Spacer(Modifier.height(12.dp))
        Text("Gym", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GymLevel.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    selected = gym == level,
                    onClick = { gym = level },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = GymLevel.entries.size),
                ) { Text(if (level == GymLevel.NONE) "No" else level.label, maxLines = 1) }
            }
        }
        if (gym != GymLevel.NONE) {
            Spacer(Modifier.height(8.dp))
            Text("${minutes.roundToInt()} minutes", style = MaterialTheme.typography.bodyLarge.merge(NumberStyle))
            Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 15f..150f, steps = 8)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Calorie target", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${Fmt.kcal(preview)} kcal",
                    style = MaterialTheme.typography.titleLarge.merge(NumberStyle),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(onClick = { onSave(input) }, modifier = Modifier.padding(start = 8.dp)) { Text("Save") }
        }
    }
}

private fun summary(input: ActivityInput): String {
    val work = if (input.worked) "Worked ${Fmt.amount(input.workHours)} h" else "Day off"
    val gym = if (input.gym == GymLevel.NONE) "no gym" else "gym ${input.gym.label.lowercase()} ${input.gymMinutes} min"
    return "$work · $gym"
}
