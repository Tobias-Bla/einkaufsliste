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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.catalog.IngredientCatalogEntry
import com.example.einkaufsliste.data.model.Ingredient
import com.example.einkaufsliste.data.repository.normalizedKey
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit
) {
    val customIngredients by viewModel.customIngredients.collectAsState()
    val ingredientCatalog by viewModel.ingredientCatalog.collectAsState()
    var draft by remember { mutableStateOf(ProductDraft()) }
    var search by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<Ingredient?>(null) }

    val filteredCatalog = remember(search, ingredientCatalog) {
        val query = search.normalizedKey()
        ingredientCatalog.filter { entry ->
            query.isBlank() ||
                entry.name.normalizedKey().contains(query) ||
                entry.category.normalizedKey().contains(query)
        }
    }

    fun resetDraft() {
        draft = ProductDraft()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produktkatalog") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::resetDraft) {
                Icon(Icons.Default.Add, contentDescription = "Neues Produkt")
            }
        }
    ) { padding ->
        deleteCandidate?.let { ingredient ->
            AlertDialog(
                onDismissRequest = { deleteCandidate = null },
                title = { Text("Produkt loeschen?") },
                text = {
                    Text("Das Produkt wird aus deiner Produktverwaltung entfernt. Bereits gespeicherte Rezepte bleiben unveraendert.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteIngredient(ingredient)
                            if (draft.id == ingredient.id) {
                                resetDraft()
                            }
                            deleteCandidate = null
                        }
                    ) {
                        Text("Loeschen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteCandidate = null }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ProductEditorCard(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSave = {
                        viewModel.saveIngredient(
                            id = draft.id,
                            name = draft.name,
                            defaultAmount = draft.amount,
                            defaultUnit = draft.unit,
                            category = draft.category
                        )
                        resetDraft()
                    },
                    onCancelEdit = ::resetDraft
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Produkte", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Produkte koennen direkt von hier auf die Einkaufsliste gesetzt werden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Produkte suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredCatalog.isEmpty()) {
                item {
                    EmptyProductCatalog()
                }
            } else {
                items(filteredCatalog, key = { it.name }) { ingredient ->
                    val customIngredient = customIngredients.firstOrNull {
                        it.name.normalizedKey() == ingredient.name.normalizedKey()
                    }
                    ProductCatalogRow(
                        ingredient = ingredient,
                        isCustom = customIngredient != null,
                        onAddToShoppingList = {
                            viewModel.addToShoppingList(
                                name = ingredient.name,
                                amount = ingredient.defaultAmount,
                                unit = ingredient.defaultUnit,
                                category = ingredient.category
                            )
                        },
                        onEdit = customIngredient?.let {
                            {
                                draft = ProductDraft(
                                    id = it.id,
                                    name = it.name,
                                    amount = it.defaultAmount,
                                    unit = it.defaultUnit,
                                    category = it.category
                                )
                            }
                        },
                        onDelete = customIngredient?.let { { deleteCandidate = it } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductEditorCard(
    draft: ProductDraft,
    onDraftChange: (ProductDraft) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (draft.id.isBlank()) "Eigenes Produkt anlegen" else "Eigenes Produkt bearbeiten",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = { onDraftChange(draft.copy(amount = it)) },
                    label = { Text("Standardmenge") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.unit,
                    onValueChange = { onDraftChange(draft.copy(unit = it)) },
                    label = { Text("Standardeinheit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = draft.category,
                onValueChange = { onDraftChange(draft.copy(category = it)) },
                label = { Text("Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = draft.name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (draft.id.isBlank()) "Speichern" else "Aktualisieren")
                }
                if (draft.id.isNotBlank()) {
                    TextButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCatalogRow(
    ingredient: IngredientCatalogEntry,
    isCustom: Boolean,
    onAddToShoppingList: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ingredient.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ingredient.name, style = MaterialTheme.typography.titleMedium)
                    if (isCustom) {
                        Text(
                            text = "Eigen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                val defaults = listOf(ingredient.defaultAmount, ingredient.defaultUnit)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (defaults.isNotBlank()) {
                    Text(defaults, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    ingredient.category.ifBlank { "Sonstiges" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onAddToShoppingList) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zur Liste")
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Produkt bearbeiten")
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Produkt loeschen")
                }
            }
        }
    }
}

@Composable
private fun EmptyProductCatalog() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Keine Produkte fuer diese Suche gefunden.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ProductDraft(
    val id: String = "",
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val category: String = "Sonstiges"
)
