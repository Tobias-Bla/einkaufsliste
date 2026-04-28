package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.discovery.MarketOffer
import com.example.einkaufsliste.data.discovery.NearbyStore
import com.example.einkaufsliste.domain.recommendation.RecommendationIngredient
import com.example.einkaufsliste.domain.recommendation.RecommendationSource
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendation
import com.example.einkaufsliste.ui.viewmodel.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onViewRecipes: () -> Unit,
    onViewShoppingList: () -> Unit,
    onOpenHousehold: () -> Unit,
    onAddMissingIngredients: (List<RecommendationIngredient>) -> Unit,
    onOpenRecipe: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val feed = uiState.feed
    val featuredOffers = feed?.stores
        ?.sortedBy { it.distanceKm }
        ?.flatMap { store -> store.offers.take(2).map { offer -> store to offer } }
        ?.take(5)
        .orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Heute")
                        Text(
                            text = feed?.let { "${it.profile.city} - ${it.updatedAtLabel}" }
                                ?: "Angebote in der Naehe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHousehold) {
                        Icon(Icons.Default.Settings, contentDescription = "Haushalt")
                    }
                    IconButton(onClick = onViewRecipes) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Rezepte")
                    }
                    IconButton(onClick = onViewShoppingList) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Einkaufsliste")
                    }
                }
            )
        }
    ) { padding ->
        if (feed == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text("Angebote werden geladen.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                DailyTipCard(
                    recommendation = uiState.dailyTip,
                    imageUrl = uiState.dailyTipImageUrl,
                    onViewRecipes = onViewRecipes,
                    onAddMissingIngredients = {
                        uiState.dailyTip?.let { tip ->
                            onAddMissingIngredients(tip.missingIngredients)
                        }
                    },
                    onOpenRecipe = onOpenRecipe
                )
            }
            item {
                if (featuredOffers.isNotEmpty()) {
                    SectionTitle(
                        title = "Schnellueberblick",
                        subtitle = "Die naechsten interessanten Angebote fuer deinen Einkauf"
                    )
                }
            }
            if (featuredOffers.isNotEmpty()) {
                items(featuredOffers, key = { (_, offer) -> offer.id }) { (store, offer) ->
                    OfferHighlightCard(store = store, offer = offer)
                }
            }
            item {
                SectionTitle(
                    title = "Angebote in deiner Naehe",
                    subtitle = "Kuratiert fuer ${feed.profile.city} im ${feed.profile.radiusKm}-km-Radius"
                )
            }
            items(uiState.highlightedStores, key = { it.id }) { store ->
                StoreCard(store = store)
            }
            item {
                SectionTitle(
                    title = "Passt zu deinen Rezepten",
                    subtitle = "Bestehende Rezepte plus neue Vorschlaege auf Basis der Angebote"
                )
            }
            if (uiState.recommendations.isEmpty()) {
                item {
                    EmptyRecommendationsCard(onViewRecipes = onViewRecipes)
                }
            } else {
                items(uiState.recommendations, key = { it.id }) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        onAddMissingIngredients = {
                            onAddMissingIngredients(recommendation.missingIngredients)
                        },
                        onOpenRecipe = onOpenRecipe
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTipCard(
    recommendation: RecipeRecommendation?,
    imageUrl: String?,
    onViewRecipes: () -> Unit,
    onAddMissingIngredients: () -> Unit,
    onOpenRecipe: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tipp des Tages", style = MaterialTheme.typography.titleLarge)
            if (recommendation == null) {
                Text(
                    text = "Speichere ein paar Rezepte, dann gibt es hier jeden Tag einen passenden Vorschlag.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onViewRecipes) {
                    Text("Rezepte ansehen")
                }
            } else {
                imageUrl?.takeIf { it.isNotBlank() }?.let { recipeImage ->
                    AsyncImage(
                        model = recipeImage,
                        contentDescription = recommendation.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = recommendation.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = recommendation.rationale,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Treffer: ${recommendation.matchedIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddMissingIngredients,
                        enabled = recommendation.missingIngredients.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fehlendes auf Liste")
                    }
                    OutlinedButton(
                        onClick = {
                            recommendation.recipeId?.let(onOpenRecipe) ?: onViewRecipes()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (recommendation.recipeId != null) {
                                "Rezept ansehen"
                            } else {
                                "Zu den Rezepten"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OfferHighlightCard(
    store: NearbyStore,
    offer: MarketOffer
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(offer.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("${store.distanceKm} km") }
                )
            }
            if (offer.subtitle.isNotBlank()) {
                Text(
                    text = offer.subtitle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(offer.priceLabel) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(offer.validityLabel) }
                )
            }
        }
    }
}

@Composable
private fun StoreCard(store: NearbyStore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(store.chain, style = MaterialTheme.typography.labelLarge)
                    Text(store.name, style = MaterialTheme.typography.titleMedium)
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${store.distanceKm} km") }
                )
            }
            Text(
                text = store.summary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = store.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                store.offers.take(3).forEach { offer ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(offer.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${offer.priceLabel} - ${offer.validityLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (offer.subtitle.isNotBlank()) {
                                Text(
                                    text = offer.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecipeRecommendation,
    onAddMissingIngredients: () -> Unit,
    onOpenRecipe: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(recommendation.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = recommendation.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when (recommendation.source) {
                                    RecommendationSource.SAVED -> "Gespeichert"
                                    RecommendationSource.AI -> "KI"
                                }
                            )
                        }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("${recommendation.score} Punkte") }
                    )
                }
            }

            Text(
                text = recommendation.rationale,
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Treffer", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recommendation.offerMatches, key = { it.offerId + it.ingredientName }) { match ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${match.ingredientName}: ${match.priceLabel}") }
                    )
                }
            }

            if (recommendation.missingIngredients.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fehlt noch", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = recommendation.missingIngredients.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddMissingIngredients,
                    enabled = recommendation.missingIngredients.isNotEmpty()
                ) {
                    Text("Fehlendes auf Liste")
                }
                OutlinedButton(
                    onClick = {
                        recommendation.recipeId?.let(onOpenRecipe)
                    },
                    enabled = recommendation.recipeId != null
                ) {
                    Text(
                        when (recommendation.source) {
                            RecommendationSource.SAVED -> "Rezept ansehen"
                            RecommendationSource.AI -> "KI-Idee"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecommendationsCard(onViewRecipes: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Noch keine starken Treffer", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Sobald du mehr Rezepte gespeichert hast, werden Angebote automatisch dagegen gematcht.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onViewRecipes) {
                Text("Zu den Rezepten")
            }
        }
    }
}
