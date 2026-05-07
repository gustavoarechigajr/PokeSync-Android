package com.pokesync.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pokesync.android.data.local.AuthStore
import com.pokesync.android.ui.screens.connect.ConnectScreen
import com.pokesync.android.ui.screens.home.HomeScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthStoreEntryPoint {
    fun authStore(): AuthStore
}

sealed class Screen(val route: String) {
    data object Connect : Screen("connect")
    data object Home : Screen("home")
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val authStore = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AuthStoreEntryPoint::class.java,
        ).authStore()
    }

    NavHost(
        navController = navController,
        startDestination = if (authStore.isLoggedIn()) Screen.Home.route else Screen.Connect.route,
    ) {
        composable(Screen.Connect.route) {
            ConnectScreen(onConnected = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Connect.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToConnect = {
                    navController.navigate(Screen.Connect.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
