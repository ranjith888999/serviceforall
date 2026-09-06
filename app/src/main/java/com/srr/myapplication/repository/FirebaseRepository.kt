package com.srr.myapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.srr.myapplication.model.Product
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.util.DummyData
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun saveUser(user: User) {
        // If auth.currentUser is null (e.g., during UI testing without real Phone Auth),
        // we use the user's phone number as the document ID to ensure data is saved.
        val docId = if (auth.currentUser?.uid != null) auth.currentUser?.uid!! else user.phone
        if (docId.isEmpty()) return
        firestore.collection("users").document(docId).set(user).await()
    }

    suspend fun getUser(uid: String): User? {
        return try {
            // First try to get by UID
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(User::class.java)
            } else {
                // Fallback: try to find a document where the ID is the phone number (for mock login cases)
                // This handles cases where uid passed was actually a phone number
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByPhone(phone: String): User? {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .await()
            if (!query.isEmpty) {
                query.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteUserByPhone(phone: String) {
        try {
            val query = firestore.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .await()
            for (document in query.documents) {
                firestore.collection("users").document(document.id).delete().await()
            }
        } catch (e: Exception) {
            // Handle or log error
        }
    }

    suspend fun saveQuotationRequest(request: QuotationRequest) {
        val id = firestore.collection("quotations").document().id
        val finalRequest = request.copy(id = id)
        firestore.collection("quotations").document(id).set(finalRequest).await()
    }

    suspend fun getProducts(): List<Product> {
        // For now, return dummy data as we are using it for subcategories
        return DummyData.allProducts
    }

    // Admin Methods
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingTechnicians(): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "Technician")
                .whereEqualTo("isApproved", false)
                .whereEqualTo("isPending", true)
                .get()
                .await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateApprovalStatus(uid: String, approved: Boolean) {
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "isApproved" to approved,
                    "isPending" to false
                )
            ).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    // Quotation Methods
    suspend fun getAllQuotations(): List<QuotationRequest> {
        return try {
            val snapshot = firestore.collection("quotations")
                .orderBy("timestamp")
                .get()
                .await()
            snapshot.toObjects(QuotationRequest::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun assignTechnician(quotationId: String, tech: User) {
        try {
            firestore.collection("quotations").document(quotationId).update(
                mapOf(
                    "assignedTechnicianId" to tech.uid,
                    "assignedTechnicianName" to tech.name,
                    "status" to "Assigned"
                )
            ).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun updateQuotationStatus(quotationId: String, status: String, comments: String? = null, price: Double? = null) {
        val updates = mutableMapOf<String, Any>("status" to status)
        if (comments != null) updates["technicianComments"] = comments
        if (price != null) updates["finalPrice"] = price
        
        try {
            firestore.collection("quotations").document(quotationId).update(updates).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    // Product Management
    suspend fun addOrUpdateProduct(product: Product) {
        val id = if (product.id.isEmpty()) firestore.collection("products").document().id else product.id
        val finalProduct = product.copy(id = id)
        firestore.collection("products").document(id).set(finalProduct).await()
    }
}
