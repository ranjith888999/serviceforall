package com.srr.myapplication.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationName(context: Context): String = withContext(Dispatchers.IO) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return@withContext "GPS Disabled"
        }
        
        try {
            // Try to get current location first
            var location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            
            // Fallback to last known location if current is null
            if (location == null) {
                location = fusedLocationClient.lastLocation.await()
            }
            
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                // Use blocking call inside withContext(Dispatchers.IO)
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: ""
                    val subLocality = address.subLocality ?: address.thoroughfare ?: ""
                    if (subLocality.isNotEmpty() && city.isNotEmpty()) {
                        "$subLocality, $city"
                    } else if (city.isNotEmpty()) {
                        city
                    } else {
                        address.getAddressLine(0) ?: "Unknown Location"
                    }
                } else {
                    "Location Name Not Found"
                }
            } else {
                "Unable to get GPS"
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}
