package com.example.einkaufsliste.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.einkaufsliste.data.discovery.LocalDiscoveryRepository
import com.example.einkaufsliste.data.discovery.NearbyStore
import com.example.einkaufsliste.data.discovery.OfferDiscoveryFeed
import com.example.einkaufsliste.data.remote.AiRecipeSuggestionService
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendation
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendationEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val feed: OfferDiscoveryFeed? = null,
    val highlightedStores: List<NearbyStore> = emptyList(),
    val recommendations: List<RecipeRecommendation> = emptyList(),
    val dailyTip: RecipeRecommendation? = null,
    val dailyTipRecipeId: String? = null,
    val dailyTipImageUrl: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModel(
    recipeRepository: RecipeRepository,
    discoveryRepository: LocalDiscoveryRepository,
    recommendationEngine: RecipeRecommendationEngine,
    aiRecipeSuggestionService: AiRecipeSuggestionService
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        recipeRepository.allRecipes,
        discoveryRepository.discoveryFeed()
    ) { recipes, feed ->
        recipes to feed
    }.mapLatest { (recipes, feed) ->
        val fallbackRecommendations = recommendationEngine.buildRecommendations(recipes, feed)
        val recommendations = aiRecipeSuggestionService.suggestRecipes(recipes, feed)
            ?: fallbackRecommendations
        val dailyTip = recommendations.firstOrNull { it.recipeId != null } ?: recommendations.firstOrNull()
        val dailyTipRecipe = recipes.firstOrNull { it.id == dailyTip?.recipeId }
        TodayUiState(
            feed = feed,
            highlightedStores = feed.stores.sortedBy { it.distanceKm },
            recommendations = recommendations,
            dailyTip = dailyTip,
            dailyTipRecipeId = dailyTipRecipe?.id,
            dailyTipImageUrl = dailyTip?.imageUrl ?: dailyTipRecipe?.imageUrl
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayUiState()
    )
}
