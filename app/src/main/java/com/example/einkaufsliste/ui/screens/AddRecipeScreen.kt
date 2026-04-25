package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    viewModel: ShoppingViewModel,
    onRecipeAdded: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("2") }
    val ingredients = remember { mutableStateListOf(IngredientDraft()) }
    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Rezept") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    TextButton(onClick = ::fillExample) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Beispiel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Portionen") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Bild-URL") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notizen") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zutaten", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { ingredients.add(IngredientDraft()) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Zeile")
                }
            }

            ingredients.forEachIndexed { index, ingredient ->
                IngredientEditorCard(
                    ingredient = ingredient,
                    canDelete = ingredients.size > 1,
                    onChange = { updated -> ingredients[index] = updated },
                    onDelete = { ingredients.removeAt(index) }
                )
            }

            Button(
                onClick = {
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

                    viewModel.addRecipe(
                        name = name,
                        description = description,
                        imageUrl = imageUrl.trim().ifBlank { null },
                        servings = servingsText.toIntOrNull()?.coerceAtLeast(1) ?: 2,
                        ingredients = cleanIngredients
                    )
                    onRecipeAdded()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rezept speichern")
            }
        }
    }
}

@Composable
private fun IngredientEditorCard(
    ingredient: IngredientDraft,
    canDelete: Boolean,
    onChange: (IngredientDraft) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = { onChange(ingredient.copy(name = it)) },
                    label = { Text("Zutat") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, enabled = canDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Zutat loeschen")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ingredient.amount,
                    onValueChange = { onChange(ingredient.copy(amount = it)) },
                    label = { Text("Menge") },
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
            OutlinedTextField(
                value = ingredient.category,
                onValueChange = { onChange(ingredient.copy(category = it)) },
                label = { Text("Kategorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class IngredientDraft(
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val category: String = "Sonstiges"
)
