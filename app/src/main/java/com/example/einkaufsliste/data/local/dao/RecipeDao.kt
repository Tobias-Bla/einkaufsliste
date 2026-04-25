package com.example.einkaufsliste.data.local.dao

import androidx.room.*
import com.example.einkaufsliste.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredient(recipeIngredient: RecipeIngredient)

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): Flow<List<Ingredient>>

    @Query("SELECT * FROM shopping_list ORDER BY isChecked ASC, category COLLATE NOCASE ASC, createdAt ASC")
    fun getShoppingList(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItems(items: List<ShoppingListItem>)

    @Update
    suspend fun updateShoppingListItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteShoppingListItem(item: ShoppingListItem)
    
    @Query("SELECT ingredients.* FROM ingredients JOIN recipe_ingredients ON ingredients.id = recipe_ingredients.ingredientId WHERE recipe_ingredients.recipeId = :recipeId")
    fun getIngredientsForRecipe(recipeId: String): Flow<List<Ingredient>>

    @Query("SELECT * FROM shopping_list WHERE normalizedName = :normalizedName AND isChecked = 0 AND lower(unit) = lower(:unit) LIMIT 1")
    suspend fun findActiveShoppingItem(normalizedName: String, unit: String): ShoppingListItem?

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
