package com.srr.myapplication.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import com.srr.myapplication.util.AnalyticsHelper
import kotlinx.coroutines.launch

@Composable
fun OTPScreen(navController: NavHostController, phoneNumber: String, verificationId: String = "") {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    fun checkUserAndNavigate(uid: String, phone: String) {
        scope.launch {
            val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
            val userByPhone = repository.getUserByPhone(formattedPhone)
            val userByUid = if (uid.isNotEmpty()) repository.getUser(uid) else null
            val finalUser = userByUid ?: userByPhone

            if (finalUser != null && finalUser.registrationComplete) {
                AnalyticsHelper.trackEvent(AnalyticsHelper.EventType.LOGIN_SUCCESS, mapOf("role" to finalUser.role))
                when (finalUser.role) {
                    "Technician" -> navController.navigate(Screen.TechnicianHome.createRoute(finalUser.uid)) {
                        popUpTo(0) { inclusive = true }
                    }
                    "Admin" -> navController.navigate(Screen.AdminDashboard.createRoute(finalUser.uid)) {
                        popUpTo(0) { inclusive = true }
                    }
                    else -> navController.navigate(Screen.CustomerHome.createRoute(finalUser.uid)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                navController.navigate(Screen.RoleSelection.createRoute(phoneNumber))
            }
            isLoading = false
        }
    }

    fun verifyOtp() {
        if (otp.length != 6) return
        isLoading = true

        if (verificationId.isNotEmpty()) {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    checkUserAndNavigate(uid, phoneNumber)
                } else {
                    isLoading = false
                    val errorMsg = task.exception?.localizedMessage ?: "Invalid OTP Code"
                    Toast.makeText(context, "Verification Failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Fallback mode for testing/local verification
            val currentUid = auth.currentUser?.uid ?: ""
            checkUserAndNavigate(currentUid, phoneNumber)
        }
    }

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
                onClick = { verifyOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = otp.length == 6 && !isLoading
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
                TextButton(onClick = { navController.navigateUp() }) {
                    Text(text = "Resend / Back", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
