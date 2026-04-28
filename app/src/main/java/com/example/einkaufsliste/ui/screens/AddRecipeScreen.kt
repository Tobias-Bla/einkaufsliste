package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.catalog.IngredientCatalogEntry
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    viewModel: ShoppingViewModel,
    initialRecipe: Recipe?,
    onRecipeAdded: () -> Unit,
    onBack: () -> Unit,
    onManageProducts: () -> Unit
) {
    var name by remember(initialRecipe?.id) { mutableStateOf(initialRecipe?.name.orEmpty()) }
    var description by remember(initialRecipe?.id) { mutableStateOf(initialRecipe?.description.orEmpty()) }
    var imageUrl by remember(initialRecipe?.id) { mutableStateOf(initialRecipe?.imageUrl.orEmpty()) }
    var servingsText by remember(initialRecipe?.id) { mutableStateOf(initialRecipe?.servings?.toString() ?: "2") }
    val ingredients = remember { mutableStateListOf(IngredientDraft()) }
    val ingredientCatalog by viewModel.ingredientCatalog.collectAsState()
    val title = if (initialRecipe == null) "Neues Rezept" else "Rezept bearbeiten"
    val hasNamedIngredient = ingredients.any { it.name.isNotBlank() }

    fun fillExample() {
        name = "Spaghetti Bolognese"
        description = "Hackfleischsauce mit Tomaten. Gut fuer zwei Tage."
        imageUrl = ""
        servingsText = "4"
        ingredients.clear()
        ingredients.addAll(
            listOf(
                IngredientDraft("Spaghetti", "500", "g", "Nudeln"),
                IngredientDraft("Hackfleisch", "400", "g", "Fleisch"),
                IngredientDraft("Passierte Tomaten", "700", "ml", "Konserven"),
                IngredientDraft("Zwiebel", "1", "Stk", "Gemuese"),
                IngredientDraft("Parmesan", "80", "g", "Kuehlregal")
            )
        )
    }

    LaunchedEffect(initialRecipe?.id) {
        ingredients.clear()
        if (initialRecipe == null) {
            ingredients.add(IngredientDraft())
        } else {
            ingredients.addAll(
                initialRecipe.ingredients.map { ingredient ->
                    IngredientDraft(
                        name = ingredient.name,
                        amount = ingredient.amount,
                        unit = ingredient.unit,
                        category = ingredient.category
                    )
                }
            )
            if (ingredients.isEmpty()) {
                ingredients.add(IngredientDraft())
            }
        }
    }

    fun saveRecipe() {
        val cleanIngredients = ingredients.mapNotNull { draft ->
            val itemName = draft.name.trim()
            if (itemName.isBlank()) {
                null
            } else {
                IngredientItem(
                    name = itemName,
                    amount = draft.amount.trim(),
                    unit = draft.unit.trim(),
                    category = draft.category.trim().ifBlank { "Sonstiges" }
                )
            }
        }

        viewModel.saveRecipe(
            recipeId = initialRecipe?.id.orEmpty(),
            name = name,
            description = description,
            imageUrl = imageUrl.trim().ifBlank { null },
            servings = servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 2,
            ingredients = cleanIngredients
        )
        onRecipeAdded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    IconButton(onClick = onManageProducts) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Produkte")
                    }
                    if (initialRecipe == null) {
                        TextButton(onClick = ::fillExample) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Beispiel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val compact = maxWidth < 980.dp

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecipeDetailsCard(
                        name = name,
                        description = description,
                        imageUrl = imageUrl,
                        servingsText = servingsText,
                        onNameChange = { name = it },
                        onDescriptionChange = { description = it },
                        onImageUrlChange = { imageUrl = it },
                        onServingsChange = { servingsText = it.filter(Char::isDigit).take(2) },
                        onSave = ::saveRecipe,
                        canSave = name.isNotBlank() && hasNamedIngredient
                    )
                    IngredientsEditorPanel(
                        ingredients = ingredients,
                        ingredientCatalog = ingredientCatalog
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RecipeDetailsCard(
                            name = name,
                            description = description,
                            imageUrl = imageUrl,
                            servingsText = servingsText,
                            onNameChange = { name = it },
                            onDescriptionChange = { description = it },
                            onImageUrlChange = { imageUrl = it },
                            onServingsChange = { servingsText = it.filter(Char::isDigit).take(2) },
                            onSave = ::saveRecipe,
                            canSave = name.isNotBlank() && hasNamedIngredient
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IngredientsEditorPanel(
                            ingredients = ingredients,
                            ingredientCatalog = ingredientCatalog
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailsCard(
    name: String,
    description: String,
    imageUrl: String,
    servingsText: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rezept", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Rezept Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = onImageUrlChange,
                    label = { Text("Rezept Bild URL") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = onServingsChange,
                    label = { Text("Portionen") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Anleitung") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!canSave) {
                    Text(
                        text = "Mindestens ein Rezeptname und eine Zutat sind erforderlich.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    onClick = onSave,
                    enabled = canSave
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientsEditorPanel(
    ingredients: MutableList<IngredientDraft>,
    ingredientCatalog: List<IngredientCatalogEntry>
) {
    var draft by remember { mutableStateOf(IngredientDraft()) }
    var expanded by remember { mutableStateOf(false) }

    val suggestions = remember(draft.name, ingredientCatalog) {
        ingredientCatalog
            .filter { entry ->
                draft.name.isBlank() || entry.name.contains(draft.name, ignoreCase = true)
            }
            .take(10)
    }

    fun addIngredient() {
        val itemName = draft.name.trim()
        if (itemName.isBlank()) return
        ingredients.add(
            draft.copy(
                name = itemName,
                amount = draft.amount.trim(),
                unit = draft.unit.trim(),
                category = draft.category.trim().ifBlank { "Sonstiges" }
            )
        )
        draft = IngredientDraft()
        expanded = false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Zutaten", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            if (ingredients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Noch keine Zutaten hinzugefuegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ingredients.forEachIndexed { index, ingredient ->
                        IngredientListRow(
                            ingredient = ingredient,
                            onChange = { updated -> ingredients[index] = updated },
                            onDelete = { ingredients.removeAt(index) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Zutat hinzufuegen",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = expanded && suggestions.isNotEmpty(),
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = {
                        draft = draft.copy(name = it)
                        expanded = true
                    },
                    label = { Text("Produkt") },
                    placeholder = { Text("Produkt waehlen oder suchen") },
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && suggestions.isNotEmpty())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                            enabled = true
                        )
                )
                ExposedDropdownMenu(
                    expanded = expanded && suggestions.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                val defaults = listOf(
                                    suggestion.defaultAmount,
                                    suggestion.defaultUnit
                                ).filter { it.isNotBlank() }.joinToString(" ")
                                Text(
                                    if (defaults.isBlank()) suggestion.name
                                    else "${suggestion.name} ($defaults)"
                                )
                            },
                            onClick = {
                                draft = draft.applyCatalogEntry(suggestion)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = { draft = draft.copy(amount = it) },
                    label = { Text("Anzahl") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.unit,
                    onValueChange = { draft = draft.copy(unit = it) },
                    label = { Text("Einheit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = draft.category,
                onValueChange = { draft = draft.copy(category = it) },
                label = { Text("Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = ::addIngredient,
                enabled = draft.name.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hinzufuegen")
            }
        }
    }
}

@Composable
private fun IngredientListRow(
    ingredient: IngredientDraft,
    onChange: (IngredientDraft) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!ingredient.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ingredient.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(ingredient.name, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Zutat entfernen")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = { onChange(ingredient.copy(name = it)) },
                    label = { Text("Produkt") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = ingredient.category,
                    onValueChange = { onChange(ingredient.copy(category = it)) },
                    label = { Text("Kategorie") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ingredient.amount,
                    onValueChange = { onChange(ingredient.copy(amount = it)) },
                    label = { Text("Anzahl") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ingredient.unit,
                    onValueChange = { onChange(ingredient.copy(unit = it)) },
                    label = { Text("Einheit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class IngredientDraft(
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val category: String = "Sonstiges",
    val imageUrl: String? = null
)

private fun IngredientDraft.applyCatalogEntry(entry: IngredientCatalogEntry): IngredientDraft =
    copy(
        name = entry.name,
        amount = amount.ifBlank { entry.defaultAmount },
        unit = unit.ifBlank { entry.defaultUnit },
        category = if (category.isBlank() || category == "Sonstiges") entry.category else category,
        imageUrl = entry.imageUrl
    )
