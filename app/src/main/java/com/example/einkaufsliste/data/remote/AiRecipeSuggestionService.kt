package com.example.einkaufsliste.data.remote

import com.example.einkaufsliste.data.discovery.OfferDiscoveryFeed
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.repository.normalizedKey
import com.example.einkaufsliste.domain.recommendation.OfferMatch
import com.example.einkaufsliste.domain.recommendation.RecommendationIngredient
import com.example.einkaufsliste.domain.recommendation.RecommendationSource
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendation
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AiRecipeSuggestionService(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    suspend fun suggestRecipes(
        savedRecipes: List<Recipe>,
        feed: OfferDiscoveryFeed
    ): List<RecipeRecommendation>? {
        if (feed.offers.isEmpty()) return null

        return runCatching {
            val storesById = feed.stores.associateBy { it.id }
            val payload = mapOf(
                "profile" to mapOf(
                    "city" to feed.profile.city,
                    "radiusKm" to feed.profile.radiusKm,
                    "preferredChains" to feed.profile.preferredChains
                ),
                "offers" to feed.offers.map { offer ->
                    val store = storesById[offer.storeId]
                    mapOf(
                        "id" to offer.id,
                        "storeId" to offer.storeId,
                        "storeName" to (store?.name ?: ""),
                        "chain" to (store?.chain ?: ""),
                        "distanceKm" to (store?.distanceKm ?: 0.0),
                        "title" to offer.title,
                        "subtitle" to offer.subtitle,
                        "priceLabel" to offer.priceLabel,
                        "validityLabel" to offer.validityLabel,
                        "tags" to offer.tags
                    )
                },
                "savedRecipes" to savedRecipes.map { recipe ->
                    mapOf(
                        "id" to recipe.id,
                        "name" to recipe.name,
                        "description" to recipe.description,
                        "imageUrl" to (recipe.imageUrl ?: ""),
                        "servings" to recipe.servings,
                        "ingredients" to recipe.ingredients.map { ingredient ->
                            mapOf(
                                "name" to ingredient.name,
                                "amount" to ingredient.amount,
                                "unit" to ingredient.unit,
                                "category" to ingredient.category
                            )
                        }
                    )
                }
            )

            val result = functions
                .getHttpsCallable("generateRecipeRecommendations")
                .call(payload)
                .await()

            parseRecommendations(result.getData(), savedRecipes)
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun parseRecommendations(
        payload: Any?,
        savedRecipes: List<Recipe>
    ): List<RecipeRecommendation> {
        val root = payload as? Map<*, *> ?: return emptyList()
        val recipesById = savedRecipes.associateBy { it.id }
        val items = root["recommendations"] as? List<*> ?: return emptyList()

        return items.mapNotNull { entry ->
            val item = entry as? Map<*, *> ?: return@mapNotNull null
            val recipeId = item.string("recipeId")
            val linkedRecipe = recipeId?.let(recipesById::get)
            val name = item.string("name") ?: return@mapNotNull null
            val stableId = item.string("id")
                ?: "ai-${name.normalizedKey().replace(" ", "-")}"

            RecipeRecommendation(
                id = stableId,
                recipeId = recipeId,
                name = name,
                description = item.string("description").orEmpty(),
                source = RecommendationSource.AI,
                score = item.int("score")?.coerceIn(0, 99) ?: 50,
                matchedIngredients = item.stringList("matchedIngredients"),
                missingIngredients = item.objectList("missingIngredients").mapNotNull { missing ->
                    val ingredientName = missing.string("name") ?: return@mapNotNull null
                    RecommendationIngredient(
                        name = ingredientName,
                        amount = missing.string("amount").orEmpty(),
                        unit = missing.string("unit").orEmpty(),
                        category = missing.string("category") ?: "Sonstiges",
                        isCore = missing.boolean("isCore") ?: true
                    )
                },
                offerMatches = item.objectList("offerMatches").mapNotNull { offer ->
                    val offerId = offer.string("offerId") ?: return@mapNotNull null
                    val ingredientName = offer.string("ingredientName") ?: return@mapNotNull null
                    OfferMatch(
                        offerId = offerId,
                        storeName = offer.string("storeName").orEmpty(),
                        offerTitle = offer.string("offerTitle").orEmpty(),
                        ingredientName = ingredientName,
                        priceLabel = offer.string("priceLabel").orEmpty()
                    )
                },
                rationale = item.string("rationale").orEmpty(),
                imageUrl = item.string("imageUrl") ?: linkedRecipe?.imageUrl
            )
        }
    }

    private fun Map<*, *>.string(key: String): String? =
        (this[key] as? String)?.trim()?.takeIf { it.isNotBlank() }

    private fun Map<*, *>.int(key: String): Int? =
        when (val value = this[key]) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            else -> null
        }

    private fun Map<*, *>.boolean(key: String): Boolean? = this[key] as? Boolean

    private fun Map<*, *>.stringList(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotBlank) }
            ?: emptyList()

    private fun Map<*, *>.objectList(key: String): List<Map<*, *>> =
        (this[key] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
}
