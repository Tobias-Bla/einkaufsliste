package com.example.einkaufsliste.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var description: String = "",
    var imageUrl: String? = null,
    var servings: Int = 2,
    var createdAt: Long = System.currentTimeMillis(),
    var ingredients: List<IngredientItem> = emptyList()
)

data class IngredientItem(
    var name: String = "",
    var amount: String = "",
    var unit: String = "",
    var category: String = "Sonstiges"
)

data class ShoppingListContribution(
    var key: String = "",
    var label: String? = null,
    var amount: String = "",
    var type: String = TYPE_MANUAL
) {
    companion object {
        const val TYPE_MANUAL = "manual"
        const val TYPE_RECIPE = "recipe"
    }
}

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var category: String = "Sonstiges",
    var defaultAmount: String = "",
    var defaultUnit: String = ""
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["recipeId", "ingredientId"],
    indices = [Index("recipeId"), Index("ingredientId")],
    foreignKeys = [
        ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Ingredient::class, parentColumns = ["id"], childColumns = ["ingredientId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class RecipeIngredient(
    var recipeId: String = "",
    var ingredientId: String = "",
    var amount: String = "",
    var unit: String = ""
)

@Entity(tableName = "shopping_list")
data class ShoppingListItem(
    @PrimaryKey var id: String = "",
    var ingredientName: String = "",
    var normalizedName: String = "",
    var amount: String = "",
    var unit: String = "",
    var category: String = "Sonstiges",
    var isChecked: Boolean = false,
    var sourceRecipeName: String? = null,
    var contributions: List<ShoppingListContribution> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var checkedAt: Long? = null
)

@Entity(tableName = "purchased_recipe_stats")
data class PurchasedRecipeStat(
    @PrimaryKey var recipeId: String = "",
    var recipeName: String = "",
    var purchaseCount: Int = 0,
    var lastPurchasedAt: Long = 0L
)

@Entity(tableName = "purchased_product_stats")
data class PurchasedProductStat(
    @PrimaryKey var normalizedName: String = "",
    var productName: String = "",
    var category: String = "Sonstiges",
    var purchaseCount: Int = 0,
    var lastPurchasedAt: Long = 0L
)
