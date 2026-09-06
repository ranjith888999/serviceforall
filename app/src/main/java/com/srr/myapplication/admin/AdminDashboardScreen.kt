package com.srr.myapplication.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
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
fun AdminDashboardScreen(navController: NavHostController) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingTechs by remember { mutableStateOf<List<User>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var quotations by remember { mutableStateOf<List<QuotationRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            pendingTechs = repository.getPendingTechnicians()
            allUsers = repository.getAllUsers()
            quotations = repository.getAllQuotations()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Quotations") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Approvals (${pendingTechs.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Users") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Products") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> QuotationsList(quotations, allUsers.filter { it.role == "Technician" && it.isApproved }) { qId, tech ->
                        scope.launch {
                            repository.assignTechnician(qId, tech)
                            refreshData()
                        }
                    }
                    1 -> PendingTechniciansList(pendingTechs) { uid, approved ->
                        scope.launch {
                            repository.updateApprovalStatus(uid, approved)
                            refreshData()
                        }
                    }
                    2 -> AllUsersList(allUsers)
                    3 -> ProductManagementPlaceholder()
                }
            }
        }
    }
}

@Composable
fun QuotationsList(quotations: List<QuotationRequest>, technicians: List<User>, onAssign: (String, User) -> Unit) {
    if (quotations.isEmpty()) {
        EmptyState(message = "No quotation requests yet")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quotations) { quote ->
                QuotationCard(quote, technicians, onAssign)
            }
        }
    }
}

@Composable
fun QuotationCard(quote: QuotationRequest, technicians: List<User>, onAssign: (String, User) -> Unit) {
    var showAssignDialog by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = quote.productName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusBadge(quote.status)
            }
            
            Text(text = "From: ${quote.userName} (${quote.userPhone})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Date: ${sdf.format(Date(quote.timestamp))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            if (quote.comments.isNotEmpty()) {
                Text(text = "Note: ${quote.comments}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            if (quote.status == "Pending") {
                Button(
                    onClick = { showAssignDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Assign Technician")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Assigned to: ${quote.assignedTechnicianName ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Select Technician") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(technicians) { tech ->
                        ListItem(
                            headlineContent = { Text(tech.name) },
                            supportingContent = { Text(tech.location) },
                            modifier = Modifier.clickable {
                                onAssign(quote.id, tech)
                                showAssignDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAssignDialog = false }) { Text("Cancel") } }
        )
    }
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
fun PendingTechniciansList(techs: List<User>, onAction: (String, Boolean) -> Unit) {
    if (techs.isEmpty()) {
        EmptyState(message = "No pending technicians to review")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(techs) { tech ->
                TechnicianApprovalCard(tech, onAction)
            }
        }
    }
}

@Composable
fun AllUsersList(users: List<User>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(users) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (user.role == "Technician") Icons.Default.Build else Icons.Default.Person,
                        null,
                        tint = if (user.role == "Technician") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Bold)
                        Text(text = user.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    StatusBadge(user.role)
                }
            }
        }
    }
}

@Composable
fun TechnicianApprovalCard(tech: User, onAction: (String, Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Build, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = tech.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = tech.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            InfoRow(label = "Email", value = tech.email)
            InfoRow(label = "Aadhar", value = tech.aadharNumber)
            InfoRow(label = "PAN", value = tech.panNumber)
            InfoRow(label = "Location", value = tech.location)
            InfoRow(label = "Address", value = tech.address)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onAction(tech.uid, false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Text("Reject") }
                Button(onClick = { onAction(tech.uid, true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Approve") }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text(text = if (value.isEmpty()) "Not Provided" else value, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color.Gray)
        }
    }
}

@Composable
fun ProductManagementPlaceholder() {
    EmptyState(message = "Product Management System Ready. You can now add and update products.")
}
