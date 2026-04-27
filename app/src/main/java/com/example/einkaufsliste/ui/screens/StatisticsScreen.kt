package com.example.einkaufsliste.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.einkaufsliste.data.model.PurchasedProductStat
import com.example.einkaufsliste.data.model.PurchasedRecipeStat
import com.example.einkaufsliste.data.repository.normalizedKey
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: ShoppingViewModel,
    onBack: () -> Unit
) {
    val recipeStats by viewModel.purchasedRecipeStats.collectAsState()
    val productStats by viewModel.purchasedProductStats.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState()
    val ingredientCatalog by viewModel.ingredientCatalog.collectAsState()

    val recipeImageById = recipes.associate { it.id to it.imageUrl }
    val productImageByName = ingredientCatalog.associate { it.name.normalizedKey() to it.imageUrl }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiken") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                StatsSectionHeader(
                    title = "Am haeufigsten gekaufte Rezepte",
                    subtitle = "Wird beim Abschluss einer Einkaufsliste als Kauf gezaehlt."
                )
            }

            if (recipeStats.isEmpty()) {
                item {
                    EmptyStatsCard("Noch keine gekauften Rezepte erfasst.")
                }
            } else {
                items(recipeStats, key = { it.recipeId }) { stat ->
                    StatsRecipeRow(
                        stat = stat,
                        imageUrl = recipeImageById[stat.recipeId]
                    )
                }
            }

            item {
                StatsSectionHeader(
                    title = "Am haeufigsten gekaufte Produkte",
                    subtitle = "Produkte aus abgeschlossenen Einkaufslisten."
                )
            }

            if (productStats.isEmpty()) {
                item {
                    EmptyStatsCard("Noch keine gekauften Produkte erfasst.")
                }
            } else {
                items(productStats, key = { it.normalizedName }) { stat ->
                    StatsProductRow(
                        stat = stat,
                        imageUrl = productImageByName[stat.normalizedName]
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSectionHeader(
    title: String,
    subtitle: String
) {
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
private fun StatsRecipeRow(
    stat: PurchasedRecipeStat,
    imageUrl: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsImage(imageUrl = imageUrl)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stat.recipeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stat.purchaseCount}x gekauft",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text("#${stat.purchaseCount}") }
            )
        }
    }
}

@Composable
private fun StatsProductRow(
    stat: PurchasedProductStat,
    imageUrl: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsImage(imageUrl = imageUrl)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stat.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stat.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text("${stat.purchaseCount}x") }
            )
        }
    }
}

@Composable
private fun StatsImage(imageUrl: String?) {
    if (imageUrl.isNullOrBlank()) {
        Spacer(Modifier.width(0.dp))
        return
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .size(58.dp)
            .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun EmptyStatsCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
