package app.protein.tracker.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.protein.tracker.domain.DayTotal
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.WeekSummary
import app.protein.tracker.ui.theme.NumberStyle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

/**
 * Seven bars of daily protein, with the 7-day average drawn across them as a bold line.
 * The target is a faint dashed line. Bars are muted on purpose: the average is the star.
 */
@Composable
fun WeeklyChart(
    week: WeekSummary,
    target: Double,
    today: Long,
    modifier: Modifier = Modifier,
) {
    val values = week.totals.map { it?.protein ?: 0.0 }
    val average = week.avgProtein
    val top = maxOf(values.maxOrNull() ?: 0.0, target, average ?: 0.0).coerceAtLeast(1.0) * 1.15

    val grow = remember { Animatable(0f) }
    LaunchedEffect(week.days.firstOrNull()) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val measurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val todayBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val targetColor = MaterialTheme.colorScheme.outline
    val averageColor = MaterialTheme.colorScheme.primary
    val labelBackground = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelMedium.merge(NumberStyle).copy(
        color = MaterialTheme.colorScheme.onPrimary,
    )
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val slot = size.width / 7f
            val barWidth = slot * 0.56f
            val chartHeight = size.height
            fun yOf(value: Double): Float = chartHeight - (value / top).toFloat() * chartHeight

            val targetY = yOf(target)
            drawLine(
                color = targetColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
            )

            values.forEachIndexed { index, value ->
                val left = slot * index + (slot - barWidth) / 2f
                if (value > 0.0) {
                    val height = (value / top).toFloat() * chartHeight * grow.value
                    drawRoundRect(
                        color = if (week.days.getOrNull(index) == today) todayBarColor else barColor,
                        topLeft = Offset(left, chartHeight - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                    )
                } else {
                    val height = 4.dp.toPx()
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(left, chartHeight - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(height / 2f, height / 2f),
                    )
                }
            }

            if (average != null) {
                val averageY = yOf(average)
                drawLine(
                    color = averageColor,
                    start = Offset(0f, averageY),
                    end = Offset(size.width, averageY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    alpha = grow.value,
                )
                val layout = measurer.measure("avg ${Fmt.protein(average)} g", style = labelStyle)
                val padH = 8.dp.toPx()
                val padV = 3.dp.toPx()
                val pillWidth = layout.size.width + padH * 2
                val pillHeight = layout.size.height + padV * 2
                val pillLeft = size.width - pillWidth
                val pillTop = (averageY - pillHeight - 6.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = labelBackground,
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
                    alpha = grow.value,
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(pillLeft + padH, pillTop + padV),
                    alpha = grow.value,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            week.days.forEachIndexed { index, day ->
                val isToday = day == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        LocalDate.ofEpochDay(day).format(dayFormat),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val value = values.getOrElse(index) { 0.0 }
                    Text(
                        if (week.totals.getOrNull(index) != null) Fmt.protein(value) else "–",
                        style = MaterialTheme.typography.labelSmall.merge(NumberStyle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A month grid. Each logged day is a circle whose shade shows how close it came to the
 * protein target: lighter = less, darker = closer. Tap any past day to open it.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    today: Long,
    totals: Map<Long, DayTotal>,
    target: Double,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val length = month.lengthOfMonth()
    val rows = (leading + length + 6) / 7

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                Fmt.month(month),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    firstDayOfWeek.plus(i.toLong()).getDisplayName(JavaTextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayOfMonth = row * 7 + column - leading + 1
                    if (dayOfMonth in 1..length) {
                        val day = month.atDay(dayOfMonth).toEpochDay()
                        DayCell(
                            dayOfMonth = dayOfMonth,
                            total = totals[day],
                            target = target,
                            isToday = day == today,
                            isFuture = day > today,
                            onClick = { onSelectDay(day) },
                        )
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    dayOfMonth: Int,
    total: DayTotal?,
    target: Double,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val fraction = if (total != null && target > 0) (total.protein / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val fill = if (total != null) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f + 0.74f * fraction)
    } else {
        Color.Transparent
    }
    val textColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
        total != null && fraction > 0.55f -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(fill)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$dayOfMonth",
            style = MaterialTheme.typography.labelLarge.merge(NumberStyle),
            color = textColor,
        )
    }
}
