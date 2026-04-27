package com.example.einkaufsliste.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.einkaufsliste.data.catalog.IngredientCatalogEntry
import com.example.einkaufsliste.data.catalog.RecipeCatalogSeedDataSource
import com.example.einkaufsliste.data.local.HouseholdState
import com.example.einkaufsliste.data.model.Ingredient
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.model.ShoppingListContribution
import com.example.einkaufsliste.data.model.ShoppingListItem
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.data.repository.normalizedKey
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ShoppingViewModel(
    private val repository: RecipeRepository,
    recipeCatalogSeedDataSource: RecipeCatalogSeedDataSource
) : ViewModel() {

    private val seedIngredientCatalog = recipeCatalogSeedDataSource.loadIngredientCatalog()

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

    val customIngredients: StateFlow<List<Ingredient>> = repository.customIngredients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchasedRecipeStats: StateFlow<List<com.example.einkaufsliste.data.model.PurchasedRecipeStat>> =
        repository.purchasedRecipeStats
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchasedProductStats: StateFlow<List<com.example.einkaufsliste.data.model.PurchasedProductStat>> =
        repository.purchasedProductStats
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredientCatalog: StateFlow<List<IngredientCatalogEntry>> = repository.customIngredients
        .combine(kotlinx.coroutines.flow.flowOf(seedIngredientCatalog)) { customIngredients, seedCatalog ->
            mergeIngredientCatalog(seedCatalog, customIngredients)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), seedIngredientCatalog)

    val syncStatus = repository.syncStatus

    init {
        repository.startSync(viewModelScope)
    }

    fun saveRecipe(
        recipeId: String = "",
        name: String,
        description: String,
        imageUrl: String?,
        servings: Int,
        ingredients: List<IngredientItem>
    ) {
        viewModelScope.launch {
            repository.insertRecipe(
                Recipe(
                    id = recipeId,
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
                    sourceRecipeName = sourceRecipeName,
                    contributions = listOf(
                        ShoppingListContribution(
                            key = UUID.randomUUID().toString(),
                            label = null,
                            amount = amount.trim(),
                            type = ShoppingListContribution.TYPE_MANUAL
                        )
                    )
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
            if (ingredient.name.isBlank()) return@forEach

            viewModelScope.launch {
                repository.addToShoppingList(
                    ShoppingListItem(
                        ingredientName = ingredient.name.trim(),
                        normalizedName = ingredient.name.normalizedKey(),
                        amount = ingredient.amount.trim(),
                        unit = ingredient.unit.trim(),
                        category = ingredient.category.trim().ifBlank { "Sonstiges" },
                        sourceRecipeName = recipe.name,
                        contributions = listOf(
                            ShoppingListContribution(
                                key = recipe.id,
                                label = recipe.name,
                                amount = ingredient.amount.trim(),
                                type = ShoppingListContribution.TYPE_RECIPE
                            )
                        )
                    )
                )
            }
        }
    }

    fun addSavedRecipeToShoppingList(recipeId: String) {
        val recipe = allRecipes.value.firstOrNull { it.id == recipeId } ?: return
        addRecipeToShoppingList(recipe)
    }

    fun removeRecipeFromShoppingList(recipe: Recipe) {
        viewModelScope.launch {
            repository.removeRecipeFromShoppingList(recipe)
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

    fun saveIngredient(
        id: String = "",
        name: String,
        defaultAmount: String,
        defaultUnit: String,
        category: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.upsertIngredient(
                Ingredient(
                    id = id.ifBlank { UUID.randomUUID().toString() },
                    name = name,
                    category = category,
                    defaultAmount = defaultAmount,
                    defaultUnit = defaultUnit
                )
            )
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.deleteIngredient(ingredient)
        }
    }

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

    private fun mergeIngredientCatalog(
        seedCatalog: List<IngredientCatalogEntry>,
        customIngredients: List<Ingredient>
    ): List<IngredientCatalogEntry> {
        val customEntries = customIngredients.map { ingredient ->
            IngredientCatalogEntry(
                name = ingredient.name,
                defaultAmount = ingredient.defaultAmount,
                defaultUnit = ingredient.defaultUnit,
                category = ingredient.category,
                imageUrl = null
            )
        }

        return (customEntries + seedCatalog)
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.normalizedKey() }
            .sortedBy { it.name.lowercase() }
    }
}
