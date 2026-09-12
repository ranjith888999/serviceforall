package com.srr.myapplication.auth

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import com.srr.myapplication.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavHostController, phoneNumber: String) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("Location Services Disabled") },
            text = { Text("Please enable GPS/Location services in settings to automatically fetch your location.") },
            confirmButton = {
                Button(onClick = {
                    showGpsDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Registration") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Personal Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Please provide your details to serve you better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(32.dp))

                RegistrationField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(value = address, onValueChange = { address = it }, label = "Residential Address", icon = Icons.Default.LocationOn)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Area/City Location") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        placeholder = { Text("Click Get Location") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isDetectingLocation = true
                                val result = LocationHelper.getCurrentLocation(context)
                                isDetectingLocation = false
                                if (result == null) {
                                    showGpsDialog = true
                                } else {
                                    locationName = result.name
                                    lat = result.latitude
                                    lng = result.longitude
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isDetectingLocation
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Get")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val phone = if (phoneNumber.startsWith("+91")) phoneNumber else "+91$phoneNumber"
                            val uid = auth.currentUser?.uid ?: phone
                            
                            val newUser = User(
                                uid = uid,
                                name = name,
                                email = email,
                                phone = phone,
                                role = "Customer",
                                address = address,
                                location = locationName,
                                latitude = lat,
                                longitude = lng,
                                locations = listOf(locationName).filter { it.isNotEmpty() },
                                locationCoords = listOf(mapOf("lat" to lat, "lng" to lng)).filter { locationName.isNotEmpty() },
                                selectedLocationIndex = 0,
                                registrationComplete = true
                            )
                            repository.saveUser(newUser)
                            navController.navigate(Screen.CustomerHome.createRoute(uid)) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotEmpty() && email.isNotEmpty() && locationName.isNotEmpty() && !isDetectingLocation
                ) {
                    Text("COMPLETE REGISTRATION")
                }
            }
        }
    }
}

@Composable
fun RegistrationField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
