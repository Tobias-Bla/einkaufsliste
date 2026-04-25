package com.example.einkaufsliste.data.remote

import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.model.ShoppingListItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    fun getRecipes(householdCode: String): Flow<List<Recipe>> {
        return recipesCollection(householdCode)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject(Recipe::class.java)?.copy(id = document.id)
                }
        }
    }

    fun getShoppingList(householdCode: String): Flow<List<ShoppingListItem>> {
        return shoppingCollection(householdCode)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject(ShoppingListItem::class.java)?.copy(id = document.id)
                }.sortedWith(
                    compareBy<ShoppingListItem> { it.isChecked }
                        .thenBy { it.category }
                        .thenBy { it.createdAt }
                )
        }
    }

    suspend fun addRecipe(householdCode: String, recipe: Recipe) {
        touchHousehold(householdCode)
        val recipesCollection = recipesCollection(householdCode)
        val docRef = if (recipe.id.isBlank()) recipesCollection.document() else recipesCollection.document(recipe.id)
        docRef.set(recipe.copy(id = docRef.id)).await()
    }

    suspend fun addToShoppingList(householdCode: String, item: ShoppingListItem) {
        touchHousehold(householdCode)
        val shoppingCollection = shoppingCollection(householdCode)
        val existingItem = shoppingCollection
            .whereEqualTo("normalizedName", item.normalizedName)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(ShoppingListItem::class.java)?.copy(id = document.id)
            }
            .firstOrNull { existing ->
                !existing.isChecked && existing.unit.equals(item.unit, ignoreCase = true)
            }

        if (existingItem != null) {
            shoppingCollection.document(existingItem.id)
                .update(
                    mapOf(
                        "amount" to mergeAmounts(existingItem.amount, item.amount),
                        "sourceRecipeName" to mergeSources(existingItem.sourceRecipeName, item.sourceRecipeName)
                    )
                )
                .await()
            return
        }

        val docRef = if (item.id.isBlank()) shoppingCollection.document() else shoppingCollection.document(item.id)
        val itemWithId = item.copy(id = docRef.id)
        docRef.set(itemWithId).await()
    }

    suspend fun toggleShoppingItem(householdCode: String, item: ShoppingListItem) {
        if (item.id.isBlank()) return

        val checked = item.isChecked
        shoppingCollection(householdCode).document(item.id)
            .update(
                mapOf(
                    "isChecked" to checked,
                    "checkedAt" to if (checked) System.currentTimeMillis() else null
                )
            )
            .await()
    }

    suspend fun updateShoppingItem(householdCode: String, item: ShoppingListItem) {
        if (item.id.isBlank()) return

        touchHousehold(householdCode)
        shoppingCollection(householdCode).document(item.id)
            .set(item)
            .await()
    }

    suspend fun deleteShoppingItem(householdCode: String, item: ShoppingListItem) {
        if (item.id.isBlank()) return

        shoppingCollection(householdCode).document(item.id).delete().await()
    }

    suspend fun deleteRecipe(householdCode: String, recipe: Recipe) {
        if (recipe.id.isBlank()) return

        recipesCollection(householdCode).document(recipe.id).delete().await()
    }

    suspend fun clearShoppingList(householdCode: String) {
        val query = shoppingCollection(householdCode).get().await()
        for (doc in query.documents) {
            doc.reference.delete().await()
        }
    }

    suspend fun updateHouseholdName(householdCode: String, name: String) {
        householdDocument(householdCode)
            .set(
                mapOf(
                    "name" to name,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun householdDocument(householdCode: String) =
        db.collection("households").document(householdCode)

    private fun recipesCollection(householdCode: String) =
        householdDocument(householdCode).collection("recipes")

    private fun shoppingCollection(householdCode: String) =
        householdDocument(householdCode).collection("shopping_list")

    private suspend fun touchHousehold(householdCode: String) {
        householdDocument(householdCode)
            .set(
                mapOf(
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun mergeAmounts(existing: String, added: String): String {
        if (existing.isBlank()) return added
        if (added.isBlank()) return existing
        if (existing == added) return existing
        return "$existing + $added"
    }

    private fun mergeSources(existing: String?, added: String?): String? {
        val sources = listOfNotNull(existing, added)
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return sources.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}
