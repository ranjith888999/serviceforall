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
    val aadharNumber: String = "",
    val panNumber: String = "",
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
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending", // Pending, Assigned, In Progress, Completed, Cancelled
    val assignedTechnicianId: String? = null,
    val assignedTechnicianName: String? = null,
    val technicianComments: String? = null,
    val finalPrice: Double? = null
)
