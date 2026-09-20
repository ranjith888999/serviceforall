package com.srr.myapplication.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "Customer", // Customer, Technician, Admin
    val address: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locations: List<String> = emptyList(), // Up to 3 locations
    val locationCoords: List<Map<String, Double>> = emptyList(), // List of {lat, lng}
    val selectedLocationIndex: Int = 0,
    val aadharNumber: String = "",
    val panNumber: String = "",
    val expertise: List<String> = emptyList(), // For Technicians (CCTV, Networking, etc.)
    val registrationComplete: Boolean = false,
    
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false,
    
    @get:PropertyName("isPending")
    @set:PropertyName("isPending")
    var isPending: Boolean = true
)

data class Category(
    val id: String,
    val name: String,
    val iconRes: Int? = null,
    val subCategories: List<SubCategory>
)

data class SubCategory(
    val id: String,
    val name: String,
    val parentCategoryId: String
)

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val subCategoryId: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0 // Added back for Cart compatibility if needed
)

data class CartItem(
    val product: Product = Product(),
    var quantity: Int = 1
)

data class QuotationRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val productId: String = "",
    val productName: String = "",
    val serviceType: String = "", // Sales, Installation, Service, AMC, Site Visit
    val comments: String = "",
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending", // Pending, Assigned, In Progress, Completed, Cancelled
    val assignedTechnicianId: String? = null,
    val assignedTechnicianName: String? = null,
    val technicianComments: String? = null,
    val finalPrice: Double? = null
)

data class AppLog(
    val id: String = "",
    val userId: String? = null,
    val userName: String? = null,
    val userRole: String? = null,
    val tag: String = "",
    val message: String = "",
    val stackTrace: String? = null,
    val deviceModel: String = "",
    val osVersion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AnalyticsEvent(
    val id: String = "",
    val type: String = "", // REGISTRATION, QUOTATION_CREATED, SERVICE_COMPLETED, APP_OPEN
    val userId: String? = null,
    val metadata: Map<String, String> = emptyList<Pair<String, String>>().toMap(),
    val timestamp: Long = System.currentTimeMillis()
)
