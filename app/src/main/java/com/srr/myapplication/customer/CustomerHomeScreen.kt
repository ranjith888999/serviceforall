package com.srr.myapplication.customer

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.srr.myapplication.R
import com.srr.myapplication.model.Product
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.navigation.Screen
import com.srr.myapplication.repository.FirebaseRepository
import com.srr.myapplication.util.DummyData
import com.srr.myapplication.util.LocationHelper
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(navController: NavHostController, uid: String) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var userData by remember { mutableStateOf<User?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var myRequests by remember { mutableStateOf<List<QuotationRequest>>(emptyList()) }

    LaunchedEffect(uid) {
        userData = repository.getUser(uid)
        allProducts = repository.getProducts()
        isLoading = false
    }

    LaunchedEffect(showRequestsDialog) {
        if (showRequestsDialog) {
            myRequests = repository.getAllQuotations().filter { it.userId == uid }
        }
    }

    val filteredProducts = remember(searchQuery, selectedCategoryId, allProducts) {
        allProducts.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
            matchesSearch && matchesCategory
        }
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = DummyData.categories,
            selectedCategoryId = selectedCategoryId,
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { id ->
                selectedCategoryId = id
                showCategoryDialog = false
            }
        )
    }

    if (showRequestsDialog) {
        RequestsHistoryDialog(
            requests = myRequests,
            onDismiss = { showRequestsDialog = false }
        )
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

    Scaffold(
        bottomBar = { 
            ModernBottomNavigation(
                navController = navController,
                onCategoriesClick = { showCategoryDialog = true },
                onRequestsClick = { showRequestsDialog = true }
            ) 
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F8F1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Search Section
                TopSearchSection(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    currentLocation = userData?.let { 
                        if (it.locations.isNotEmpty()) it.locations.getOrNull(it.selectedLocationIndex) ?: "Select Location"
                        else it.location.ifEmpty { "Select Location" }
                    } ?: "Loading...",
                    onLocationClick = { showLocationDialog = true },
                    onLogout = {
                        auth.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Category Tab Bar
                ScrollableCategoryTabs(
                    categories = DummyData.categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { id -> selectedCategoryId = id }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // "Most Requested Services" Section
                SectionHeader(title = if (selectedCategoryId == null) "All Services" else "Recommended Services")
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ProductGrid(filteredProducts, navController)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TopSearchSection(
    query: String, 
    onQueryChange: (String) -> Unit, 
    currentLocation: String,
    onLocationClick: () -> Unit,
    onLogout: () -> Unit, 
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLocationClick() },
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
                        Text(
                            text = currentLocation,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap to change location",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
                Row {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                placeholder = { Text("Search for \"CCTV\"", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF1F3F4),
                    unfocusedContainerColor = Color(0xFFF1F3F4)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ScrollableCategoryTabs(
    categories: List<com.srr.myapplication.model.Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            CategoryTab(
                text = "All Services",
                icon = Icons.Default.GridView,
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories) { category ->
            CategoryTab(
                text = category.name,
                icon = getCategoryIcon(category.id),
                isSelected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun CategoryTab(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) Color.White else Color.Gray,
        label = "contentColor"
    )
    val elevation by animateDpAsState(
        if (isSelected) 4.dp else 1.dp,
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shadowElevation = elevation,
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            )
        }
    }
}

private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "cat_cctv" -> Icons.Default.Videocam
        "cat_epabx" -> Icons.Default.Phone
        "cat_biometric" -> Icons.Default.Fingerprint
        "cat_networking" -> Icons.Default.Router
        "cat_cables" -> Icons.Default.SettingsInputComponent
        "cat_it" -> Icons.Default.Computer
        "cat_printers" -> Icons.Default.Print
        "cat_wlc" -> Icons.Default.WaterDrop
        else -> Icons.Default.Category
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "View all",
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun ProductGrid(products: List<Product>, navController: NavHostController) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        val rows = products.chunked(2)
        rows.forEach { rowProducts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowProducts.forEach { product ->
                    ServiceCard(
                        product = product,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate(Screen.ProductDetails.createRoute(product.id))
                        }
                    )
                }
                if (rowProducts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = R.drawable.defaultimage,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Text(
                    text = "Professional Quality",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View Details", style = MaterialTheme.typography.labelMedium)
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
        title = { Text("Select Location") },
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

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
fun CategorySelectionDialog(
    categories: List<com.srr.myapplication.model.Category>,
    selectedCategoryId: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text("All Services") },
                    leadingContent = { Icon(Icons.Default.GridView, contentDescription = null) },
                    modifier = Modifier.clickable { onCategorySelected(null) },
                    trailingContent = { if (selectedCategoryId == null) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                categories.forEach { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        leadingContent = { Icon(getCategoryIcon(category.id), contentDescription = null) },
                        modifier = Modifier.clickable { onCategorySelected(category.id) },
                        trailingContent = { if (selectedCategoryId == category.id) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RequestsHistoryDialog(
    requests: List<QuotationRequest>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My Service Requests") },
        text = {
            if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No requests found", color = Color.Gray)
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    requests.forEach { request ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = request.productName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = getStatusColor(request.status),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = request.status,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(text = "Service: ${request.serviceType}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(request.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "Pending" -> Color(0xFFFFA500)
        "Assigned", "In Progress" -> Color(0xFF2196F3)
        "Completed" -> Color(0xFF4CAF50)
        "Cancelled" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

@Composable
fun ModernBottomNavigation(
    navController: NavHostController,
    onCategoriesClick: () -> Unit,
    onRequestsClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = true,
            onClick = { /* Already here */ }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
            label = { Text("Categories") },
            selected = false,
            onClick = onCategoriesClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text("Requests") },
            selected = false,
            onClick = onRequestsClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = false,
            onClick = { navController.navigate(Screen.Profile.route) }
        )
    }
}
