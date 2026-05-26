package com.astpredict.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astpredict.app.data.db.AppDatabase
import com.astpredict.app.data.ml.ColonyDetector
import com.astpredict.app.ui.navigation.AppNavigation
import com.astpredict.app.ui.navigation.Screen
import com.astpredict.app.ui.theme.ASTTheme

class MainActivity : ComponentActivity() {

    private lateinit var detector: ColonyDetector
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ML detector
        detector = ColonyDetector(applicationContext)
        try {
            detector.initialize(useGpu = false)
        } catch (e: Exception) {
            // Model might not be available yet — handle gracefully
            e.printStackTrace()
        }

        // Initialize database
        database = AppDatabase.getDatabase(applicationContext)

        setContent {
            ASTTheme(darkTheme = true) {
                ASTApp(detector = detector, database = database)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ASTApp(
    detector: ColonyDetector,
    database: AppDatabase
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Bottom nav items
    data class NavItem(
        val route: String,
        val label: String,
        val selectedIcon: @Composable () -> Unit,
        val unselectedIcon: @Composable () -> Unit
    )

    val navItems = listOf(
        NavItem(
            Screen.Home.route, "Home",
            { Icon(Icons.Filled.Home, contentDescription = "Home") },
            { Icon(Icons.Outlined.Home, contentDescription = "Home") }
        ),
        NavItem(
            Screen.Camera.route, "Analyze",
            { Icon(Icons.Filled.CameraAlt, contentDescription = "Analyze") },
            { Icon(Icons.Outlined.CameraAlt, contentDescription = "Analyze") }
        ),
        NavItem(
            Screen.History.route, "History",
            { Icon(Icons.Filled.History, contentDescription = "History") },
            { Icon(Icons.Outlined.History, contentDescription = "History") }
        ),
        NavItem(
            Screen.Settings.route, "Settings",
            { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        )
    )

    // Hide bottom bar on results screen
    val showBottomBar = currentRoute != null && !currentRoute.startsWith("results/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { if (selected) item.selectedIcon() else item.unselectedIcon() },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(
                navController = navController,
                detector = detector,
                database = database
            )
        }
    }
}