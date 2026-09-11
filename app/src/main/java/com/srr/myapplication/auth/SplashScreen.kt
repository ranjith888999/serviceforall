package com.srr.myapplication.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.R
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val scale = remember { Animatable(0f) }
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle results if needed
    }

    LaunchedEffect(key1 = true) {
        // Request permissions on start
        permissionsLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_CONTACTS
            )
        )

        scale.animateTo(
            targetValue = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1500L)
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val user = repository.getUser(currentUser.uid)
            if (user != null && user.registrationComplete) {
                when (user.role) {
                    "Technician" -> navController.navigate(Screen.TechnicianHome.createRoute(user.uid)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    "Admin" -> navController.navigate(Screen.AdminDashboard.createRoute(user.uid)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    else -> navController.navigate(Screen.CustomerHome.createRoute(user.uid)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            } else {
                navController.navigate(Screen.RoleSelection.createRoute(currentUser.phoneNumber ?: "")) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFF1F8F1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logosf),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SERVICE FOR EVER",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp
                )
            )
            Text(
                text = "Modern Service Solutions",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
