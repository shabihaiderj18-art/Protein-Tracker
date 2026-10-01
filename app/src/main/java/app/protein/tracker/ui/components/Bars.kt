package app.protein.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.hasUnit
import app.protein.tracker.domain.Fmt

/** Top bar for full-screen panels (log food, edit food). */
@Composable
fun PanelTopBar(
    title: String,
    navigationIcon: ImageVector,
    navigationLabel: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigate) {
            Icon(navigationIcon, contentDescription = navigationLabel)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/** "25 g protein · 165 kcal per 100 g · 1 egg = 50 g" */
fun foodSummary(food: Food): String {
    val base = "${Fmt.protein(food.proteinPer100)} g protein · ${Fmt.kcal(food.kcalPer100)} kcal per 100 ${food.baseUnit.symbol}"
    val unitName = food.unitName
    val unitSize = food.unitSize
    return if (food.hasUnit && unitName != null && unitSize != null) {
        "$base · ${Fmt.unitDefinition(unitName, unitSize, food.baseUnit)}"
    } else {
        base
    }
}
