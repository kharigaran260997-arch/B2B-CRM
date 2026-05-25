package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.CrmViewModel
import com.example.ui.CrmViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Dark state toggle synced
            var darkThemeEnabled by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = darkThemeEnabled) {
                // Instantiate database and repo singletons via Application context
                val app = application as MainApplication
                val factory = CrmViewModelFactory(app.repository)
                val crmViewModel: CrmViewModel = viewModel(factory = factory)

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "auth",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Splash, Auth credentials gate
                    composable("auth") {
                        SplashAuthScreen(
                            viewModel = crmViewModel,
                            onNavigateToDashboard = {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Main central core layout hosting bottom navigation tabs
                    composable("main") {
                        var currentTab by remember { mutableStateOf(0) }

                        Scaffold(
                            bottomBar = {
                                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard", fontSize = 11.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Pipeline") },
                                        label = { Text("Pipeline", fontSize = 11.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2 },
                                        icon = { Icon(Icons.Default.Group, contentDescription = "Team") },
                                        label = { Text("Team", fontSize = 11.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3 },
                                        icon = { Icon(Icons.Default.ShowChart, contentDescription = "Forecast") },
                                        label = { Text("Forecast", fontSize = 11.sp) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 4,
                                        onClick = { currentTab = 4 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings", fontSize = 11.sp) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentTab) {
                                    0 -> DashboardScreen(
                                        viewModel = crmViewModel,
                                        onNavigateToDealDetail = { id -> navController.navigate("detail/$id") },
                                        onNavigateToNotifications = { navController.navigate("notifications") },
                                        onNavigateToAddDeal = { navController.navigate("add_edit?id=NULL") }
                                    )
                                    1 -> PipelineScreen(
                                        viewModel = crmViewModel,
                                        onNavigateToDealDetail = { id -> navController.navigate("detail/$id") },
                                        onNavigateToAddDeal = { navController.navigate("add_edit?id=NULL") }
                                    )
                                    2 -> TeamScreen(
                                        viewModel = crmViewModel,
                                        onNavigateToDealDetail = { id -> navController.navigate("detail/$id") }
                                    )
                                    3 -> ForecastScreen(viewModel = crmViewModel)
                                    4 -> SettingsScreen(
                                        viewModel = crmViewModel,
                                        onDarkThemeToggle = { darkThemeEnabled = it },
                                        currentDarkState = darkThemeEnabled
                                    )
                                }
                            }
                        }
                    }

                    // Deal details screen
                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val dealId = backStackEntry.arguments?.getString("id") ?: ""
                        DealDetailScreen(
                            dealId = dealId,
                            viewModel = crmViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEdit = { id -> navController.navigate("add_edit?id=$id") }
                        )
                    }

                    // Form screen (optional edit ID)
                    composable(
                        route = "add_edit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "NULL" })
                    ) { backStackEntry ->
                        val rawId = backStackEntry.arguments?.getString("id")
                        val dealId = if (rawId == "NULL" || rawId.isNullOrBlank()) null else rawId
                        AddEditDealScreen(
                            dealId = dealId,
                            viewModel = crmViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Notifications center alert hub
                    composable("notifications") {
                        NotificationsScreen(
                            viewModel = crmViewModel,
                            onNavigateToDealDetail = { id -> navController.navigate("detail/$id") },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
