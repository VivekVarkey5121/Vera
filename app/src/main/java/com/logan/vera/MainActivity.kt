package com.logan.vera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Includes Button, Icon, NavigationBar, etc.
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.logan.vera.ui.navigation.Screen
import com.logan.vera.ui.navigation.VeraNavGraph
import com.logan.vera.ui.theme.VeraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VeraTheme {
                // 1. Create a state to track if we have permission
                // We initialize it by checking the system status
                var hasPermission by remember { 
                    mutableStateOf(checkStoragePermission()) 
                }

                // 2. Logic to decide which screen to show
                if (!hasPermission) {
                    // SHOW THIS if permission is missing
                    PermissionRequestScreen(
                        onGrantClick = { requestAllFilesAccess(this) },
                        onCheckAgain = { hasPermission = checkStoragePermission() }
                    )
                } else {
                    // SHOW THIS if everything is okay
                    MainScreen()
                }
            }
        }
    }

    // Helper to check the permission status
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Older phones use standard manifest permissions
        }
    }
}

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit, onCheckAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vera needs All Files Access to find and read your Ebooks.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantClick) {
            Text("Open Settings")
        }
        TextButton(onClick = onCheckAgain) {
            Text("I already gave permission")
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var showBottomBar by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == screen.route) {
                                        screen.selectedIcon
                                    } else {
                                        screen.unselectedIcon
                                    },
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            VeraNavGraph(
                navController = navController,
                onShowBottomBar = { show -> showBottomBar = show }
            )
        }
    }
}

fun requestAllFilesAccess(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            activity.startActivity(intent)
        }
    }
}