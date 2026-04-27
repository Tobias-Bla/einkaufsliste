package com.example.einkaufsliste.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeCatalogCsvParserTest {

    private val parser = RecipeCatalogCsvParser()

    @Test
    fun `parse builds recipes from exported catalogs`() {
        val recipesCsv = """
            "RECIPE_ID","RECIPE_NAME","RECIPE_IMAGE_URL","CATEGORY_ID","GARNISH_ID","GUIDE","USERNAME","MODIFYING_USER","ITS","UTS"
            1,"Lasagne","https://example.com/lasagne.jpg",2,,"Schritt 1
            Schritt 2","Franziska","FRANZISKA",2021-03-07 00:00:00,2025-12-23 00:00:00
        """.trimIndent()
        val productsCsv = """
            "PRODUCT_ID","PRODUCT_NAME","UNIT_ID","QUANTITY","IMAGE_URL","ORDER_NUMBER","USERNAME","MODIFYING_USER","ITS","UTS","MARKET"
            1,"Hackfleisch","1",500,"",2,"","","","",
            2,"Passierte Tomaten","7",1,"",10,"","","","",
        """.trimIndent()
        val relationsCsv = """
            "RECIPE_ID","PRODUCT_ID","AMOUNT"
            1,1,1
            1,2,2
        """.trimIndent()

        val recipes = parser.parse(recipesCsv, productsCsv, relationsCsv)

        assertEquals(1, recipes.size)
        assertEquals("legacy-recipe-1", recipes.first().id)
        assertEquals("Lasagne", recipes.first().name)
        assertTrue(recipes.first().description.contains("Schritt 1\nSchritt 2"))
        assertEquals("500", recipes.first().ingredients.first().amount)
        assertEquals("g", recipes.first().ingredients.first().unit)
        assertEquals("Packung", recipes.first().ingredients[1].unit)
        assertEquals("2", recipes.first().ingredients[1].amount)
    }

    @Test
    fun `parseIngredientCatalog builds product suggestions with defaults`() {
        val productsCsv = """
            "PRODUCT_ID","PRODUCT_NAME","UNIT_ID","QUANTITY","IMAGE_URL","ORDER_NUMBER","USERNAME","MODIFYING_USER","ITS","UTS","MARKET"
            1,"Hackfleisch","1",500,"",2,"","","","",
            2,"Passierte Tomaten","7",1,"",10,"","","","",
        """.trimIndent()

        val catalog = parser.parseIngredientCatalog(productsCsv)

        assertEquals(2, catalog.size)
        assertEquals("Hackfleisch", catalog.first().name)
        assertEquals("500", catalog.first().defaultAmount)
        assertEquals("g", catalog.first().defaultUnit)
        assertEquals("Fleisch", catalog.first().category)
        assertEquals(null, catalog.first().imageUrl)
        assertEquals("Passierte Tomaten", catalog[1].name)
        assertEquals("Packung", catalog[1].defaultUnit)
    }
}
