package com.srr.myapplication.technician

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianHomeScreen(navController: NavHostController, uid: String) {
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    
    var user by remember { mutableStateOf<User?>(null) }
    var assignedTasks by remember { mutableStateOf<List<QuotationRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            // Use the passed UID to fetch user data
            user = repository.getUser(uid)
            // Filter quotations assigned to this technician (using the passed UID)
            assignedTasks = repository.getAllQuotations().filter { it.assignedTechnicianId == uid }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (user?.isApproved == false) {
            PendingApprovalScreen(onLogout = {
                auth.signOut()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Service Requests") },
                        actions = {
                            IconButton(onClick = { refreshData() }) { Icon(Icons.Default.Refresh, null) }
                            IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                                Icon(Icons.Default.Person, contentDescription = "Profile")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues)) {
                    if (assignedTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No assigned tasks yet", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
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
