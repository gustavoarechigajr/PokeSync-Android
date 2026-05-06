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
import com.pokesync.android.ui.screens.saves.SavesScreen
import com.pokesync.android.ui.screens.transfer.TransferScreen
import com.pokesync.android.ui.screens.vault.VaultScreen
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthStoreEntryPoint {
    fun authStore(): AuthStore
}

sealed class Screen(val route: String) {
    data object Connect : Screen("connect")
    data object Vault : Screen("vault")
    data object Saves : Screen("saves")
    data object Transfer : Screen("transfer/{saveId}") {
        fun createRoute(saveId: String) = "transfer/$saveId"
    }
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

    val startDestination = if (authStore.isLoggedIn()) Screen.Vault.route else Screen.Connect.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Connect.route) {
            ConnectScreen(onConnected = {
                navController.navigate(Screen.Vault.route) {
                    popUpTo(Screen.Connect.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Vault.route) {
            VaultScreen(onOpenSaves = { navController.navigate(Screen.Saves.route) })
        }
        composable(Screen.Saves.route) {
            SavesScreen(
                onTransfer = { saveId ->
                    navController.navigate(Screen.Transfer.createRoute(saveId))
                }
            )
        }
        composable(Screen.Transfer.route) { backStackEntry ->
            val saveId = backStackEntry.arguments?.getString("saveId") ?: return@composable
            TransferScreen(saveId = saveId, onBack = { navController.popBackStack() })
        }
    }
}
