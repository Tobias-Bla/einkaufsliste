package com.example.einkaufsliste.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.discovery.MarketOffer
import com.example.einkaufsliste.data.discovery.NearbyStore
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendation
import com.example.einkaufsliste.ui.viewmodel.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onViewRecipes: () -> Unit,
    onViewShoppingList: () -> Unit,
    onOpenHousehold: () -> Unit,
    onAddRecipeToShoppingList: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val feed = uiState.feed
    var expandedStoreId by rememberSaveable { mutableStateOf<String?>(null) }
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
                    onAddRecipeToShoppingList = onAddRecipeToShoppingList
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
                    subtitle = "Waehle einen Markt, um alle aktuellen Angebote zu sehen"
                )
            }
            items(uiState.highlightedStores, key = { it.id }) { store ->
                StoreCard(
                    store = store,
                    expanded = expandedStoreId == store.id,
                    onToggleExpanded = {
                        expandedStoreId = if (expandedStoreId == store.id) null else store.id
                    }
                )
            }
        }
    }
}

@Composable
private fun DailyTipCard(
    recommendation: RecipeRecommendation?,
    imageUrl: String?,
    onViewRecipes: () -> Unit,
    onAddRecipeToShoppingList: (String) -> Unit
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
                Button(
                    onClick = {
                        recommendation.recipeId?.let(onAddRecipeToShoppingList)
                    },
                    enabled = recommendation.recipeId != null
                ) {
                    Text("Rezept hinzufuegen")
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
private fun StoreCard(
    store: NearbyStore,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val visibleOffers = if (expanded) store.offers else store.offers.take(3)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .animateContentSize(),
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
            if (store.offers.size > 3) {
                Text(
                    text = if (expanded) {
                        "Alle ${store.offers.size} Angebote angezeigt - tippen zum Einklappen"
                    } else {
                        "${store.offers.size} Angebote verfuegbar - tippen fuer alle Angebote"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleOffers.forEach { offer ->
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
