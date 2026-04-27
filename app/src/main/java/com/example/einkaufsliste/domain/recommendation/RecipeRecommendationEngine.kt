package com.example.einkaufsliste.domain.recommendation

import com.example.einkaufsliste.data.discovery.MarketOffer
import com.example.einkaufsliste.data.discovery.OfferDiscoveryFeed
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.repository.normalizedKey
import kotlin.math.roundToInt

enum class RecommendationSource {
    SAVED,
    AI
}

data class RecommendationIngredient(
    val name: String,
    val amount: String = "",
    val unit: String = "",
    val category: String = "Sonstiges",
    val isCore: Boolean = true
)

data class OfferMatch(
    val offerId: String,
    val storeName: String,
    val offerTitle: String,
    val ingredientName: String,
    val priceLabel: String
)

data class RecipeRecommendation(
    val id: String,
    val recipeId: String?,
    val name: String,
    val description: String,
    val source: RecommendationSource,
    val score: Int,
    val matchedIngredients: List<String>,
    val missingIngredients: List<RecommendationIngredient>,
    val offerMatches: List<OfferMatch>,
    val rationale: String,
    val imageUrl: String? = null
)

class RecipeRecommendationEngine {

    fun buildRecommendations(
        savedRecipes: List<Recipe>,
        feed: OfferDiscoveryFeed
    ): List<RecipeRecommendation> {
        val storeNames = feed.stores.associate { it.id to it.name }
        val offersByCanonical = feed.offers.flatMap { offer ->
            canonicalTagsForOffer(offer).map { canonicalTag -> canonicalTag to offer }
        }.groupBy({ it.first }, { it.second })

        val candidates = savedRecipes.map { it.toCandidate() }

        return candidates.mapNotNull { candidate ->
            val matches = candidate.ingredients.mapNotNull { ingredient ->
                val canonical = canonicalizeIngredient(ingredient.name)
                val matchingOffer = offersByCanonical[canonical]?.firstOrNull() ?: return@mapNotNull null

                OfferMatch(
                    offerId = matchingOffer.id,
                    storeName = storeNames.getValue(matchingOffer.storeId),
                    offerTitle = matchingOffer.title,
                    ingredientName = ingredient.name,
                    priceLabel = matchingOffer.priceLabel
                )
            }.distinctBy { it.offerId to it.ingredientName }

            val matchedNames = matches.map { it.ingredientName }.toSet()
            val missing = candidate.ingredients.filter { it.name !in matchedNames }

            val matchedCore = candidate.ingredients.count { it.isCore && it.name in matchedNames }
            val matchedOptional = candidate.ingredients.count { !it.isCore && it.name in matchedNames }
            val missingCore = candidate.ingredients.count { it.isCore && it.name !in matchedNames }

            val totalWeight = candidate.ingredients.sumOf { if (it.isCore) 1.0 else 0.6 }
            val matchedWeight = candidate.ingredients.sumOf { ingredient ->
                when {
                    ingredient.name !in matchedNames -> 0.0
                    ingredient.isCore -> 1.0
                    else -> 0.6
                }
            }

            val weightedCoverage = if (totalWeight == 0.0) 0.0 else matchedWeight / totalWeight
            val score = (
                weightedCoverage * 70 +
                    matchedCore * 12 +
                    matchedOptional * 6 -
                    missingCore.coerceAtMost(3) * 4
                ).roundToInt().coerceIn(0, 99)

            if (matches.size < 2 || score < 40) return@mapNotNull null

            RecipeRecommendation(
                id = candidate.id,
                recipeId = candidate.recipeId,
                name = candidate.name,
                description = candidate.description,
                source = candidate.source,
                score = score,
                matchedIngredients = matches.map { it.ingredientName }.distinct(),
                missingIngredients = missing,
                offerMatches = matches,
                rationale = buildRationale(candidate.name, matches),
                imageUrl = candidate.imageUrl
            )
        }.sortedWith(
            compareByDescending<RecipeRecommendation> { it.score }
                .thenBy { if (it.source == RecommendationSource.SAVED) 0 else 1 }
                .thenByDescending { it.offerMatches.size }
                .thenBy { it.name }
        )
    }

    private fun buildRationale(name: String, matches: List<OfferMatch>): String {
        val topMatches = matches.take(2).joinToString(" und ") {
            "${it.ingredientName} bei ${it.storeName}"
        }
        return "$name passt heute gut, weil $topMatches im Angebot sind."
    }

    private fun Recipe.toCandidate(): RecommendationCandidate =
        RecommendationCandidate(
            id = "saved-$id",
            recipeId = id,
            name = name,
            description = description.ifBlank { "Aus deinen gespeicherten Rezepten." },
            source = RecommendationSource.SAVED,
            imageUrl = imageUrl,
            ingredients = ingredients.map { it.toRecommendationIngredient() }
        )

    private fun IngredientItem.toRecommendationIngredient(): RecommendationIngredient =
        RecommendationIngredient(
            name = name,
            amount = amount,
            unit = unit,
            category = category,
            isCore = true
        )

    private fun canonicalTagsForOffer(offer: MarketOffer): Set<String> {
        val explicitTags = offer.tags.map(::canonicalizeIngredient)
        val implicitTags = detectCanonicalTerms("${offer.title} ${offer.subtitle}")
        return (explicitTags + implicitTags).toSet()
    }

    private fun canonicalizeIngredient(name: String): String {
        val normalized = normalizeText(name)
        val match = aliasGroups.entries.firstOrNull { (_, aliases) ->
            aliases.any { alias -> normalized.contains(alias) || alias.contains(normalized) }
        }
        return match?.key ?: normalized
    }

    private fun detectCanonicalTerms(text: String): Set<String> {
        val normalized = normalizeText(text)
        return aliasGroups.mapNotNull { (canonical, aliases) ->
            canonical.takeIf { aliases.any { alias -> normalized.contains(alias) } }
        }.toSet()
    }

    private fun normalizeText(text: String): String = text.normalizedKey()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private data class RecommendationCandidate(
        val id: String,
        val recipeId: String?,
        val name: String,
        val description: String,
        val source: RecommendationSource,
        val imageUrl: String?,
        val ingredients: List<RecommendationIngredient>
    )

    private companion object {
        val aliasGroups: Map<String, Set<String>> = mapOf(
            "passierte tomaten" to setOf("passierte tomaten", "tomaten", "tomatensauce"),
            "rinderhackfleisch" to setOf("rinderhack", "rinder hack", "rinderhackfleisch", "hackfleisch", "gehacktes"),
            "lasagneplatten" to setOf("lasagneplatten", "nudelplatten", "lasagne blatt"),
            "kaese" to setOf("kaese", "gouda", "mozzarella", "parmesan", "gerieben"),
            "spaghetti" to setOf("spaghetti", "nudeln", "pasta"),
            "kidneybohnen" to setOf("kidneybohnen", "kidney bohnen", "bohnen"),
            "paprika" to setOf("paprika"),
            "zucchini" to setOf("zucchini"),
            "basilikum" to setOf("basilikum"),
            "zwiebel" to setOf("zwiebel", "zwiebeln")
        )
    }
}
