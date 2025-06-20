package com.pejic.campmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pejic.campmate.model.Campsite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class AllCampsitesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
    val campsites: StateFlow<List<Campsite>> = _campsites

    private val _userCampsites = MutableStateFlow<List<Campsite>>(emptyList())
    val userCampsites: StateFlow<List<Campsite>> = _userCampsites

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var lastVisibleCampsite: String? = null
    private var lastVisibleUserCampsite: String? = null
    private val pageSize = 10L

    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .setPersistenceEnabled(true)
            .build()
    }

    fun fetchAllCampsites(
        searchQuery: String = "",
        minPrice: Float = 0f,
        maxPrice: Float = Float.MAX_VALUE,
        showAvailableOnly: Boolean = false,
        type: String? = null,
        reset: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                if (reset) {
                    lastVisibleCampsite = null
                    _campsites.value = emptyList()
                }

                var query = db.collection("campsites")
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(pageSize)

                lastVisibleCampsite?.let { lastId ->
                    query = query.startAfter(lastId)
                }

                val querySnapshot = query.get().await()
                val campsiteList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Campsite::class.java)?.copy(id = doc.id)
                }.filter { campsite ->
                    val matchesSearch = campsite.name.contains(searchQuery, ignoreCase = true) ||
                            campsite.location.contains(searchQuery, ignoreCase = true)
                    val price = campsite.price.toFloatOrNull() ?: 0f
                    val matchesPrice = price in minPrice..maxPrice
                    val matchesAvailability = !showAvailableOnly || campsite.availability
                    val matchesType = type == null || campsite.type == type
                    matchesSearch && matchesPrice && matchesAvailability && matchesType
                }

                lastVisibleCampsite = querySnapshot.documents.lastOrNull()?.id
                _campsites.value = _campsites.value + campsiteList
                _errorMessage.value = null
                Log.d("Firestore", "Fetched ${campsiteList.size} campsites, total: ${_campsites.value.size}")
            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching campsites: ${e.message}", e)
                _errorMessage.value = "Failed to load campsites. Please try again."
            }
        }
    }

    fun fetchUserCampsites(reset: Boolean = false) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                if (reset) {
                    lastVisibleUserCampsite = null
                    _userCampsites.value = emptyList()
                }

                var query = db.collection("campsites")
                    .whereEqualTo("creatorId", userId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(pageSize)

                lastVisibleUserCampsite?.let { lastId ->
                    query = query.startAfter(lastId)
                }

                val querySnapshot = query.get().await()
                val campsiteList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Campsite::class.java)?.copy(id = doc.id)
                }

                lastVisibleUserCampsite = querySnapshot.documents.lastOrNull()?.id
                _userCampsites.value = _userCampsites.value + campsiteList
                _errorMessage.value = null
                Log.d("Firestore", "Fetched ${campsiteList.size} user campsites, total: ${_userCampsites.value.size}")
            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching user campsites: ${e.message}", e)
                _errorMessage.value = "Failed to load your campsites. Please try again."
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}