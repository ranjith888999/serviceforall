package com.srr.myapplication.util

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.srr.myapplication.model.AnalyticsEvent
import com.srr.myapplication.model.AppLog
import com.srr.myapplication.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

object AnalyticsHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logError(tag: String, message: String, exception: Throwable? = null) {
        val uid = auth.currentUser?.uid
        val stackTrace = exception?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        scope.launch {
            try {
                // Get user basic info if possible
                var userName: String? = null
                var userRole: String? = null
                
                if (uid != null) {
                    val userDoc = firestore.collection("users").document(uid).get().result
                    if (userDoc != null && userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        userName = user?.name
                        userRole = user?.role
                    }
                }

                val log = AppLog(
                    id = firestore.collection("app_logs").document().id,
                    userId = uid,
                    userName = userName,
                    userRole = userRole,
                    tag = tag,
                    message = message,
                    stackTrace = stackTrace,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    timestamp = System.currentTimeMillis()
                )
                
                firestore.collection("app_logs").document(log.id).set(log)
            } catch (e: Exception) {
                Log.e("AnalyticsHelper", "Failed to upload log", e)
            }
        }
    }

    fun trackEvent(type: String, metadata: Map<String, String> = emptyMap()) {
        val uid = auth.currentUser?.uid
        scope.launch {
            try {
                val event = AnalyticsEvent(
                    id = firestore.collection("analytics").document().id,
                    type = type,
                    userId = uid,
                    metadata = metadata,
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("analytics").document(event.id).set(event)
            } catch (e: Exception) {
                Log.e("AnalyticsHelper", "Failed to track event", e)
            }
        }
    }

    object EventType {
        const val APP_OPEN = "APP_OPEN"
        const val REGISTRATION_START = "REGISTRATION_START"
        const val REGISTRATION_COMPLETE = "REGISTRATION_COMPLETE"
        const val QUOTATION_CREATED = "QUOTATION_CREATED"
        const val SERVICE_COMPLETED = "SERVICE_COMPLETED"
        const val LOGIN_SUCCESS = "LOGIN_SUCCESS"
        const val LOGOUT = "LOGOUT"
    }
}
