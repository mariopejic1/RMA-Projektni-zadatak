package com.pejic.campmate

import ImageViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pejic.campmate.view.*
import com.pejic.campmate.viewmodel.AuthenticationViewModel
import com.pejic.campmate.viewmodel.CampsiteDetailsViewModel
import com.pejic.campmate.viewmodel.AllCampsitesViewModel
import com.pejic.campmate.viewmodel.CreateCampsiteViewModel
import com.pejic.campmate.viewmodel.NotificationViewModel
import com.pejic.campmate.viewmodel.UserViewModel
import androidx.compose.ui.platform.LocalContext
import com.pejic.campmate.viewmodel.ArchiveCampsiteViewModel
import com.pejic.campmate.viewmodel.LocationTrackingViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ALL_CAMPS = "all_camps"
    const val CREATE_CAMPSITE = "create_campsite"
    const val CAMPSITE_DETAIL = "campsite_detail"
    const val PROFILE = "profile"
    const val MY_CAMPSITES = "my_campsites"
    const val LOCATION_TRACKING = "location_tracking"
}

@Composable
fun NavigationController(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val imageViewModel: ImageViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            val authViewModel: AuthenticationViewModel = viewModel()
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onLoginSuccess = {
                    navController.navigate(Routes.ALL_CAMPS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            val authViewModel: AuthenticationViewModel = viewModel()
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ALL_CAMPS) {
            val campsiteListViewModel: AllCampsitesViewModel = viewModel()
            AllCampsitesScreen(
                navController = navController,
                viewModel = campsiteListViewModel,
                imageViewModel = imageViewModel
            )
        }
        composable(Routes.CREATE_CAMPSITE) {
            val createCampsiteViewModel: CreateCampsiteViewModel = viewModel()
            CreateCampsiteScreen(
                navController = navController,
                viewModel = createCampsiteViewModel,
                imageViewModel = imageViewModel
            )
        }
        composable(
            route = "${Routes.CAMPSITE_DETAIL}/{campsiteId}",
            arguments = listOf(navArgument("campsiteId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            val notificationViewModel: NotificationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return NotificationViewModel(context) as T
                    }
                }
            )
            val campsiteDetailsViewModel: CampsiteDetailsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CampsiteDetailsViewModel(context) as T
                    }
                }
            )
            CampsiteScreen(
                navController = navController,
                campsiteViewModel = campsiteDetailsViewModel,
                imageViewModel = imageViewModel,
                campsiteId = campsiteId
            )
        }
        composable(Routes.PROFILE) {
            val userViewModel: UserViewModel = viewModel()
            val authViewModel: AuthenticationViewModel = viewModel()
            ProfileScreen(
                navController = navController,
                userViewModel = userViewModel,
                authViewModel = authViewModel
            )
        }
        composable(Routes.MY_CAMPSITES) {
            val archiveCampsiteViewModel: ArchiveCampsiteViewModel = viewModel()
            ArchiveCampsiteScreen(
                navController = navController,
                viewModel = archiveCampsiteViewModel,
                imageViewModel = imageViewModel
            )
        }
        composable(Routes.LOCATION_TRACKING) {
            val locationViewModel: LocationTrackingViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LocationTrackingViewModel(context) as T
                    }
                }
            )
            LocationTrackingScreen(
                navController = navController,
                viewModel = locationViewModel
            )
        }
    }
}