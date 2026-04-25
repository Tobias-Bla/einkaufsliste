package com.example.einkaufsliste.data.discovery

data class DiscoveryProfile(
    val city: String,
    val radiusKm: Int,
    val preferredChains: List<String>
)

data class MarketOffer(
    val id: String,
    val storeId: String,
    val title: String,
    val subtitle: String = "",
    val priceLabel: String,
    val validityLabel: String,
    val tags: List<String> = emptyList()
)

data class NearbyStore(
    val id: String,
    val chain: String,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val summary: String,
    val offers: List<MarketOffer>
)

data class OfferDiscoveryFeed(
    val profile: DiscoveryProfile,
    val updatedAtLabel: String,
    val stores: List<NearbyStore>
) {
    val offers: List<MarketOffer>
        get() = stores.flatMap { it.offers }
}
