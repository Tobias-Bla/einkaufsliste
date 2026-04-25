package com.example.einkaufsliste.data.local

import androidx.room.TypeConverter
import com.example.einkaufsliste.data.model.IngredientItem
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
}
