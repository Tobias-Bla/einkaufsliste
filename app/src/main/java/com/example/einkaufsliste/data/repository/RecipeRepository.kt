package com.example.einkaufsliste.data.repository

import com.example.einkaufsliste.data.local.HouseholdState
import com.example.einkaufsliste.data.local.HouseholdStore
import com.example.einkaufsliste.data.local.dao.RecipeDao
import com.example.einkaufsliste.data.model.*
import com.example.einkaufsliste.data.remote.AuthService
import com.example.einkaufsliste.data.remote.FirestoreService
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
    private val householdStore: HouseholdStore
) {
    val household: Flow<HouseholdState> = householdStore.household
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val shoppingList: Flow<List<ShoppingListItem>> = recipeDao.getShoppingList()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    fun startSync(scope: CoroutineScope) {
        scope.launch {
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
                                recipeDao.replaceRecipes(recipes.map { it.cleaned() })
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
                amount = mergeAmounts(existing.amount, cleanedItem.amount),
                sourceRecipeName = mergeSources(existing.sourceRecipeName, cleanedItem.sourceRecipeName),
                category = cleanedItem.category.ifBlank { existing.category },
                createdAt = existing.createdAt
            )
        } else {
            cleanedItem.copy(id = cleanedItem.id.ifBlank { UUID.randomUUID().toString() })
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

    private fun markRemoteUnavailable() {
        authService.markUnavailable()
        _syncStatus.value = SyncStatus(
            isOnline = false,
            message = "Nicht synchronisiert"
        )
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

fun ShoppingListItem.cleaned(): ShoppingListItem {
    val name = ingredientName.trim()
    return copy(
        ingredientName = name,
        normalizedName = name.normalizedKey(),
        amount = amount.trim(),
        unit = unit.trim(),
        category = category.trim().ifBlank { "Sonstiges" },
        sourceRecipeName = sourceRecipeName?.trim()?.takeIf { it.isNotBlank() }
    )
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
