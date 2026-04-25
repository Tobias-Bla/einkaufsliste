package com.example.einkaufsliste.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.einkaufsliste.data.local.HouseholdState
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.model.ShoppingListItem
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.data.repository.normalizedKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: RecipeRepository) : ViewModel() {

    val household: StateFlow<HouseholdState> = repository.household
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HouseholdState(code = "", name = "Gemeinsame Liste")
        )

    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingList: StateFlow<List<ShoppingListItem>> = repository.shoppingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncStatus = repository.syncStatus

    init {
        repository.startSync(viewModelScope)
    }

    fun addRecipe(
        name: String,
        description: String,
        imageUrl: String?,
        servings: Int,
        ingredients: List<IngredientItem>
    ) {
        viewModelScope.launch {
            repository.insertRecipe(
                Recipe(
                    name = name.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    servings = servings,
                    ingredients = ingredients.cleanIngredients()
                )
            )
        }
    }

    fun toggleShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.updateShoppingListItem(
                item.copy(
                    isChecked = !item.isChecked,
                    checkedAt = if (!item.isChecked) System.currentTimeMillis() else null
                )
            )
        }
    }

    fun addToShoppingList(
        name: String,
        amount: String = "",
        unit: String = "",
        category: String = "Sonstiges",
        sourceRecipeName: String? = null
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.addToShoppingList(
                ShoppingListItem(
                    ingredientName = name.trim(),
                    normalizedName = name.normalizedKey(),
                    amount = amount.trim(),
                    unit = unit.trim(),
                    category = category.trim().ifBlank { "Sonstiges" },
                    sourceRecipeName = sourceRecipeName
                )
            )
        }
    }

    fun addRecipeToShoppingList(recipe: Recipe) {
        val ingredients = recipe.ingredients.cleanIngredients()
        if (ingredients.isEmpty()) {
            addToShoppingList(recipe.name, "1", "x", "Rezepte", recipe.name)
            return
        }

        ingredients.forEach { ingredient ->
            addToShoppingList(
                name = ingredient.name,
                amount = ingredient.amount,
                unit = ingredient.unit,
                category = ingredient.category,
                sourceRecipeName = recipe.name
            )
        }
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.deleteShoppingListItem(item)
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    fun clearShoppingList() {
        viewModelScope.launch {
            repository.clearShoppingList()
        }
    }

    fun updateHouseholdName(name: String) {
        viewModelScope.launch {
            repository.updateHouseholdName(name)
        }
    }

    fun joinHousehold(code: String): Boolean = repository.joinHousehold(code)

    fun createNewHousehold() = repository.createNewHousehold()

    private fun List<IngredientItem>.cleanIngredients(): List<IngredientItem> =
        mapNotNull { ingredient ->
            val name = ingredient.name.trim()
            if (name.isBlank()) {
                null
            } else {
                ingredient.copy(
                    name = name,
                    amount = ingredient.amount.trim(),
                    unit = ingredient.unit.trim(),
                    category = ingredient.category.trim().ifBlank { "Sonstiges" }
                )
            }
        }
}
