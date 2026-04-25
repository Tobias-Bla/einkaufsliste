package com.example.einkaufsliste.domain.recommendation

import com.example.einkaufsliste.data.discovery.MarketOffer
import com.example.einkaufsliste.data.discovery.OfferDiscoveryFeed
import com.example.einkaufsliste.data.model.IngredientItem
import com.example.einkaufsliste.data.model.Recipe
import com.example.einkaufsliste.data.repository.normalizedKey
import kotlin.math.roundToInt

enum class RecommendationSource {
    SAVED,
    SUGGESTED
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
    val rationale: String
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

        val knownRecipeNames = savedRecipes.map { it.name.normalizedKey() }.toSet()
        val candidates = savedRecipes.map { it.toCandidate() } +
            suggestionTemplates().filterNot { it.name.normalizedKey() in knownRecipeNames }

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
                rationale = buildRationale(candidate.name, matches)
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

    private fun suggestionTemplates(): List<RecommendationCandidate> = listOf(
        RecommendationCandidate(
            id = "suggested-lasagne",
            recipeId = null,
            name = "Lasagne",
            description = "Klassiker fuer die Woche, wenn Tomaten und Hackfleisch guenstig sind.",
            source = RecommendationSource.SUGGESTED,
            ingredients = listOf(
                RecommendationIngredient("Passierte Tomaten", "2", "Packungen", "Konserven"),
                RecommendationIngredient("Rinderhackfleisch", "500", "g", "Fleisch"),
                RecommendationIngredient("Lasagneplatten", "1", "Packung", "Vorrat"),
                RecommendationIngredient("Kaese", "200", "g", "Milchprodukte"),
                RecommendationIngredient("Zwiebel", "1", "Stk.", "Gemuese", isCore = false),
                RecommendationIngredient("Basilikum", "1", "Bund", "Gewuerze", isCore = false)
            )
        ),
        RecommendationCandidate(
            id = "suggested-tomaten-hack-pasta",
            recipeId = null,
            name = "Tomaten-Hack-Pasta",
            description = "Schnelles Feierabendgericht mit starkem Angebotsfit.",
            source = RecommendationSource.SUGGESTED,
            ingredients = listOf(
                RecommendationIngredient("Rinderhackfleisch", "400", "g", "Fleisch"),
                RecommendationIngredient("Passierte Tomaten", "1", "Packung", "Konserven"),
                RecommendationIngredient("Spaghetti", "500", "g", "Vorrat"),
                RecommendationIngredient("Kaese", "80", "g", "Milchprodukte", isCore = false),
                RecommendationIngredient("Zwiebel", "1", "Stk.", "Gemuese", isCore = false)
            )
        ),
        RecommendationCandidate(
            id = "suggested-chili",
            recipeId = null,
            name = "Chili con Carne",
            description = "Lohnt sich besonders, wenn Bohnen, Paprika und Hack im Angebot sind.",
            source = RecommendationSource.SUGGESTED,
            ingredients = listOf(
                RecommendationIngredient("Rinderhackfleisch", "500", "g", "Fleisch"),
                RecommendationIngredient("Passierte Tomaten", "1", "Packung", "Konserven"),
                RecommendationIngredient("Kidneybohnen", "1", "Dose", "Konserven"),
                RecommendationIngredient("Paprika", "2", "Stk.", "Gemuese"),
                RecommendationIngredient("Zwiebel", "1", "Stk.", "Gemuese", isCore = false)
            )
        ),
        RecommendationCandidate(
            id = "suggested-zucchini-auflauf",
            recipeId = null,
            name = "Zucchini-Mozzarella-Auflauf",
            description = "Gemuesefokussierter Vorschlag aus den Frischeangeboten.",
            source = RecommendationSource.SUGGESTED,
            ingredients = listOf(
                RecommendationIngredient("Zucchini", "3", "Stk.", "Gemuese"),
                RecommendationIngredient("Mozzarella", "2", "Kugeln", "Milchprodukte"),
                RecommendationIngredient("Passierte Tomaten", "1", "Packung", "Konserven"),
                RecommendationIngredient("Basilikum", "1", "Bund", "Gewuerze", isCore = false)
            )
        )
    )

    private data class RecommendationCandidate(
        val id: String,
        val recipeId: String?,
        val name: String,
        val description: String,
        val source: RecommendationSource,
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
