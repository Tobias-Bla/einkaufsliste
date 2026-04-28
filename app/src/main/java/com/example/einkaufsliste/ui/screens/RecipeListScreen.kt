package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.repository.normalizedKey
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: ShoppingViewModel,
    onRecipeClick: (Recipe) -> Unit,
    onAddRecipeClick: () -> Unit,
    onViewShoppingList: () -> Unit,
    onManageProducts: () -> Unit
) {
    val recipes by viewModel.allRecipes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecipes = remember(recipes, searchQuery) {
        val query = searchQuery.normalizedKey()
        recipes.filter { recipe ->
            query.isBlank() ||
                recipe.name.normalizedKey().contains(query) ||
                recipe.description.normalizedKey().contains(query) ||
                recipe.ingredients.any { ingredient ->
                    ingredient.name.normalizedKey().contains(query) ||
                        ingredient.category.normalizedKey().contains(query)
                }
        }
    }
    val ingredientCount = filteredRecipes.sumOf { it.ingredients.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rezepte") },
                actions = {
                    IconButton(onClick = onManageProducts) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Produkte")
                    }
                    IconButton(onClick = onViewShoppingList) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Einkaufsliste")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipeClick) {
                Icon(Icons.Default.Add, contentDescription = "Rezept hinzufuegen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RecipeOverviewCard(
                    recipeCount = filteredRecipes.size,
                    ingredientCount = ingredientCount
                )
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rezepte oder Zutaten suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (recipes.isEmpty()) {
                item {
                    EmptyRecipeState(onAddRecipeClick = onAddRecipeClick)
                }
            } else if (filteredRecipes.isEmpty()) {
                item {
                    EmptySearchState()
                }
            } else {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe) },
                        onAddToShoppingList = { viewModel.addRecipeToShoppingList(recipe) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeOverviewCard(
    recipeCount: Int,
    ingredientCount: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecipeOverviewMetric(
                modifier = Modifier.weight(1f),
                label = "Rezepte",
                value = recipeCount.toString()
            )
            RecipeOverviewMetric(
                modifier = Modifier.weight(1f),
                label = "Zutaten",
                value = ingredientCount.toString()
            )
        }
    }
}

@Composable
private fun RecipeOverviewMetric(
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
fun RecipeRow(
    recipe: Recipe,
    onClick: () -> Unit,
    onAddToShoppingList: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                recipe.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = recipe.name, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${recipe.servings} Portionen") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("${recipe.ingredients.size} Zutaten") }
                        )
                    }
                    if (recipe.description.isNotBlank()) {
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onAddToShoppingList) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auf Liste")
                }
            }
        }
    }
}

@Composable
private fun EmptyRecipeState(onAddRecipeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Noch keine Rezepte gespeichert.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Lege Rezepte mit Zutaten an, damit Angebote automatisch dazu passen und du sie direkt auf die Einkaufsliste setzen kannst.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddRecipeClick) {
                Text("Rezept anlegen")
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Text(
            text = "Keine Rezepte passen zu deiner Suche.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
