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
import com.example.einkaufsliste.ui.screens.AddRecipeScreen
import com.example.einkaufsliste.ui.screens.HouseholdScreen
import com.example.einkaufsliste.ui.screens.ProductCatalogScreen
import com.example.einkaufsliste.ui.screens.RecipeDetailScreen
import com.example.einkaufsliste.ui.screens.RecipeListScreen
import com.example.einkaufsliste.ui.screens.ShoppingListScreen
import com.example.einkaufsliste.ui.screens.TodayScreen
import com.example.einkaufsliste.ui.theme.EinkaufslisteTheme
import com.example.einkaufsliste.ui.viewmodel.ShoppingViewModel
import com.example.einkaufsliste.ui.viewmodel.TodayViewModel

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
    val appContainer = remember { AppContainer(context.applicationContext) }
    val shoppingViewModel: ShoppingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShoppingViewModel(
                    repository = appContainer.recipeRepository,
                    recipeCatalogSeedDataSource = appContainer.recipeCatalogSeedDataSource
                ) as T
            }
        }
    )
    val todayViewModel: TodayViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodayViewModel(
                    recipeRepository = appContainer.recipeRepository,
                    discoveryRepository = appContainer.discoveryRepository,
                    recommendationEngine = appContainer.recommendationEngine,
                    aiRecipeSuggestionService = appContainer.aiRecipeSuggestionService
                ) as T
            }
        }
    )

    val navController = rememberNavController()
    val navigateToRecipesOverview: () -> Unit = {
        navController.navigate("recipes") {
            popUpTo("recipes") { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = "today") {
        composable("today") {
            TodayScreen(
                viewModel = todayViewModel,
                onViewRecipes = { navController.navigate("recipes") },
                onViewShoppingList = { navController.navigate("shopping_list") },
                onOpenHousehold = { navController.navigate("household") },
                onAddRecipeToShoppingList = { recipeId ->
                    shoppingViewModel.addSavedRecipeToShoppingList(recipeId)
                    navController.navigate("shopping_list")
                },
                onAddAiRecommendationToShoppingList = { recommendation ->
                    shoppingViewModel.addRecommendationToShoppingList(recommendation)
                    navController.navigate("shopping_list")
                }
            )
        }
        composable("recipes") {
            RecipeListScreen(
                viewModel = shoppingViewModel,
                onRecipeClick = { recipe ->
                    navController.navigate("recipe_detail/${recipe.id}")
                },
                onAddRecipeClick = { navController.navigate("add_recipe") },
                onViewShoppingList = { navController.navigate("shopping_list") },
                onManageProducts = { navController.navigate("products") }
            )
        }
        composable("recipe_detail/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            val recipes by shoppingViewModel.allRecipes.collectAsState()
            val recipe = recipes.find { it.id == recipeId }
            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    viewModel = shoppingViewModel,
                    onEdit = { navController.navigate("edit_recipe/${recipe.id}") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("add_recipe") {
            AddRecipeScreen(
                viewModel = shoppingViewModel,
                initialRecipe = null,
                onRecipeAdded = navigateToRecipesOverview,
                onBack = { navController.popBackStack() },
                onManageProducts = { navController.navigate("products") }
            )
        }
        composable("edit_recipe/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            val recipes by shoppingViewModel.allRecipes.collectAsState()
            val recipe = recipes.find { it.id == recipeId }
            if (recipe != null) {
                AddRecipeScreen(
                    viewModel = shoppingViewModel,
                    initialRecipe = recipe,
                    onRecipeAdded = navigateToRecipesOverview,
                    onBack = { navController.popBackStack() },
                    onManageProducts = { navController.navigate("products") }
                )
            }
        }
        composable("products") {
            ProductCatalogScreen(
                viewModel = shoppingViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("shopping_list") {
            ShoppingListScreen(
                viewModel = shoppingViewModel,
                onViewRecipes = { navController.navigate("recipes") },
                onOpenHousehold = { navController.navigate("household") },
                onManageProducts = { navController.navigate("products") }
            )
        }
        composable("household") {
            HouseholdScreen(
                viewModel = shoppingViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
