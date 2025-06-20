package com.pejic.campmate.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.model.Campsite
import com.pejic.campmate.model.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class LocationTrackingViewModel(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state

    private val locationManager = try {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    } catch (e: Exception) {
        Log.e("LocationTrackingViewModel", "Failed to get LocationManager", e)
        null
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("LocationTrackingViewModel", "Location updated: ${location.latitude}, ${location.longitude}")
            updateLocation(location.latitude, location.longitude)
            locationManager?.removeUpdates(this)
        }

        override fun onProviderEnabled(provider: String) {
            Log.d("LocationTrackingViewModel", "Provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d("LocationTrackingViewModel", "Provider disabled: $provider")
            _state.value = _state.value.copy(error = "GPS is disabled. Please enable it.")
        }
    }

    init {
        fetchCampsites()
    }

    @SuppressLint("MissingPermission")
    fun requestSingleLocation() {
        if (!_state.value.permissionGranted) {
            Log.d("LocationTrackingViewModel", "Permissions not granted")
            _state.value = _state.value.copy(error = "Location permissions required")
            return
        }

        if (locationManager == null) {
            Log.e("LocationTrackingViewModel", "LocationManager is null")
            _state.value = _state.value.copy(error = "Location service unavailable")
            return
        }

        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!isGpsEnabled) {
                Log.d("LocationTrackingViewModel", "GPS is disabled")
                _state.value = _state.value.copy(error = "GPS is disabled. Please enable it.")
                return
            }

            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                Log.d("LocationTrackingViewModel", "Using last known location: ${location.latitude}, ${location.longitude}")
                updateLocation(location.latitude, location.longitude)
                return
            }

            locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                locationListener,
                Looper.getMainLooper()
            )
            Log.d("LocationTrackingViewModel", "Requested single location update")
        } catch (e: SecurityException) {
            Log.e("LocationTrackingViewModel", "Security exception", e)
            _state.value = _state.value.copy(error = "Location permission denied")
        } catch (e: Exception) {
            Log.e("LocationTrackingViewModel", "Error requesting location", e)
            _state.value = _state.value.copy(error = "Failed to get location")
        }
    }

    private fun updateLocation(lat: Double, lon: Double) {
        _state.value = _state.value.copy(
            currentLocation = Pair(lat, lon),
            error = null
        )
        updateNearestCampsite(lat, lon)
    }

    fun setPermissionGranted(granted: Boolean) {
        Log.d("LocationTrackingViewModel", "Permission granted: $granted")
        _state.value = _state.value.copy(
            permissionGranted = granted,
            error = if (!granted) "Location permissions required" else null
        )
    }

    private fun fetchCampsites() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("campsites").get().await()
                val campsites = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Campsite::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("LocationTrackingViewModel", "Error parsing campsite: ${doc.id}", e)
                        null
                    }
                }
                Log.d("LocationTrackingViewModel", "Fetched ${campsites.size} campsites")
                _state.value = _state.value.copy(campsites = campsites)
                _state.value.currentLocation?.let { (lat, lon) ->
                    updateNearestCampsite(lat, lon)
                }
            } catch (e: Exception) {
                Log.e("LocationTrackingViewModel", "Error fetching campsites: ${e.message}")
                _state.value = _state.value.copy(error = "Failed to fetch campsites")
            }
        }
    }

    private fun updateNearestCampsite(userLat: Double, userLon: Double) {
        val validCampsites = _state.value.campsites.filter {
            it.latitude != null && it.longitude != null
        }
        if (validCampsites.isEmpty()) {
            Log.d("LocationTrackingViewModel", "No campsites with valid coordinates")
            _state.value = _state.value.copy(
                nearestCampsite = null,
                nearestCampsiteDistance = null,
                targetLocation = null
            )
            return
        }

        val nearest = validCampsites.minByOrNull { campsite ->
            calculateDistance(
                userLat, userLon,
                campsite.latitude!!, campsite.longitude!!
            )
        }
        nearest?.let {
            val distance = calculateDistance(
                userLat, userLon,
                it.latitude!!, it.longitude!!
            )
            Log.d("LocationTrackingViewModel", "Nearest campsite: ${it.name}, Address: ${it.location}, Distance: $distance km")
            _state.value = _state.value.copy(
                nearestCampsite = it,
                nearestCampsiteDistance = distance,
                targetLocation = Pair(it.latitude!!, it.longitude!!)
            )
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    override fun onCleared() {
        locationManager?.removeUpdates(locationListener)
        super.onCleared()
    }
}
