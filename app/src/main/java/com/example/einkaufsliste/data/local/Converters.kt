package com.example.einkaufsliste.data.local

import androidx.room.TypeConverter
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.ShoppingListContribution
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun ingredientItemsToJson(ingredients: List<IngredientItem>): String {
        val array = JSONArray()
        ingredients.forEach { ingredient ->
            array.put(
                JSONObject()
                    .put("name", ingredient.name)
                    .put("amount", ingredient.amount)
                    .put("unit", ingredient.unit)
                    .put("category", ingredient.category)
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToIngredientItems(json: String?): List<IngredientItem> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                IngredientItem(
                    name = item.optString("name"),
                    amount = item.optString("amount"),
                    unit = item.optString("unit"),
                    category = item.optString("category", "Sonstiges")
                )
            }.filter { it.name.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun shoppingContributionsToJson(contributions: List<ShoppingListContribution>): String {
        val array = JSONArray()
        contributions.forEach { contribution ->
            array.put(
                JSONObject()
                    .put("key", contribution.key)
                    .put("label", contribution.label)
                    .put("amount", contribution.amount)
                    .put("type", contribution.type)
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun jsonToShoppingContributions(json: String?): List<ShoppingListContribution> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                ShoppingListContribution(
                    key = item.optString("key"),
                    label = item.optString("label").takeIf { it.isNotBlank() },
                    amount = item.optString("amount"),
                    type = item.optString("type", ShoppingListContribution.TYPE_MANUAL)
                )
            }.filter { it.key.isNotBlank() || it.amount.isNotBlank() || !it.label.isNullOrBlank() }
        }.getOrDefault(emptyList())
    }
}
