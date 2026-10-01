package app.protein.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.protein.tracker.domain.Fmt
import app.protein.tracker.ui.theme.NumberStyle

private const val FILL_MILLIS = 1100

/**
 * The big protein ring. It fills from empty when it first appears. Past 100 % a thinner second
 * lap is drawn in a calm second colour — never red.
 */
@Composable
fun ProteinRing(
    eaten: Double,
    target: Double,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
    strokeWidth: Dp = 22.dp,
    showLabels: Boolean = true,
) {
    val fraction = if (target > 0) (eaten / target).toFloat() else 0f
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        progress.animateTo(fraction, tween(FILL_MILLIS, easing = FastOutSlowInEasing))
    }
    val shownGrams = remember { Animatable(0f) }
    LaunchedEffect(eaten) {
        shownGrams.animateTo(eaten.toFloat(), tween(FILL_MILLIS, easing = FastOutSlowInEasing))
    }

    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = MaterialTheme.colorScheme.primary
    val extra = MaterialTheme.colorScheme.tertiary

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val p = progress.value
            val main = p.coerceIn(0f, 1f)
            if (main > 0f) {
                drawArc(
                    color = fill,
                    startAngle = -90f,
                    sweepAngle = 360f * main,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            if (p > 1f) {
                drawArc(
                    color = extra,
                    startAngle = -90f,
                    sweepAngle = 360f * (p - 1f).coerceAtMost(1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 0.42f, cap = StrokeCap.Round),
                )
            }
        }
        if (showLabels) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${Fmt.protein(shownGrams.value.toDouble())} g",
                    style = MaterialTheme.typography.displayMedium.merge(NumberStyle),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "of ${Fmt.protein(target)} g protein",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    proteinStatus(eaten, target),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Neutral wording either side of the target: no warnings, no guilt. */
fun proteinStatus(eaten: Double, target: Double): String {
    val remaining = target - eaten
    return when {
        remaining >= 0.05 -> "${Fmt.protein(remaining)} g to go"
        -remaining < 0.5 -> "Target reached"
        else -> "Target reached · +${Fmt.protein(-remaining)} g"
    }
}

@Composable
fun CalorieBar(kcal: Double, target: Double, modifier: Modifier = Modifier) {
    val fraction = if (target > 0) (kcal / target).toFloat().coerceIn(0f, 1f) else 0f
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        progress.animateTo(fraction, tween(FILL_MILLIS, easing = FastOutSlowInEasing))
    }
    val difference = target - kcal
    val caption = when {
        difference >= 1 -> "${Fmt.kcal(difference)} kcal left"
        difference > -1 -> "Target reached"
        else -> "${Fmt.kcal(-difference)} kcal above target"
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Calories", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${Fmt.kcal(kcal)} / ${Fmt.kcal(target)} kcal",
                style = MaterialTheme.typography.bodyMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
