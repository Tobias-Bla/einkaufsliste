package com.example.einkaufsliste.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.einkaufsliste.data.local.dao.RecipeDao
import com.example.einkaufsliste.data.model.*

@Database(
    entities = [
        Recipe::class,
        Ingredient::class,
        RecipeIngredient::class,
        ShoppingListItem::class,
        PurchasedRecipeStat::class,
        PurchasedProductStat::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE ingredients ADD COLUMN defaultAmount TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE ingredients ADD COLUMN defaultUnit TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE shopping_list ADD COLUMN contributions TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchased_recipe_stats` (
                        `recipeId` TEXT NOT NULL,
                        `recipeName` TEXT NOT NULL,
                        `purchaseCount` INTEGER NOT NULL,
                        `lastPurchasedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`recipeId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchased_product_stats` (
                        `normalizedName` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `purchaseCount` INTEGER NOT NULL,
                        `lastPurchasedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`normalizedName`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
