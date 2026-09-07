package com.srr.myapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.srr.myapplication.model.Product
import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import com.srr.myapplication.util.DummyData
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun saveUser(user: User) {
        if (user.uid.isEmpty()) return
        // Use SetOptions.merge() to ensure we don't overwrite existing fields if we only pass a partial object,
        // but here we pass the whole object. It's safer for updates.
        firestore.collection("users").document(user.uid).set(user, SetOptions.merge()).await()
    }

    suspend fun getUser(uid: String): User? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                user?.copy(uid = snapshot.id) // Ensure UID matches document ID
            } else {
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
                val doc = query.documents[0]
                val user = doc.toObject(User::class.java)
                user?.copy(uid = doc.id)
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
        }
    }

    suspend fun deleteUser(uid: String) {
        try {
            firestore.collection("users").document(uid).delete().await()
        } catch (e: Exception) {}
    }

    suspend fun saveQuotationRequest(request: QuotationRequest) {
        val id = firestore.collection("quotations").document().id
        val finalRequest = request.copy(id = id)
        firestore.collection("quotations").document(id).set(finalRequest).await()
    }

    suspend fun getProducts(): List<Product> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            if (snapshot.isEmpty) {
                val dummy = DummyData.allProducts
                dummy.forEach { addOrUpdateProduct(it) }
                dummy
            } else {
                snapshot.toObjects(Product::class.java)
            }
        } catch (e: Exception) {
            DummyData.allProducts
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingTechnicians(): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "Technician")
                .whereEqualTo("isApproved", false)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateApprovalStatus(uid: String, approved: Boolean) {
        if (uid.isEmpty()) return
        try {
            // Using set with merge is more robust than update
            firestore.collection("users").document(uid).set(
                mapOf(
                    "isApproved" to approved,
                    "isPending" to false
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getAllQuotations(): List<QuotationRequest> {
        return try {
            val snapshot = firestore.collection("quotations")
                .get()
                .await()
            snapshot.toObjects(QuotationRequest::class.java).sortedByDescending { it.timestamp }
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
            throw e
        }
    }

    suspend fun updateQuotationStatus(quotationId: String, status: String, comments: String? = null, price: Double? = null) {
        val updates = mutableMapOf<String, Any>("status" to status)
        if (comments != null) updates["technicianComments"] = comments
        if (price != null) updates["finalPrice"] = price
        
        try {
            firestore.collection("quotations").document(quotationId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun addOrUpdateProduct(product: Product) {
        val id = if (product.id.isEmpty()) firestore.collection("products").document().id else product.id
        val finalProduct = product.copy(id = id)
        firestore.collection("products").document(id).set(finalProduct, SetOptions.merge()).await()
    }

    suspend fun deleteProduct(productId: String) {
        try {
            firestore.collection("products").document(productId).delete().await()
        } catch (e: Exception) {}
    }
}
