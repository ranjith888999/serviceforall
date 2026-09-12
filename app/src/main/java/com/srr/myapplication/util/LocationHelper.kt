package com.srr.myapplication.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

data class LocationResult(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult? = withContext(Dispatchers.IO) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return@withContext null
        }
        
        try {
            var location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            
            if (location == null) {
                location = fusedLocationClient.lastLocation.await()
            }
            
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val name = if (!addresses.isNullOrEmpty()) {
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
                LocationResult(name, location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Legacy method for compatibility or specific name-only needs
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationName(context: Context): String {
        return getCurrentLocation(context)?.name ?: "Location not found"
    }
    
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // In km
    }
}
