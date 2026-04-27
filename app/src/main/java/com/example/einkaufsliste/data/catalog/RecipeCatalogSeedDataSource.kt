package com.example.einkaufsliste.data.catalog

import android.content.Context
import com.example.einkaufsliste.data.model.Recipe

class RecipeCatalogSeedDataSource(
    context: Context,
    private val parser: RecipeCatalogCsvParser = RecipeCatalogCsvParser()
) {
    private val assetManager = context.assets

    @Volatile
    private var cachedRecipes: List<Recipe>? = null

    @Volatile
    private var cachedIngredientCatalog: List<IngredientCatalogEntry>? = null

    fun loadRecipes(): List<Recipe> {
        cachedRecipes?.let { return it }

        return synchronized(this) {
            cachedRecipes ?: parser.parse(
                recipesCsv = readAsset(RECIPE_ASSET_PATH),
                productsCsv = readAsset(PRODUCT_ASSET_PATH),
                recipeProductsCsv = readAsset(RECIPE_PRODUCT_ASSET_PATH)
            ).also { cachedRecipes = it }
        }
    }

    fun loadIngredientCatalog(): List<IngredientCatalogEntry> {
        cachedIngredientCatalog?.let { return it }

        return synchronized(this) {
            cachedIngredientCatalog ?: parser.parseIngredientCatalog(
                productsCsv = readAsset(PRODUCT_ASSET_PATH)
            ).also { cachedIngredientCatalog = it }
        }
    }

    private fun readAsset(path: String): String =
        assetManager.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private companion object {
        const val RECIPE_ASSET_PATH = "seed/recipes.csv"
        const val PRODUCT_ASSET_PATH = "seed/products.csv"
        const val RECIPE_PRODUCT_ASSET_PATH = "seed/recipe_products.csv"
    }
}
