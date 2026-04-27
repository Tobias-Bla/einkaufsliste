package com.example.einkaufsliste.data.repository

import com.example.einkaufsliste.data.catalog.RecipeCatalogSeedDataSource
import com.example.einkaufsliste.data.local.HouseholdState
import com.example.einkaufsliste.data.local.HouseholdStore
import com.example.einkaufsliste.data.local.dao.RecipeDao
import com.example.einkaufsliste.data.model.*
import com.example.einkaufsliste.data.remote.AuthService
import com.example.einkaufsliste.data.remote.FirestoreService
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val authService: AuthService,
    private val firestoreService: FirestoreService,
    private val householdStore: HouseholdStore,
    private val recipeCatalogSeedDataSource: RecipeCatalogSeedDataSource
) {
    val household: Flow<HouseholdState> = householdStore.household
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val shoppingList: Flow<List<ShoppingListItem>> = recipeDao.getShoppingList()
    val customIngredients: Flow<List<Ingredient>> = recipeDao.getAllIngredients()
    val purchasedRecipeStats: Flow<List<PurchasedRecipeStat>> = recipeDao.getPurchasedRecipeStats()
    val purchasedProductStats: Flow<List<PurchasedProductStat>> = recipeDao.getPurchasedProductStats()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    fun startSync(scope: CoroutineScope) {
        scope.launch {
            ensureLocalCatalogSeeded()
            if (!authService.isReady.value) {
                signInBestEffort()
            }
        }

        scope.launch {
            combine(authService.isReady, householdStore.household) { ready, household ->
                ready to household
            }.collectLatest { (ready, household) ->
                if (!ready) {
                    _syncStatus.value = SyncStatus(
                        isOnline = false,
                        message = "Lokaler Modus"
                    )
                    return@collectLatest
                }

                coroutineScope {
                    launch {
                        firestoreService.getRecipes(household.code)
                            .catch { markRemoteUnavailable() }
                            .collect { recipes ->
                                mergeRecipesFromRemote(household.code, recipes)
                                _syncStatus.value = SyncStatus(isOnline = true)
                            }
                    }
                    launch {
                        firestoreService.getShoppingList(household.code)
                            .catch { markRemoteUnavailable() }
                            .collect { items ->
                                recipeDao.replaceShoppingList(items.map { it.cleaned() })
                                _syncStatus.value = SyncStatus(isOnline = true)
                            }
                    }
                }
            }
        }
    }

    suspend fun insertRecipe(recipe: Recipe) {
        val savedRecipe = recipe.cleaned().copy(id = recipe.id.ifBlank { UUID.randomUUID().toString() })
        recipeDao.insertRecipe(savedRecipe)
        syncRecipe(savedRecipe)
    }
    
    suspend fun addToShoppingList(item: ShoppingListItem) {
        val cleanedItem = item.cleaned()
        val existing = recipeDao.findActiveShoppingItem(cleanedItem.normalizedName, cleanedItem.unit)
        val savedItem = if (existing != null) {
            existing.copy(
                category = cleanedItem.category.ifBlank { existing.category },
                createdAt = existing.createdAt,
                contributions = mergeContributions(existing.contributions, cleanedItem.contributions)
            ).recomputedFromContributions(fallbackAmount = mergeAmounts(existing.amount, cleanedItem.amount))
        } else {
            cleanedItem.copy(id = cleanedItem.id.ifBlank { UUID.randomUUID().toString() })
                .recomputedFromContributions(fallbackAmount = cleanedItem.amount)
        }

        recipeDao.insertShoppingListItem(savedItem)
        syncShoppingItem(savedItem)
    }

    suspend fun updateShoppingListItem(item: ShoppingListItem) {
        val savedItem = item.cleaned().copy(
            checkedAt = if (item.isChecked) {
                item.checkedAt ?: System.currentTimeMillis()
            } else {
                null
            }
        )
        recipeDao.updateShoppingListItem(savedItem)
        syncShoppingItem(savedItem)
    }

    suspend fun clearShoppingList() {
        val currentItems = recipeDao.getShoppingListSnapshot().map { it.cleaned() }
        if (currentItems.isNotEmpty()) {
            recordCompletedPurchase(currentItems)
        }
        recipeDao.clearShoppingList()
        if (signInBestEffort()) {
            runCatching {
                firestoreService.clearShoppingList(householdCode)
            }.onFailure {
                markRemoteUnavailable()
            }
        }
    }
    
    suspend fun deleteShoppingListItem(item: ShoppingListItem) {
        recipeDao.deleteShoppingListItem(item)
        if (signInBestEffort()) {
            runCatching {
                firestoreService.deleteShoppingItem(householdCode, item)
            }.onFailure {
                markRemoteUnavailable()
            }
        }
    }

    suspend fun removeRecipeFromShoppingList(recipe: Recipe) {
        val cleanedRecipe = recipe.cleaned()
        cleanedRecipe.ingredients.forEach { ingredient ->
            val matches = recipeDao.findShoppingItemsByNameAndUnit(
                normalizedName = ingredient.name.normalizedKey(),
                unit = ingredient.unit
            )

            matches.forEach { item ->
                val remainingContributions = item.contributions.filterNot { contribution ->
                    contribution.type == ShoppingListContribution.TYPE_RECIPE &&
                        contribution.key == cleanedRecipe.id
                }

                val updatedItem = item.copy(
                    contributions = remainingContributions
                ).recomputedFromContributions(fallbackAmount = item.amount)

                if (updatedItem.contributions.isEmpty() && updatedItem.sourceRecipeName == null) {
                    recipeDao.deleteShoppingListItem(item)
                    if (signInBestEffort()) {
                        runCatching {
                            firestoreService.deleteShoppingItem(householdCode, item)
                        }.onFailure {
                            markRemoteUnavailable()
                        }
                    }
                } else {
                    recipeDao.updateShoppingListItem(updatedItem)
                    syncShoppingItem(updatedItem)
                }
            }
        }
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
        if (signInBestEffort()) {
            runCatching {
                firestoreService.deleteRecipe(householdCode, recipe)
            }.onFailure {
                markRemoteUnavailable()
            }
        }
    }

    suspend fun updateHouseholdName(name: String) {
        householdStore.updateName(name)
        if (signInBestEffort()) {
            runCatching {
                firestoreService.updateHouseholdName(householdCode, householdStore.current.name)
            }.onFailure {
                markRemoteUnavailable()
            }
        }
    }

    suspend fun upsertIngredient(ingredient: Ingredient) {
        val savedIngredient = ingredient.cleaned().copy(
            id = ingredient.id.ifBlank { UUID.randomUUID().toString() }
        )
        recipeDao.insertIngredient(savedIngredient)
    }

    suspend fun deleteIngredient(ingredient: Ingredient) {
        recipeDao.deleteIngredient(ingredient)
    }

    fun joinHousehold(code: String): Boolean = householdStore.joinHousehold(code)

    fun createNewHousehold() = householdStore.createNewHousehold()

    private val householdCode: String
        get() = householdStore.current.code

    private suspend fun syncRecipe(recipe: Recipe) {
        if (!signInBestEffort()) return

        runCatching {
            firestoreService.addRecipe(householdCode, recipe)
            _syncStatus.value = SyncStatus(isOnline = true)
        }.onFailure {
            markRemoteUnavailable()
        }
    }

    private suspend fun syncShoppingItem(item: ShoppingListItem) {
        if (!signInBestEffort()) return

        runCatching {
            firestoreService.updateShoppingItem(householdCode, item)
            _syncStatus.value = SyncStatus(isOnline = true)
        }.onFailure {
            markRemoteUnavailable()
        }
    }

    private suspend fun signInBestEffort(): Boolean =
        authService.ensureSignedIn().also { signedIn ->
            if (!signedIn) markRemoteUnavailable()
        }

    private suspend fun ensureLocalCatalogSeeded() {
        if (recipeDao.getRecipeCount() > 0) return

        val seedRecipes = recipeCatalogSeedDataSource.loadRecipes()
        if (seedRecipes.isNotEmpty()) {
            recipeDao.replaceRecipes(seedRecipes)
        }
    }

    private suspend fun mergeRecipesFromRemote(householdCode: String, remoteRecipes: List<Recipe>) {
        val cleanedRemoteRecipes = remoteRecipes.map { it.cleaned() }
        if (cleanedRemoteRecipes.isNotEmpty()) {
            recipeDao.replaceRecipes(cleanedRemoteRecipes)
            return
        }

        val localRecipes = recipeDao.getAllRecipesSnapshot().map { it.cleaned() }
        if (localRecipes.isEmpty()) {
            ensureLocalCatalogSeeded()
        }

        val recipesToUpload = recipeDao.getAllRecipesSnapshot().map { it.cleaned() }
        recipesToUpload.forEach { recipe ->
            firestoreService.addRecipe(householdCode, recipe)
        }
    }

    private fun markRemoteUnavailable() {
        authService.markUnavailable()
        _syncStatus.value = SyncStatus(
            isOnline = false,
            message = "Nicht synchronisiert"
        )
    }

    private suspend fun recordCompletedPurchase(items: List<ShoppingListItem>) {
        val purchasedAt = System.currentTimeMillis()
        val recipesById = recipeDao.getAllRecipesSnapshot()
            .associateBy { it.id }

        items.asSequence()
            .flatMap { item ->
                item.contributions.asSequence()
                    .filter { contribution ->
                        contribution.type == ShoppingListContribution.TYPE_RECIPE &&
                            contribution.key.isNotBlank()
                    }
                    .map { contribution ->
                        contribution.key to (
                            recipesById[contribution.key]?.name
                                ?: contribution.label
                                ?: "Rezept"
                            )
                    }
            }
            .distinctBy { it.first }
            .forEach { (recipeId, recipeName) ->
                val existing = recipeDao.getPurchasedRecipeStat(recipeId)
                recipeDao.insertPurchasedRecipeStat(
                    PurchasedRecipeStat(
                        recipeId = recipeId,
                        recipeName = recipeName,
                        purchaseCount = (existing?.purchaseCount ?: 0) + 1,
                        lastPurchasedAt = purchasedAt
                    )
                )
            }

        items.groupBy { item ->
            item.normalizedName.ifBlank { item.ingredientName.normalizedKey() }
        }.forEach { (normalizedName, groupedItems) ->
            if (normalizedName.isBlank()) return@forEach

            val representative = groupedItems.first()
            val existing = recipeDao.getPurchasedProductStat(normalizedName)
            recipeDao.insertPurchasedProductStat(
                PurchasedProductStat(
                    normalizedName = normalizedName,
                    productName = representative.ingredientName,
                    category = representative.category,
                    purchaseCount = (existing?.purchaseCount ?: 0) + groupedItems.size,
                    lastPurchasedAt = purchasedAt
                )
            )
        }
    }
}

fun String.normalizedKey(): String = trim()
    .lowercase()
    .replace(Regex("\\s+"), " ")

data class SyncStatus(
    val isOnline: Boolean = false,
    val message: String? = null
)

fun Recipe.cleaned(): Recipe =
    copy(
        name = name.trim(),
        description = description.trim(),
        imageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() },
        servings = servings.coerceAtLeast(1),
        ingredients = ingredients.mapNotNull { ingredient ->
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
    )

fun Ingredient.cleaned(): Ingredient =
    copy(
        name = name.trim(),
        category = category.trim().ifBlank { "Sonstiges" },
        defaultAmount = defaultAmount.trim(),
        defaultUnit = defaultUnit.trim()
    )

fun ShoppingListItem.cleaned(): ShoppingListItem {
    val name = ingredientName.trim()
    val cleanedContributions = contributions.mapNotNull { contribution ->
        val key = contribution.key.trim()
        val amount = contribution.amount.trim()
        val label = contribution.label?.trim()?.takeIf { it.isNotBlank() }
        if (key.isBlank() && amount.isBlank() && label == null) {
            null
        } else {
            contribution.copy(
                key = key,
                label = label,
                amount = amount,
                type = contribution.type.trim().ifBlank { ShoppingListContribution.TYPE_MANUAL }
            )
        }
    }

    return copy(
        ingredientName = name,
        normalizedName = name.normalizedKey(),
        amount = amount.trim(),
        unit = unit.trim(),
        category = category.trim().ifBlank { "Sonstiges" },
        sourceRecipeName = sourceRecipeName?.trim()?.takeIf { it.isNotBlank() },
        contributions = cleanedContributions
    )
}

private fun mergeContributions(
    existing: List<ShoppingListContribution>,
    added: List<ShoppingListContribution>
): List<ShoppingListContribution> {
    if (added.isEmpty()) return existing
    if (existing.isEmpty()) return added

    val merged = existing.toMutableList()
    added.forEach { incoming ->
        val index = merged.indexOfFirst { it.key == incoming.key && it.type == incoming.type }
        if (index >= 0 && incoming.type == ShoppingListContribution.TYPE_RECIPE) {
            merged[index] = incoming
        } else {
            merged.add(incoming)
        }
    }
    return merged
}

private fun ShoppingListItem.recomputedFromContributions(fallbackAmount: String): ShoppingListItem {
    val contributionAmount = contributions.displayAmount()
    val recipeNames = contributions
        .filter { it.type == ShoppingListContribution.TYPE_RECIPE }
        .mapNotNull { it.label?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
        .joinToString(", ")
        .takeIf { it.isNotBlank() }

    return copy(
        amount = contributionAmount.ifBlank { fallbackAmount.trim() },
        sourceRecipeName = recipeNames
    )
}

private fun List<ShoppingListContribution>.displayAmount(): String {
    val relevantAmounts = map { it.amount.trim() }.filter { it.isNotBlank() }
    if (relevantAmounts.isEmpty()) return ""

    val numericAmounts = relevantAmounts.map { it.toBigDecimalOrNull() }
    return if (numericAmounts.all { it != null }) {
        numericAmounts.filterNotNull()
            .fold(BigDecimal.ZERO) { sum, value -> sum + value }
            .stripTrailingZeros()
            .toPlainString()
    } else {
        relevantAmounts.joinToString(" + ")
    }
}

fun mergeAmounts(existing: String, added: String): String {
    if (existing.isBlank()) return added
    if (added.isBlank()) return existing
    if (existing == added) return existing
    return "$existing + $added"
}

fun mergeSources(existing: String?, added: String?): String? {
    val sources = listOfNotNull(existing, added)
        .flatMap { it.split(",") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    return sources.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
