package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.einkaufsliste.data.model.ShoppingListItem
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
    onViewRecipes: () -> Unit,
    onOpenHousehold: () -> Unit
) {
    val household by viewModel.household.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sonstiges") }
    var showClearConfirmation by remember { mutableStateOf(false) }

    fun addItem() {
        if (name.isBlank()) return
        viewModel.addToShoppingList(
            name = name,
            amount = amount,
            unit = unit,
            category = category
        )
        name = ""
        amount = ""
        unit = ""
    }

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
                        onClick = { showClearConfirmation = true },
                        enabled = shoppingList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Liste leeren")
                    }
                }
            )
        }
    ) { padding ->
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text("Liste leeren?") },
                text = { Text("Alle Artikel werden aus dieser Einkaufsliste entfernt.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearShoppingList()
                            showClearConfirmation = false
                        }
                    ) {
                        Text("Leeren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmation = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val compact = maxWidth < 720.dp

            if (compact) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        AddItemPanel(
                            name = name,
                            amount = amount,
                            unit = unit,
                            category = category,
                            onNameChange = { name = it },
                            onAmountChange = { amount = it },
                            onUnitChange = { unit = it },
                            onCategoryChange = { category = it },
                            onAdd = ::addItem
                        )
                    }
                    shoppingListItems(
                        shoppingList = shoppingList,
                        viewModel = viewModel
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AddItemPanel(
                        name = name,
                        amount = amount,
                        unit = unit,
                        category = category,
                        onNameChange = { name = it },
                        onAmountChange = { amount = it },
                        onUnitChange = { unit = it },
                        onCategoryChange = { category = it },
                        onAdd = ::addItem,
                        modifier = Modifier.weight(0.9f)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1.4f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        shoppingListItems(
                            shoppingList = shoppingList,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shoppingListItems(
    shoppingList: List<ShoppingListItem>,
    viewModel: ShoppingViewModel
) {
    if (shoppingList.isEmpty()) {
        item {
            EmptyShoppingList()
        }
    } else {
        items(
            items = shoppingList,
            key = { it.id.ifBlank { "${it.ingredientName}-${it.createdAt}" } }
        ) { item ->
            ShoppingItemRow(
                item = item,
                onCheckedChange = { viewModel.toggleShoppingItem(item) },
                onDelete = { viewModel.deleteShoppingItem(item) }
            )
        }
    }
}

@Composable
private fun AddItemPanel(
    name: String,
    amount: String,
    unit: String,
    category: String,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Artikel hinzufuegen", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Artikel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Menge") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = onUnitChange,
                    label = { Text("Einheit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = category,
                onValueChange = onCategoryChange,
                label = { Text("Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onAdd,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hinzufuegen")
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onCheckedChange() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
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
                    Text(quantity, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(item.category.ifBlank { "Sonstiges" }) }
                    )
                    item.sourceRecipeName?.takeIf { it.isNotBlank() }?.let { source ->
                        Text(
                            text = source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
