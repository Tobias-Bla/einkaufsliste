package com.example.einkaufsliste.data.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalDiscoveryRepository {

    fun discoveryFeed(): Flow<OfferDiscoveryFeed> = flowOf(sampleFeed())

    private fun sampleFeed(): OfferDiscoveryFeed {
        val reweStoreId = "rewe-bad-camberg-limburger-str"
        val lidlStoreId = "lidl-bad-camberg-frankfurter-str"
        val aldiStoreId = "aldi-sued-bad-camberg"

        val reweOffers = listOf(
            MarketOffer(
                id = "rewe-passierte-tomaten",
                storeId = reweStoreId,
                title = "REWE Beste Wahl Passierte Tomaten, 500 g",
                subtitle = "Ideal fuer Pasta, Lasagne und Saucen",
                priceLabel = "0,79 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("passierte tomaten", "tomaten")
            ),
            MarketOffer(
                id = "rewe-rinderhack",
                storeId = reweStoreId,
                title = "Frisches Rinder-Hackfleisch, 500 g",
                subtitle = "Aus der Bedienung oder SB je nach Verfuegbarkeit",
                priceLabel = "3,49 EUR",
                validityLabel = "Do-Sa",
                tags = listOf("rinderhackfleisch", "hackfleisch")
            ),
            MarketOffer(
                id = "rewe-lasagneplatten",
                storeId = reweStoreId,
                title = "Barilla Lasagneplatten",
                subtitle = "Trockenware fuer Ofengerichte",
                priceLabel = "1,49 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("lasagneplatten", "nudelplatten")
            ),
            MarketOffer(
                id = "rewe-gouda",
                storeId = reweStoreId,
                title = "Gouda gerieben, 250 g",
                subtitle = "Zum Ueberbacken",
                priceLabel = "1,79 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("kaese", "gouda")
            )
        )

        val lidlOffers = listOf(
            MarketOffer(
                id = "lidl-zucchini",
                storeId = lidlStoreId,
                title = "Zucchini, Klasse I",
                subtitle = "Regional verfuegbar",
                priceLabel = "0,89 EUR",
                validityLabel = "Do-Sa",
                tags = listOf("zucchini")
            ),
            MarketOffer(
                id = "lidl-mozzarella",
                storeId = lidlStoreId,
                title = "Mozzarella, 125 g",
                subtitle = "Passt zu Ofengemuese oder Pasta",
                priceLabel = "0,69 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("mozzarella", "kaese")
            ),
            MarketOffer(
                id = "lidl-basilikum",
                storeId = lidlStoreId,
                title = "Basilikum im Topf",
                subtitle = "Frische Kraeuter fuer Tomatengerichte",
                priceLabel = "1,29 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("basilikum")
            )
        )

        val aldiOffers = listOf(
            MarketOffer(
                id = "aldi-kidneybohnen",
                storeId = aldiStoreId,
                title = "Kidneybohnen, 400 g",
                subtitle = "Fuer Chili und Bowls",
                priceLabel = "0,79 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("kidneybohnen", "bohnen")
            ),
            MarketOffer(
                id = "aldi-paprika",
                storeId = aldiStoreId,
                title = "Paprika Mix, 500 g",
                subtitle = "Rot, gelb und gruen",
                priceLabel = "1,49 EUR",
                validityLabel = "Do-Sa",
                tags = listOf("paprika")
            ),
            MarketOffer(
                id = "aldi-spaghetti",
                storeId = aldiStoreId,
                title = "Italienische Spaghetti, 500 g",
                subtitle = "Fuer schnelle Feierabendgerichte",
                priceLabel = "0,88 EUR",
                validityLabel = "Mo-Sa",
                tags = listOf("spaghetti", "nudeln")
            )
        )

        return OfferDiscoveryFeed(
            profile = DiscoveryProfile(
                city = "Bad Camberg",
                radiusKm = 8,
                preferredChains = listOf("REWE", "Lidl", "ALDI SUED")
            ),
            updatedAtLabel = "Heute, 08:30",
            stores = listOf(
                NearbyStore(
                    id = reweStoreId,
                    chain = "REWE",
                    name = "REWE Markt Limburger Str. 63",
                    address = "Limburger Str. 63, 65520 Bad Camberg",
                    distanceKm = 1.2,
                    summary = "Stark fuer Lasagne, Pasta und Tomatensaucen.",
                    offers = reweOffers
                ),
                NearbyStore(
                    id = lidlStoreId,
                    chain = "Lidl",
                    name = "Lidl Frankfurter Str.",
                    address = "Frankfurter Str. 8, 65520 Bad Camberg",
                    distanceKm = 1.8,
                    summary = "Frische Gemuese- und Mozzarella-Angebote.",
                    offers = lidlOffers
                ),
                NearbyStore(
                    id = aldiStoreId,
                    chain = "ALDI SUED",
                    name = "ALDI SUED Bad Camberg",
                    address = "Robert-Bosch-Str. 2, 65520 Bad Camberg",
                    distanceKm = 2.1,
                    summary = "Gut fuer Chili, Pasta und Vorrat.",
                    offers = aldiOffers
                )
            )
        )
    }
}
