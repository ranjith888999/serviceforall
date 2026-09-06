package com.srr.myapplication.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun OTPScreen(navController: NavHostController, phoneNumber: String) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verification",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "We have sent an OTP to +91 $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = otp,
                onValueChange = { newValue -> if (newValue.length <= 6) otp = newValue },
                label = { Text("Enter 6-digit OTP") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (otp.length == 6) {
                        isLoading = true
                        scope.launch {
                            // Normalize phone number for lookup
                            val formattedPhone = if (phoneNumber.startsWith("+91")) phoneNumber else "+91$phoneNumber"
                            
                            // 1. Try finding user by phone number (Primary check)
                            val userByPhone = repository.getUserByPhone(formattedPhone)
                            
                            if (userByPhone != null && userByPhone.registrationComplete) {
                                // EXISTING USER FOUND: Skip Registration
                                when (userByPhone.role) {
                                    "Technician" -> navController.navigate(Screen.TechnicianHome.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    "Admin" -> navController.navigate(Screen.AdminDashboard.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    else -> navController.navigate(Screen.CustomerHome.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else {
                                // 2. Fallback: Try finding by current UID if authenticated
                                val currentUid = auth.currentUser?.uid
                                if (currentUid != null) {
                                    val userByUid = repository.getUser(currentUid)
                                    if (userByUid != null && userByUid.registrationComplete) {
                                        // User exists in Firestore under UID
                                        if (userByUid.role == "Technician") {
                                            navController.navigate(Screen.TechnicianHome.route) { popUpTo(0) { inclusive = true } }
                                        } else {
                                            navController.navigate(Screen.CustomerHome.route) { popUpTo(0) { inclusive = true } }
                                        }
                                    } else {
                                        // Authenticated but not registered
                                        navController.navigate(Screen.RoleSelection.createRoute(phoneNumber))
                                    }
                                } else {
                                    // 3. NEW USER: Go to Role Selection
                                    navController.navigate(Screen.RoleSelection.createRoute(phoneNumber))
                                }
                            }
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = otp.length == 6
            ) {
                Text(
                    text = "VERIFY & CONTINUE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Didn't receive code? ", color = Color.Gray)
                TextButton(onClick = { /* Resend OTP */ }) {
                    Text(text = "Resend", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
