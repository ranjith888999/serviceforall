package com.srr.myapplication.customer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    
    var user by remember { mutableStateOf<User?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var aadhar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val userData = repository.getUser(uid)
            user = userData
        } else {
            // If UID is null (mock login), try to find by some other means or show login
            // For now, if we're in this screen, we can try to fetch the most recent user or similar
            // But realistically, this screen shouldn't be reached if not logged in.
        }
        
        // Populate fields if user was found
        user?.let { userData ->
            name = userData.name
            email = userData.email
            address = userData.address
            location = userData.location
            aadhar = userData.aadharNumber
            pan = userData.panNumber
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else {
                        IconButton(onClick = {
                            isLoading = true
                            scope.launch {
                                val updatedUser = user?.copy(
                                    name = name,
                                    email = email,
                                    address = address,
                                    location = location,
                                    aadharNumber = aadhar,
                                    panNumber = pan
                                )
                                if (updatedUser != null) {
                                    repository.saveUser(updatedUser)
                                    user = updatedUser
                                }
                                isEditing = false
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Header
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = user?.role ?: "User",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                ProfileField(label = "Full Name", value = name, onValueChange = { name = it }, isEnabled = isEditing, icon = Icons.Default.Person)
                ProfileField(label = "Email Address", value = email, onValueChange = { email = it }, isEnabled = isEditing, icon = Icons.Default.Email)
                ProfileField(label = "Phone Number", value = user?.phone ?: "", onValueChange = {}, isEnabled = false, icon = Icons.Default.Phone)
                ProfileField(label = "Address", value = address, onValueChange = { address = it }, isEnabled = isEditing, icon = Icons.Default.Home)
                
                if (user?.role == "Technician") {
                    ProfileField(label = "Service Location", value = location, onValueChange = { location = it }, isEnabled = isEditing, icon = Icons.Default.LocationOn)
                    ProfileField(label = "Aadhar Number", value = aadhar, onValueChange = { aadhar = it }, isEnabled = isEditing, icon = Icons.Default.CreditCard)
                    ProfileField(label = "PAN Number", value = pan, onValueChange = { pan = it }, isEnabled = isEditing, icon = Icons.Default.Badge)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isEditing) {
                    Button(
                        onClick = { isEditing = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("Cancel", color = Color.Black)
                    }
                } else {
                    Button(
                        onClick = {
                            auth.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("LOGOUT")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit, isEnabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled,
            leadingIcon = { Icon(icon, contentDescription = null, tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color(0xFFEEEEEE),
                disabledLeadingIconColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                disabledPlaceholderColor = Color.Gray
            )
        )
    }
}
