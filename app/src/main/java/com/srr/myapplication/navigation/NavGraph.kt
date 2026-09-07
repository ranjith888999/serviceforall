package com.srr.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.srr.myapplication.auth.*
import com.srr.myapplication.customer.*
import com.srr.myapplication.technician.*
import com.srr.myapplication.admin.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object OTP : Screen("otp/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp/$phoneNumber"
    }
    object RoleSelection : Screen("role_selection/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "role_selection/$phoneNumber"
    }
    object Registration : Screen("registration/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "registration/$phoneNumber"
    }
    object TechnicianRegistration : Screen("technician_registration/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "technician_registration/$phoneNumber"
    }
    
    // Customer Screens
    object CustomerHome : Screen("customer_home/{uid}") {
        fun createRoute(uid: String) = "customer_home/$uid"
    }
    object ProductDetails : Screen("product_details/{productId}") {
        fun createRoute(productId: String) = "product_details/$productId"
    }
    object Cart : Screen("cart")
    object Profile : Screen("profile")
    
    // Technician Screens
    object TechnicianHome : Screen("technician_home/{uid}") {
        fun createRoute(uid: String) = "technician_home/$uid"
    }
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard/{uid}") {
        fun createRoute(uid: String) = "admin_dashboard/$uid"
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.OTP.route) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OTPScreen(navController, phoneNumber)
        }
        composable(Screen.RoleSelection.route) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            RoleSelectionScreen(navController, phoneNumber)
        }
        composable(Screen.Registration.route) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            RegistrationScreen(navController, phoneNumber)
        }
        composable(Screen.TechnicianRegistration.route) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            TechnicianRegistrationScreen(navController, phoneNumber)
        }
        
        // Customer
        composable(Screen.CustomerHome.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            CustomerHomeScreen(navController, uid)
        }
        composable(Screen.ProductDetails.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailsScreen(navController, productId)
        }
        composable(Screen.Cart.route) { CartScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        
        // Technician
        composable(Screen.TechnicianHome.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            TechnicianHomeScreen(navController, uid)
        }
        
        // Admin
        composable(Screen.AdminDashboard.route) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            AdminDashboardScreen(navController, uid)
        }
    }
}
