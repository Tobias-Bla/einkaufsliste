package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.catalog.IngredientCatalogEntry
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.model.ShoppingListContribution
import com.example.einkaufsliste.data.model.ShoppingListItem
import com.example.einkaufsliste.data.repository.normalizedKey
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
    onViewRecipes: () -> Unit,
    onOpenHousehold: () -> Unit,
    onManageProducts: () -> Unit
) {
    val household by viewModel.household.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val allRecipes by viewModel.allRecipes.collectAsState()
    val ingredientCatalog by viewModel.ingredientCatalog.collectAsState()
    val catalogByName = ingredientCatalog.associateBy { it.name.normalizedKey() }
    val showClearConfirmation = remember { mutableStateOf(false) }
    val recipesOnList = rememberRecipesOnShoppingList(shoppingList, allRecipes)
    val pendingItems = shoppingList
        .filterNot { it.isChecked }
        .sortedWith(compareBy<ShoppingListItem> { it.category }.thenBy { it.ingredientName.lowercase() })
    val checkedItems = shoppingList
        .filter { it.isChecked }
        .sortedWith(compareByDescending<ShoppingListItem> { it.checkedAt ?: 0L }.thenBy { it.ingredientName.lowercase() })
    val groupedPending = pendingItems.groupBy { it.category.ifBlank { "Sonstiges" } }
    val completedCount = checkedItems.size
    val totalCount = shoppingList.size
    val remainingCount = totalCount - completedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(household.name)
                        syncStatus.message?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHousehold) {
                        Icon(Icons.Default.Settings, contentDescription = "Haushalt")
                    }
                    IconButton(onClick = onViewRecipes) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Rezepte")
                    }
                    IconButton(
                        onClick = { showClearConfirmation.value = true },
                        enabled = shoppingList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Liste leeren")
                    }
                }
            )
        }
    ) { padding ->
        if (showClearConfirmation.value) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation.value = false },
                title = { Text("Liste leeren?") },
                text = { Text("Alle Artikel werden aus dieser Einkaufsliste entfernt.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearShoppingList()
                            showClearConfirmation.value = false
                        }
                    ) {
                        Text("Leeren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmation.value = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                ShoppingProgressCard(
                    remainingCount = remainingCount,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    recipeCount = recipesOnList.size
                )
            }
            item {
                QuickAddCard(
                    ingredientCatalog = ingredientCatalog,
                    onAddItem = { name, amount, unit, category ->
                        viewModel.addToShoppingList(
                            name = name,
                            amount = amount,
                            unit = unit,
                            category = category
                        )
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onViewRecipes,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rezept hinzufuegen")
                    }
                    Button(
                        onClick = onManageProducts,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Produkt hinzufuegen")
                    }
                }
            }
            if (recipesOnList.isNotEmpty()) {
                item {
                    ShoppingRecipeSection(
                        recipes = recipesOnList,
                        onRemoveRecipe = viewModel::removeRecipeFromShoppingList
                    )
                }
            }
            shoppingListSections(
                groupedPending = groupedPending,
                checkedItems = checkedItems,
                catalogByName = catalogByName,
                onCheckedChange = viewModel::toggleShoppingItem,
                onDelete = viewModel::deleteShoppingItem
            )
        }
    }
}

@Composable
private fun ShoppingProgressCard(
    remainingCount: Int,
    completedCount: Int,
    totalCount: Int,
    recipeCount: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Einkauf heute", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShoppingMetric(
                    modifier = Modifier.weight(1f),
                    label = "Offen",
                    value = remainingCount.toString()
                )
                ShoppingMetric(
                    modifier = Modifier.weight(1f),
                    label = "Erledigt",
                    value = completedCount.toString()
                )
                ShoppingMetric(
                    modifier = Modifier.weight(1f),
                    label = "Rezepte",
                    value = recipeCount.toString()
                )
            }
            Text(
                text = if (totalCount == 0) {
                    "Die Liste ist leer."
                } else {
                    "$completedCount von $totalCount Artikeln sind bereits abgehakt."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShoppingMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAddCard(
    ingredientCatalog: List<IngredientCatalogEntry>,
    onAddItem: (name: String, amount: String, unit: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sonstiges") }

    fun applyCatalogDefaults(productName: String) {
        val entry = ingredientCatalog.firstOrNull { it.name.equals(productName, ignoreCase = true) } ?: return
        if (amount.isBlank()) amount = entry.defaultAmount
        if (unit.isBlank()) unit = entry.defaultUnit
        if (category.isBlank() || category == "Sonstiges") category = entry.category
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Schnell hinzufuegen", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    applyCatalogDefaults(it)
                },
                label = { Text("Artikel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Menge") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Einheit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onAddItem(name.trim(), amount.trim(), unit.trim(), category.trim().ifBlank { "Sonstiges" })
                        name = ""
                        amount = ""
                        unit = ""
                        category = "Sonstiges"
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Zur Liste")
                }
            }
        }
    }
}

@Composable
private fun rememberRecipesOnShoppingList(
    shoppingList: List<ShoppingListItem>,
    allRecipes: List<Recipe>
): List<Recipe> {
    val recipesById = allRecipes.associateBy { it.id }
    val recipeIdsOnList = shoppingList
        .flatMap { it.contributions }
        .filter { it.type == ShoppingListContribution.TYPE_RECIPE }
        .map { it.key }
        .distinct()

    return recipeIdsOnList.mapNotNull { recipesById[it] }
}

@Composable
private fun ShoppingRecipeSection(
    recipes: List<Recipe>,
    onRemoveRecipe: (Recipe) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rezepte auf der Liste", style = MaterialTheme.typography.titleMedium)
            recipes.forEach { recipe ->
                RecipeOnShoppingListRow(
                    recipe = recipe,
                    onRemove = { onRemoveRecipe(recipe) }
                )
            }
        }
    }
}

@Composable
private fun RecipeOnShoppingListRow(
    recipe: Recipe,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        recipe.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(recipe.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${recipe.ingredients.size} Zutaten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Rezept entfernen")
        }
    }
}

private fun LazyListScope.shoppingListSections(
    groupedPending: Map<String, List<ShoppingListItem>>,
    checkedItems: List<ShoppingListItem>,
    catalogByName: Map<String, IngredientCatalogEntry>,
    onCheckedChange: (ShoppingListItem) -> Unit,
    onDelete: (ShoppingListItem) -> Unit
) {
    if (groupedPending.isEmpty() && checkedItems.isEmpty()) {
        item {
            EmptyShoppingList()
        }
        return
    }

    groupedPending
        .toSortedMap(compareBy { it.lowercase() })
        .forEach { (category, itemsInCategory) ->
            item(key = "category-$category") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
            items(
                items = itemsInCategory,
                key = { it.id.ifBlank { "${it.ingredientName}-${it.createdAt}" } }
            ) { item ->
                ShoppingItemRow(
                    item = item,
                    imageUrl = catalogByName[item.normalizedName]?.imageUrl,
                    onCheckedChange = { onCheckedChange(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }

    if (checkedItems.isNotEmpty()) {
        item(key = "checked-heading") {
            Text(
                text = "Erledigt",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        items(
            items = checkedItems,
            key = { it.id.ifBlank { "${it.ingredientName}-${it.createdAt}" } }
        ) { item ->
            ShoppingItemRow(
                item = item,
                imageUrl = catalogByName[item.normalizedName]?.imageUrl,
                onCheckedChange = { onCheckedChange(item) },
                onDelete = { onDelete(item) }
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    imageUrl: String?,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (item.isChecked) 0.72f else 1f },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onCheckedChange() }
            )
            imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.ingredientName,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                )
                val quantity = listOf(item.amount, item.unit)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (quantity.isNotBlank()) {
                    Text(
                        quantity,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isChecked) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Erledigt") }
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(item.category.ifBlank { "Sonstiges" }) }
                    )
                }
                item.sourceRecipeName?.takeIf { it.isNotBlank() }?.let { source ->
                    Text(
                        text = source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Artikel loeschen")
            }
        }
    }
}

@Composable
private fun EmptyShoppingList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Noch keine Artikel auf der Liste.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
