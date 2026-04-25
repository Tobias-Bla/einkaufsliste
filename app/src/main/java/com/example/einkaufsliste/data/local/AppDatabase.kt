package com.example.einkaufsliste.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.einkaufsliste.data.local.dao.RecipeDao
import com.example.einkaufsliste.data.model.*

@Database(
    entities = [Recipe::class, Ingredient::class, RecipeIngredient::class, ShoppingListItem::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
