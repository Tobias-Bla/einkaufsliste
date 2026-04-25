package com.example.einkaufsliste

import android.content.Context
import androidx.room.Room
import com.example.einkaufsliste.data.discovery.LocalDiscoveryRepository
import com.example.einkaufsliste.data.local.AppDatabase
import com.example.einkaufsliste.data.local.HouseholdStore
import com.example.einkaufsliste.data.remote.AuthService
import com.example.einkaufsliste.data.remote.FirestoreService
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.domain.recommendation.RecipeRecommendationEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "shopping-db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val householdStore: HouseholdStore by lazy { HouseholdStore(appContext) }
    private val authService: AuthService by lazy { AuthService() }
    private val firestoreService: FirestoreService by lazy { FirestoreService() }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(
            recipeDao = database.recipeDao(),
            authService = authService,
            firestoreService = firestoreService,
            householdStore = householdStore
        )
    }

    val discoveryRepository: LocalDiscoveryRepository by lazy { LocalDiscoveryRepository() }

    val recommendationEngine: RecipeRecommendationEngine by lazy {
        RecipeRecommendationEngine()
    }
}
