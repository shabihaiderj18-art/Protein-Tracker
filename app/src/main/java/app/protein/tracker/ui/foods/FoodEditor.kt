package app.protein.tracker.ui.foods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.protein.tracker.data.db.Food
import app.protein.tracker.domain.BaseUnit
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.Nutrition
import app.protein.tracker.ui.components.PanelTopBar
import app.protein.tracker.ui.components.rememberAutoFocus

/**
 * Add or edit a food. Every value can be changed, including the starter foods.
 * Used from the Foods tab and from the log panel ("Add food").
 */
@Composable
fun FoodEditor(
    initial: Food?,
    prefillName: String,
    onClose: () -> Unit,
    onSave: (Food) -> Unit,
    onDelete: (() -> Unit)?,
    saveLabel: String = "Save food",
) {
    var name by remember { mutableStateOf(initial?.name ?: prefillName) }
    var baseUnit by remember { mutableStateOf(initial?.baseUnit ?: BaseUnit.GRAM) }
    var kcal by remember { mutableStateOf(initial?.kcalPer100?.let { Fmt.plain(it) } ?: "") }
    var protein by remember { mutableStateOf(initial?.proteinPer100?.let { Fmt.plain(it) } ?: "") }
    var unitName by remember { mutableStateOf(initial?.unitName ?: "") }
    var unitSize by remember { mutableStateOf(initial?.unitSize?.let { Fmt.plain(it) } ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var favorite by remember { mutableStateOf(initial?.isFavorite ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = rememberAutoFocus()

    val kcalValue = Nutrition.parseAmount(kcal)
    val proteinValue = Nutrition.parseAmount(protein)
    val unitSizeValue = Nutrition.parseAmount(unitSize)
    val unitBlank = unitName.isBlank() && unitSize.isBlank()
    val unitOk = unitBlank || (unitName.isNotBlank() && unitSizeValue != null && unitSizeValue > 0.0)
    val valid = name.isNotBlank() && kcalValue != null && proteinValue != null && unitOk

    val save: () -> Unit = {
        if (name.isNotBlank() && kcalValue != null && proteinValue != null && unitOk) {
            val hasUnit = !unitBlank
            val base = initial ?: Food(name = "", kcalPer100 = 0.0, proteinPer100 = 0.0)
            onSave(
                base.copy(
                    name = name.trim(),
                    kcalPer100 = kcalValue,
                    proteinPer100 = proteinValue,
                    baseUnit = baseUnit,
                    unitName = if (hasUnit) unitName.trim() else null,
                    unitSize = if (hasUnit) unitSizeValue else null,
                    note = note.trim().ifBlank { null },
                    isFavorite = favorite,
                    lastInUnits = base.lastInUnits && hasUnit,
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PanelTopBar(
            title = if (initial == null) "New food" else "Edit food",
            navigationIcon = Icons.Rounded.Close,
            navigationLabel = "Close",
            onNavigate = onClose,
            actions = {
                if (onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete food")
                    }
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            Column {
                Text("Measured in", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    BaseUnit.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = baseUnit == unit,
                            onClick = { baseUnit = unit },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BaseUnit.entries.size),
                        ) { Text(unit.longName) }
                    }
                }
            }

            Column {
                Text("Per 100 ${baseUnit.symbol}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Protein") },
                        suffix = { Text("g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { kcal = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Calories") },
                        suffix = { Text("kcal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    )
                }
            }

            Column {
                Text("Serving unit (optional)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Lets you log in eggs, slices, scoops… instead of ${baseUnit.symbol}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Unit") },
                        placeholder = { Text("slice") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        value = unitSize,
                        onValueChange = { unitSize = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("1 unit =") },
                        suffix = { Text(baseUnit.symbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    )
                }
                if (!unitOk) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Give the unit a name and a size, or leave both empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Brand, or how it was cooked") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { favorite = !favorite }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Favourite", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Favourites appear first when you log food.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = favorite, onCheckedChange = { favorite = it })
            }
            Spacer(Modifier.height(4.dp))
        }

        Button(
            onClick = save,
            enabled = valid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp),
        ) {
            Text(saveLabel, style = MaterialTheme.typography.titleMedium)
        }
    }

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${initial.name}?") },
            text = { Text("Entries you already logged keep their values.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
