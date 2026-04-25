package com.example.einkaufsliste.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.einkaufsliste.data.discovery.LocalDiscoveryRepository
import com.example.einkaufsliste.data.discovery.NearbyStore
import com.example.einkaufsliste.data.discovery.OfferDiscoveryFeed
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendation
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendationEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val feed: OfferDiscoveryFeed? = null,
    val highlightedStores: List<NearbyStore> = emptyList(),
    val recommendations: List<RecipeRecommendation> = emptyList()
)

class TodayViewModel(
    recipeRepository: RecipeRepository,
    discoveryRepository: LocalDiscoveryRepository,
    recommendationEngine: RecipeRecommendationEngine
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        recipeRepository.allRecipes,
        discoveryRepository.discoveryFeed()
    ) { recipes, feed ->
        TodayUiState(
            feed = feed,
            highlightedStores = feed.stores.sortedBy { it.distanceKm },
            recommendations = recommendationEngine.buildRecommendations(recipes, feed)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayUiState()
    )
}
