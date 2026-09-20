package com.srr.myapplication.auth

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import com.srr.myapplication.util.AnalyticsHelper
import com.srr.myapplication.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TechnicianRegistrationScreen(navController: NavHostController, phoneNumber: String) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var aadharNumber by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var selectedExpertise by remember { mutableStateOf(setOf<String>()) }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    val expertiseOptions = listOf("CCTV", "EPABX", "Biometric", "Networking", "Cables", "IT Support", "Printers", "Water Controllers")

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
                title = { Text("Technician Registration") },
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
                    text = "Professional Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Fill in your professional information to start receiving jobs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                RegistrationField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(value = email, onValueChange = { email = it }, label = "Email Address (Optional)", icon = Icons.Default.Email)
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(
                    value = aadharNumber, 
                    onValueChange = { if (it.length <= 12) aadharNumber = it }, 
                    label = "Aadhar Card Number", 
                    icon = Icons.Default.CreditCard,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(
                    value = panNumber, 
                    onValueChange = { if (it.length <= 10) panNumber = it.uppercase() }, 
                    label = "PAN Card Number", 
                    icon = Icons.Default.Badge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(16.dp))
                RegistrationField(value = address, onValueChange = { address = it }, label = "Full Address", icon = Icons.Default.Home)
                Spacer(modifier = Modifier.height(16.dp))
                
                // expertise Selection
                Text(
                    text = "Select Expertise / Specialized Departments",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    expertiseOptions.forEach { expertise ->
                        FilterChip(
                            selected = selectedExpertise.contains(expertise),
                            onClick = {
                                selectedExpertise = if (selectedExpertise.contains(expertise)) {
                                    selectedExpertise - expertise
                                } else {
                                    selectedExpertise + expertise
                                }
                            },
                            label = { Text(expertise, fontSize = 12.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location Picker
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Service Location / City") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            enabled = !isDetectingLocation,
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
                            }
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = "Use My Location", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (validateTechnician(name, aadharNumber, panNumber, address, locationName, selectedExpertise)) {
                            isLoading = true
                            scope.launch {
                                try {
                                    val phone = if (phoneNumber.startsWith("+91")) phoneNumber else "+91$phoneNumber"
                                    val uid = auth.currentUser?.uid ?: phone
                                    
                                    val newUser = User(
                                        uid = uid,
                                        name = name,
                                        email = email,
                                        phone = phone,
                                        role = "Technician",
                                        address = address,
                                        location = locationName,
                                        latitude = lat,
                                        longitude = lng,
                                        locations = listOf(locationName).filter { it.isNotEmpty() },
                                        locationCoords = listOf(mapOf("lat" to lat, "lng" to lng)).filter { locationName.isNotEmpty() },
                                        selectedLocationIndex = 0,
                                        aadharNumber = aadharNumber,
                                        panNumber = panNumber,
                                        expertise = selectedExpertise.toList(),
                                        registrationComplete = true
                                    )
                                    repository.saveUser(newUser)
                                    AnalyticsHelper.trackEvent(AnalyticsHelper.EventType.REGISTRATION_COMPLETE, mapOf("role" to "Technician"))
                                    navController.navigate(Screen.TechnicianHome.createRoute(uid)) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    AnalyticsHelper.logError("TechRegistration", "Failed to save tech", e)
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotEmpty() && aadharNumber.length == 12 && panNumber.length == 10 && address.isNotEmpty() && locationName.isNotEmpty() && !isDetectingLocation && selectedExpertise.isNotEmpty()
                ) {
                    Text("REGISTER AS TECHNICIAN")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun validateTechnician(name: String, aadhar: String, pan: String, address: String, location: String, expertise: Set<String>): Boolean {
    return name.isNotEmpty() && aadhar.length == 12 && pan.length == 10 && address.isNotEmpty() && expertise.isNotEmpty()
}

@Composable
fun RegistrationField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions
    )
}
