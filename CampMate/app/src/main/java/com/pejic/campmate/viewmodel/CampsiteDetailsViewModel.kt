package com.pejic.campmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.model.Campsite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class CampsiteDetailsViewModel(
    private val context: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val notificationViewModel = NotificationViewModel(context)

    private val _campsite = MutableStateFlow<Campsite?>(null)
    val campsite: StateFlow<Campsite?> = _campsite

    private val _currentUserId = MutableStateFlow<String?>(auth.currentUser?.uid)
    val currentUserId: StateFlow<String?> = _currentUserId

    init {
        auth.addAuthStateListener {
            _currentUserId.value = it.currentUser?.uid
        }
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchCampsite(campsiteId: String) {
        if (_campsite.value?.id == campsiteId) {
            Log.d("CampsiteDetailsVM", "Skipping fetch, already loaded: $campsiteId")
            return
        }
        viewModelScope.launch {
            try {
                Log.d("CampsiteDetailsVM", "Fetching campsite: $campsiteId")
                val document = db.collection("campsites").document(campsiteId).get().await()
                val campsite = document.toObject(Campsite::class.java)?.copy(id = document.id)
                _campsite.value = campsite
                _error.value = if (campsite == null) "Campsite not found" else null
            } catch (e: Exception) {
                Log.e("CampsiteDetailsVM", "Error fetching campsite: ${e.message}", e)
                _error.value = e.message
            }
        }
    }

    fun rateCampsite(
        campsiteId: String,
        rating: Double,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

                val userSnapshot = db.collection("users").document(userId).get().await()
                val firstname = userSnapshot.getString("firstname") ?: "Nepoznato"
                val lastname = userSnapshot.getString("lastname") ?: "Nepoznato"

                db.collection("campsites").document(campsiteId)
                    .update("ratings.$userId", rating)
                    .await()

                val campsiteSnapshot = db.collection("campsites").document(campsiteId).get().await()
                val campsite = campsiteSnapshot.toObject(Campsite::class.java)
                    ?: throw Exception("Campsite not found")

                if (campsite.creatorId != userId) {
                    notificationViewModel.sendLocalNotification(
                        context = context,
                        creatorId = campsite.creatorId,
                        raterFirstName = firstname,
                        raterLastName = lastname,
                        campsiteName = campsite.name,
                        rating = rating
                    )
                }

                Log.d("Firestore", "Rating submitted successfully for campsite: $campsiteId")
                onSuccess()
            } catch (e: Exception) {
                Log.e("Firestore", "Error rating campsite: ${e.message}", e)
                onFailure(e)
            }
        }
    }

    fun deleteCampsite(
        campsiteId: String,
        creatorId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                if (userId != creatorId) {
                    throw Exception("Only the creator can delete this campsite")
                }
                db.collection("campsites").document(campsiteId).delete().await()
                Log.d("Firestore", "Campsite deleted successfully: $campsiteId")
                onSuccess()
            } catch (e: Exception) {
                Log.e("Firestore", "Error deleting campsite: ${e.message}", e)
                onFailure(e)
            }
        }
    }
}