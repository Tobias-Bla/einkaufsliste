package com.example.einkaufsliste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.einkaufsliste.data.local.AppDatabase
import com.example.einkaufsliste.data.local.HouseholdStore
import com.example.einkaufsliste.data.remote.AuthService
import com.example.einkaufsliste.data.remote.FirestoreService
import com.example.einkaufsliste.data.repository.RecipeRepository
import com.example.einkaufsliste.ui.screens.AddRecipeScreen
import com.example.einkaufsliste.ui.screens.HouseholdScreen
import com.example.einkaufsliste.ui.screens.RecipeDetailScreen
import com.example.einkaufsliste.ui.screens.RecipeListScreen
import com.example.einkaufsliste.ui.screens.ShoppingListScreen
import com.example.einkaufsliste.ui.theme.EinkaufslisteTheme
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EinkaufslisteTheme {
                MainApp()
            }
        }
    }
}


@Composable
fun MainApp() {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val db = remember {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "shopping-db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    val householdStore = remember { HouseholdStore(applicationContext) }
    val authService = remember { AuthService() }
    val firestoreService = remember { FirestoreService() }
    val repository = remember { RecipeRepository(db.recipeDao(), authService, firestoreService, householdStore) }
    val viewModel: ShoppingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShoppingViewModel(repository) as T
            }
        }
    )

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "shopping_list") {
        composable("recipes") {
            RecipeListScreen(
                viewModel = viewModel,
                onRecipeClick = { recipe ->
                    navController.navigate("recipe_detail/${recipe.id}")
                },
                onAddRecipeClick = { navController.navigate("add_recipe") },
                onViewShoppingList = { navController.navigate("shopping_list") }
            )
        }
        composable("recipe_detail/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            val recipes by viewModel.allRecipes.collectAsState()
            val recipe = recipes.find { it.id == recipeId }
            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("add_recipe") {
            AddRecipeScreen(
                viewModel = viewModel,
                onRecipeAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable("shopping_list") {
            ShoppingListScreen(
                viewModel = viewModel,
                onViewRecipes = { navController.navigate("recipes") },
                onOpenHousehold = { navController.navigate("household") }
            )
        }
        composable("household") {
            HouseholdScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
