package com.example.einkaufsliste.domain.recommendation

import com.example.einkaufsliste.data.discovery.LocalDiscoveryRepository
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeRecommendationEngineTest {

    @Test
    fun `lasagne gets recommended when tomatoes and minced beef are on offer`() = runBlocking {
        val engine = RecipeRecommendationEngine()
        val feed = LocalDiscoveryRepository().discoveryFeed().first()
        val recipes = listOf(
            Recipe(
                id = "1",
                name = "Lasagne",
                description = "Familienklassiker",
                ingredients = listOf(
                    IngredientItem(name = "Passierte Tomaten"),
                    IngredientItem(name = "Rinderhackfleisch"),
                    IngredientItem(name = "Lasagneplatten"),
                    IngredientItem(name = "Kaese")
                )
            )
        )

        val recommendations = engine.buildRecommendations(recipes, feed)

        assertTrue(recommendations.isNotEmpty())
        assertEquals("Lasagne", recommendations.first().name)
        assertTrue(recommendations.first().matchedIngredients.contains("Passierte Tomaten"))
        assertTrue(recommendations.first().matchedIngredients.contains("Rinderhackfleisch"))
    }
}
