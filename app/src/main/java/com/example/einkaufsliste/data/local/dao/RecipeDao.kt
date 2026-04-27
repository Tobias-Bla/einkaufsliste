package com.example.einkaufsliste.data.local.dao

import androidx.room.*
import com.example.einkaufsliste.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    suspend fun getAllRecipesSnapshot(): List<Recipe>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<Recipe>)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient): Long

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredient(recipeIngredient: RecipeIngredient)

    @Query("SELECT * FROM ingredients ORDER BY name COLLATE NOCASE ASC")
    fun getAllIngredients(): Flow<List<Ingredient>>

    @Query("SELECT * FROM shopping_list ORDER BY category COLLATE NOCASE ASC, createdAt ASC")
    fun getShoppingList(): Flow<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list ORDER BY category COLLATE NOCASE ASC, createdAt ASC")
    suspend fun getShoppingListSnapshot(): List<ShoppingListItem>

    @Query("SELECT * FROM purchased_recipe_stats ORDER BY purchaseCount DESC, lastPurchasedAt DESC, recipeName COLLATE NOCASE ASC")
    fun getPurchasedRecipeStats(): Flow<List<PurchasedRecipeStat>>

    @Query("SELECT * FROM purchased_product_stats ORDER BY purchaseCount DESC, lastPurchasedAt DESC, productName COLLATE NOCASE ASC")
    fun getPurchasedProductStats(): Flow<List<PurchasedProductStat>>

    @Query("SELECT * FROM purchased_recipe_stats WHERE recipeId = :recipeId LIMIT 1")
    suspend fun getPurchasedRecipeStat(recipeId: String): PurchasedRecipeStat?

    @Query("SELECT * FROM purchased_product_stats WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getPurchasedProductStat(normalizedName: String): PurchasedProductStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItems(items: List<ShoppingListItem>)

    @Update
    suspend fun updateShoppingListItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteShoppingListItem(item: ShoppingListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasedRecipeStat(stat: PurchasedRecipeStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasedProductStat(stat: PurchasedProductStat)
    
    @Query("SELECT ingredients.* FROM ingredients JOIN recipe_ingredients ON ingredients.id = recipe_ingredients.ingredientId WHERE recipe_ingredients.recipeId = :recipeId")
    fun getIngredientsForRecipe(recipeId: String): Flow<List<Ingredient>>

    @Query("SELECT * FROM shopping_list WHERE normalizedName = :normalizedName AND isChecked = 0 AND lower(unit) = lower(:unit) LIMIT 1")
    suspend fun findActiveShoppingItem(normalizedName: String, unit: String): ShoppingListItem?

    @Query("SELECT * FROM shopping_list WHERE normalizedName = :normalizedName AND lower(unit) = lower(:unit)")
    suspend fun findShoppingItemsByNameAndUnit(normalizedName: String, unit: String): List<ShoppingListItem>

    @Query("DELETE FROM shopping_list")
    suspend fun clearShoppingList()

    @Transaction
    suspend fun replaceRecipes(recipes: List<Recipe>) {
        clearRecipes()
        insertRecipes(recipes)
    }

    @Transaction
    suspend fun replaceShoppingList(items: List<ShoppingListItem>) {
        clearShoppingList()
        insertShoppingListItems(items)
    }
}
