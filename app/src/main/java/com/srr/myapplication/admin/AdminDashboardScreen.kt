package com.srr.myapplication.admin

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.model.Product
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavHostController, uid: String) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var allTechnicians by remember { mutableStateOf<List<User>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var quotations by remember { mutableStateOf<List<QuotationRequest>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            try {
                val users = repository.getAllUsers()
                allUsers = users
                allTechnicians = users.filter { it.role == "Technician" }
                quotations = repository.getAllQuotations()
                products = repository.getProducts()
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
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
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Approvals") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Users") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Products") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> QuotationsList(quotations, allTechnicians.filter { it.isApproved }) { qId, tech ->
                        scope.launch {
                            try {
                                repository.assignTechnician(qId, tech)
                                Toast.makeText(context, "Assigned to ${tech.name}", Toast.LENGTH_SHORT).show()
                                refreshData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to assign: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    1 -> TechniciansApprovalList(allTechnicians) { techUid, approved ->
                        scope.launch {
                            try {
                                repository.updateApprovalStatus(techUid, approved)
                                Toast.makeText(context, if (approved) "Technician Approved" else "Approval Revoked", Toast.LENGTH_SHORT).show()
                                refreshData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    2 -> AllUsersList(allUsers, 
                        onDelete = { deleteUid -> 
                            scope.launch { 
                                repository.deleteUser(deleteUid)
                                refreshData()
                            } 
                        },
                        onUpdate = { userToUpdate -> 
                            scope.launch { 
                                repository.saveUser(userToUpdate)
                                refreshData()
                            } 
                        }
                    )
                    3 -> ProductManagementList(products,
                        onDelete = { pId -> 
                            scope.launch { 
                                repository.deleteProduct(pId)
                                refreshData()
                            } 
                        },
                        onSave = { prod -> 
                            scope.launch { 
                                repository.addOrUpdateProduct(prod)
                                refreshData()
                            } 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TechniciansApprovalList(allTechs: List<User>, onAction: (String, Boolean) -> Unit) {
    var filterState by remember { mutableStateOf("Pending") } // "Pending", "Approved", "All"
    
    val filteredTechs = remember(allTechs, filterState) {
        when (filterState) {
            "Pending" -> allTechs.filter { !it.isApproved }
            "Approved" -> allTechs.filter { it.isApproved }
            else -> allTechs
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filterState == "Pending", onClick = { filterState = "Pending" }, label = { Text("Pending") })
            FilterChip(selected = filterState == "Approved", onClick = { filterState = "Approved" }, label = { Text("Approved") })
            FilterChip(selected = filterState == "All", onClick = { filterState = "All" }, label = { Text("All") })
        }

        if (filteredTechs.isEmpty()) {
            EmptyState(message = "No technicians found for this filter")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(filteredTechs) { tech ->
                    TechnicianApprovalCard(tech, onAction)
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
                if (technicians.isEmpty()) {
                    Text("No approved technicians available.")
                } else {
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
        "Technician" -> Color(0xFF4CAF50)
        "Customer" -> Color(0xFF2196F3)
        "Admin" -> Color(0xFFF44336)
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
fun AllUsersList(users: List<User>, onDelete: (String) -> Unit, onUpdate: (User) -> Unit) {
    var showEditDialog by remember { mutableStateOf<User?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showEditDialog = user },
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
                        IconButton(onClick = { onDelete(user.uid) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showEditDialog = User(role = "Customer") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add User")
        }
    }

    if (showEditDialog != null) {
        UserEditDialog(user = showEditDialog!!, onDismiss = { showEditDialog = null }, onSave = onUpdate)
    }
}

@Composable
fun UserEditDialog(user: User, onDismiss: () -> Unit, onSave: (User) -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var role by remember { mutableStateOf(user.role) }
    var address by remember { mutableStateOf(user.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user.uid.isEmpty()) "Create User" else "Edit User") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                
                Text("Role", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = role == "Customer", onClick = { role = "Customer" }, label = { Text("Customer") })
                    FilterChip(selected = role == "Technician", onClick = { role = "Technician" }, label = { Text("Technician") })
                    FilterChip(selected = role == "Admin", onClick = { role = "Admin" }, label = { Text("Admin") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalUid = if (user.uid.isEmpty()) phone else user.uid
                onSave(user.copy(uid = finalUid, name = name, email = email, phone = phone, role = role, address = address, registrationComplete = true))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProductManagementList(products: List<Product>, onDelete: (String) -> Unit, onSave: (Product) -> Unit) {
    var showEditDialog by remember { mutableStateOf<Product?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showEditDialog = product },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, fontWeight = FontWeight.Bold)
                            Text(text = "Category ID: ${product.categoryId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { onDelete(product.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showEditDialog = Product() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    if (showEditDialog != null) {
        ProductEditDialog(product = showEditDialog!!, onDismiss = { showEditDialog = null }, onSave = onSave)
    }
}

@Composable
fun ProductEditDialog(product: Product, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var categoryId by remember { mutableStateOf(product.categoryId) }
    var price by remember { mutableStateOf(product.price.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product.id.isEmpty()) "Create Product" else "Edit Product") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.height(100.dp))
                OutlinedTextField(value = categoryId, onValueChange = { categoryId = it }, label = { Text("Category ID (e.g. cat_cctv)") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(product.copy(name = name, description = description, categoryId = categoryId, price = price.toDoubleOrNull() ?: 0.0))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
                Spacer(modifier = Modifier.weight(1f))
                if (tech.isApproved) {
                    StatusBadge("Approved")
                } else {
                    StatusBadge("Pending")
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
                if (tech.isApproved) {
                    OutlinedButton(onClick = { onAction(tech.uid, false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Text("Revoke Approval") }
                } else {
                    OutlinedButton(onClick = { onAction(tech.uid, false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Text("Reject") }
                    Button(onClick = { onAction(tech.uid, true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Approve") }
                }
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
