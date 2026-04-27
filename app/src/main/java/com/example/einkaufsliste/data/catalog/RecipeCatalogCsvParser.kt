package com.example.einkaufsliste.data.catalog

import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

class RecipeCatalogCsvParser {

    fun parse(
        recipesCsv: String,
        productsCsv: String,
        recipeProductsCsv: String
    ): List<Recipe> {
        val productsById = parseProducts(productsCsv).associateBy { it.id }
        val ingredientsByRecipeId = parseRecipeProducts(recipeProductsCsv).groupBy { it.recipeId }

        return parseRecipes(recipesCsv).map { recipeRow ->
            recipeRow.toRecipe(
                productsById = productsById,
                recipeProducts = ingredientsByRecipeId[recipeRow.id].orEmpty()
            )
        }.sortedByDescending { it.createdAt }
    }

    fun parseIngredientCatalog(productsCsv: String): List<IngredientCatalogEntry> =
        parseProducts(productsCsv)
            .map { product ->
                IngredientCatalogEntry(
                    name = product.name,
                    defaultAmount = normalizeQuantity(product.quantity),
                    defaultUnit = mapUnit(product.unitId),
                    category = mapCategory(product.categoryOrder),
                    imageUrl = product.imageUrl.ifBlank { null }
                )
            }
            .distinctBy { it.name.lowercase(Locale.ROOT) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

    private fun parseRecipes(csv: String): List<LegacyRecipeRow> =
        parseRecords(csv).mapNotNull { row ->
            val id = row["RECIPE_ID"].orEmpty().trim()
            val name = row["RECIPE_NAME"].orEmpty().trim()
            if (id.isBlank() || name.isBlank()) {
                null
            } else {
                LegacyRecipeRow(
                    id = id,
                    name = name,
                    imageUrl = row["RECIPE_IMAGE_URL"].orEmpty().trim(),
                    guide = row["GUIDE"].orEmpty().trim(),
                    createdAt = parseTimestamp(row["UTS"], row["ITS"])
                )
            }
        }

    private fun parseProducts(csv: String): List<LegacyProductRow> =
        parseRecords(csv).mapNotNull { row ->
            val id = row["PRODUCT_ID"].orEmpty().trim()
            val name = row["PRODUCT_NAME"].orEmpty().trim()
            if (id.isBlank() || name.isBlank()) {
                null
            } else {
                LegacyProductRow(
                    id = id,
                    name = name,
                    unitId = row["UNIT_ID"].orEmpty().trim(),
                    quantity = row["QUANTITY"].orEmpty().trim(),
                    categoryOrder = row["ORDER_NUMBER"].orEmpty().trim(),
                    imageUrl = row["IMAGE_URL"].orEmpty().trim()
                )
            }
        }

    private fun parseRecipeProducts(csv: String): List<LegacyRecipeProductRow> =
        parseRecords(csv).mapNotNull { row ->
            val recipeId = row["RECIPE_ID"].orEmpty().trim()
            val productId = row["PRODUCT_ID"].orEmpty().trim()
            if (recipeId.isBlank() || productId.isBlank()) {
                null
            } else {
                LegacyRecipeProductRow(
                    recipeId = recipeId,
                    productId = productId,
                    amount = row["AMOUNT"].orEmpty().trim()
                )
            }
        }

    private fun parseRecords(csv: String): List<Map<String, String>> {
        val rows = parseCsvRows(csv)
        if (rows.isEmpty()) return emptyList()

        val headers = rows.first().mapIndexed { index, value ->
            if (index == 0) value.removePrefix("\uFEFF").trim() else value.trim()
        }

        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                headers.mapIndexed { index, header ->
                    header to row.getOrElse(index) { "" }
                }.toMap()
            }
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < csv.length) {
            when (val char = csv[index]) {
                '"' -> {
                    if (inQuotes && index + 1 < csv.length && csv[index + 1] == '"') {
                        currentField.append('"')
                        index++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ',' -> {
                    if (inQuotes) {
                        currentField.append(char)
                    } else {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                }

                '\r' -> {
                    if (inQuotes) {
                        currentField.append(char)
                    } else {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                        if (index + 1 < csv.length && csv[index + 1] == '\n') {
                            index++
                        }
                    }
                }

                '\n' -> {
                    if (inQuotes) {
                        currentField.append(char)
                    } else {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                }

                else -> currentField.append(char)
            }
            index++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }

        return rows
    }

    private fun LegacyRecipeRow.toRecipe(
        productsById: Map<String, LegacyProductRow>,
        recipeProducts: List<LegacyRecipeProductRow>
    ): Recipe = Recipe(
        id = "legacy-recipe-$id",
        name = name,
        description = guide,
        imageUrl = imageUrl.ifBlank { null },
        servings = 2,
        createdAt = createdAt,
        ingredients = recipeProducts.mapNotNull { relation ->
            productsById[relation.productId]?.toIngredientItem(relation.amount)
        }
    )

    private fun LegacyProductRow.toIngredientItem(recipeAmount: String): IngredientItem =
        IngredientItem(
            name = name,
            amount = calculateAmount(recipeAmount, quantity),
            unit = mapUnit(unitId),
            category = mapCategory(categoryOrder)
        )

    private fun calculateAmount(multiplierRaw: String, quantityRaw: String): String {
        val multiplier = multiplierRaw.toBigDecimalOrNull() ?: return multiplierRaw
        val quantity = quantityRaw.toBigDecimalOrNull()
        return (quantity?.multiply(multiplier) ?: multiplier).toDisplayString()
    }

    private fun normalizeQuantity(quantityRaw: String): String =
        quantityRaw.toBigDecimalOrNull()?.toDisplayString() ?: quantityRaw

    private fun BigDecimal.toDisplayString(): String =
        stripTrailingZeros().toPlainString()

    private fun parseTimestamp(primary: String?, fallback: String?): Long {
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val value = listOf(primary, fallback)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?: return 0L

        return parser.parse(value)?.time ?: 0L
    }

    private fun mapUnit(unitId: String): String = when (unitId) {
        "1" -> "g"
        "2" -> "kg"
        "4", "42" -> "Stk"
        "5" -> "ml"
        "6" -> "l"
        "7", "11" -> "Packung"
        "8" -> "Flasche"
        "9" -> "Kasten"
        "10" -> "Glas"
        "12" -> "Flasche"
        "21" -> "Bund"
        "41" -> "Dose"
        else -> ""
    }

    private fun mapCategory(orderNumber: String): String = when (orderNumber) {
        "1" -> "Obst & Gemuese"
        "2", "6" -> "Fleisch"
        "3" -> "Kuehlregal"
        "4", "9" -> "Backen"
        "5" -> "Gewuerze"
        "7" -> "Baeckerei"
        "8" -> "Fruehstueck"
        "10" -> "Vorrat"
        "11" -> "Milchprodukte"
        "12" -> "Saucen"
        "13" -> "Oele & Konserven"
        "14" -> "Suesigkeiten"
        "15", "20" -> "Getraenke"
        "18" -> "Tiefkuehl"
        else -> "Sonstiges"
    }

    private data class LegacyRecipeRow(
        val id: String,
        val name: String,
        val imageUrl: String,
        val guide: String,
        val createdAt: Long
    )

    private data class LegacyProductRow(
        val id: String,
        val name: String,
        val unitId: String,
        val quantity: String,
        val categoryOrder: String,
        val imageUrl: String
    )

    private data class LegacyRecipeProductRow(
        val recipeId: String,
        val productId: String,
        val amount: String
    )

    private companion object {
        const val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss"
    }
}

data class IngredientCatalogEntry(
    val name: String,
    val defaultAmount: String = "",
    val defaultUnit: String = "",
    val category: String = "Sonstiges",
    val imageUrl: String? = null
)
