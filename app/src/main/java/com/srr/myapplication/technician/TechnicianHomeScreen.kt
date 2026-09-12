package com.srr.myapplication.technician

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import com.srr.myapplication.util.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianHomeScreen(navController: NavHostController, uid: String) {
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var userData by remember { mutableStateOf<User?>(null) }
    var assignedTasks by remember { mutableStateOf<List<QuotationRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showLocationDialog by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            userData = repository.getUser(uid)
            assignedTasks = repository.getAllQuotations().filter { it.assignedTechnicianId == uid }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

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

    if (showLocationDialog && userData != null) {
        LocationSelectionDialog(
            user = userData!!,
            isFetching = isFetchingLocation,
            onDismiss = { if (!isFetchingLocation) showLocationDialog = false },
            onLocationSelected = { index ->
                scope.launch {
                    val updatedUser = userData!!.copy(selectedLocationIndex = index)
                    repository.saveUser(updatedUser)
                    userData = updatedUser
                    showLocationDialog = false
                }
            },
            onAddLocation = { newLocation ->
                scope.launch {
                    val currentLocations = userData!!.locations.toMutableList()
                    if (currentLocations.size < 3) {
                        currentLocations.add(newLocation)
                        val updatedUser = userData!!.copy(
                            locations = currentLocations,
                            selectedLocationIndex = currentLocations.size - 1
                        )
                        repository.saveUser(updatedUser)
                        userData = updatedUser
                    }
                    showLocationDialog = false
                }
            },
            onAutoFetch = {
                scope.launch {
                    isFetchingLocation = true
                    val result = LocationHelper.getCurrentLocation(context)
                    isFetchingLocation = false
                    
                    if (result == null) {
                        showGpsDialog = true
                    } else {
                        val fetched = result.name
                        val currentLat = result.latitude
                        val currentLng = result.longitude
                        
                        val currentLocations = userData!!.locations.toMutableList()
                        val currentCoords = userData!!.locationCoords.toMutableList()
                        
                        if (currentLocations.size < 3) {
                            currentLocations.add(fetched)
                            currentCoords.add(mapOf("lat" to currentLat, "lng" to currentLng))
                            val updatedUser = userData!!.copy(
                                locations = currentLocations,
                                locationCoords = currentCoords,
                                selectedLocationIndex = currentLocations.size - 1
                            )
                            repository.saveUser(updatedUser)
                            userData = updatedUser
                        } else {
                            currentLocations[userData!!.selectedLocationIndex] = fetched
                            currentCoords[userData!!.selectedLocationIndex] = mapOf("lat" to currentLat, "lng" to currentLng)
                            val updatedUser = userData!!.copy(
                                locations = currentLocations,
                                locationCoords = currentCoords
                            )
                            repository.saveUser(updatedUser)
                            userData = updatedUser
                        }
                        showLocationDialog = false
                    }
                }
            }
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (userData?.isApproved == false) {
            PendingApprovalScreen(onLogout = {
                auth.signOut()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        } else {
            Scaffold(
                topBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Location Display in Top Left
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showLocationDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    val currentLoc = userData?.let { 
                                        if (it.locations.isNotEmpty()) it.locations.getOrNull(it.selectedLocationIndex) ?: it.location
                                        else it.location
                                    } ?: "Select Location"
                                    
                                    Text(
                                        text = currentLoc.ifEmpty { "Select Location" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Technician Area",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                            }
                            
                            Row {
                                IconButton(onClick = { refreshData() }) { Icon(Icons.Default.Refresh, null) }
                                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                                    Icon(Icons.Default.Person, contentDescription = "Profile")
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues)) {
                    Text(
                        text = "Assigned Service Requests",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    if (assignedTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No assigned tasks yet", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(assignedTasks) { task ->
                                TaskCard(task) { status, price, comment ->
                                    scope.launch {
                                        repository.updateQuotationStatus(task.id, status, comment, price)
                                        refreshData()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationSelectionDialog(
    user: User,
    isFetching: Boolean,
    onDismiss: () -> Unit,
    onLocationSelected: (Int) -> Unit,
    onAddLocation: (String) -> Unit,
    onAutoFetch: () -> Unit
) {
    var newLocationText by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Working Location") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isFetching) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    Text(
                        "Detecting your location...",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    user.locations.forEachIndexed { index, loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLocationSelected(index) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == user.selectedLocationIndex,
                                onClick = { onLocationSelected(index) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = loc, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (user.locations.size < 3 && !isAddingNew) {
                        TextButton(
                            onClick = { isAddingNew = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add New Location")
                        }
                    }

                    if (isAddingNew) {
                        OutlinedTextField(
                            value = newLocationText,
                            onValueChange = { newLocationText = it },
                            label = { Text("Enter Location Manually") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            trailingIcon = {
                                IconButton(onClick = { if (newLocationText.isNotEmpty()) onAddLocation(newLocationText) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Add")
                                }
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    Button(
                        onClick = onAutoFetch,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-detect Current Location")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isFetching) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TaskCard(task: QuotationRequest, onUpdate: (String, Double?, String?) -> Unit) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = task.productName, fontWeight = FontWeight.Bold)
                StatusBadge(task.status)
            }
            Text(text = "Customer: ${task.userName}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Contact: ${task.userPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(text = "Requested: ${sdf.format(Date(task.timestamp))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            if (task.comments.isNotEmpty()) {
                Text(text = "Issue: ${task.comments}", modifier = Modifier.padding(top = 8.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            if (task.status != "Completed" && task.status != "Cancelled") {
                Button(
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Work Status")
                }
            } else if (task.finalPrice != null) {
                Text(text = "Final Price: ₹${task.finalPrice}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }

    if (showUpdateDialog) {
        var status by remember { mutableStateOf(task.status) }
        var price by remember { mutableStateOf("") }
        var comments by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Progress") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Status: $status", style = MaterialTheme.typography.labelMedium)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip("In Progress", status == "In Progress") { status = "In Progress" }
                        StatusChip("Completed", status == "Completed") { status = "Completed" }
                        StatusChip("Cancelled", status == "Cancelled") { status = "Cancelled" }
                    }

                    if (status == "Completed") {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Final Price (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Technician Comments") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(status, price.toDoubleOrNull(), comments)
                    showUpdateDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StatusChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) }
    )
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Pending" -> Color(0xFFFFA000)
        "Assigned" -> Color(0xFF2196F3)
        "In Progress" -> Color(0xFF9C27B0)
        "Completed" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PendingApprovalScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.padding(30.dp).size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Approval Pending",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your profile is currently under review by our admin team. This usually takes 24-48 hours.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOGOUT")
        }
    }
}
