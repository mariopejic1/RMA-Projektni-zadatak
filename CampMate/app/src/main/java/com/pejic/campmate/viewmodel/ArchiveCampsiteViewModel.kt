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

class ArchiveCampsiteViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userCampsites = MutableStateFlow<List<Campsite>>(emptyList())
    val userCampsites: StateFlow<List<Campsite>> = _userCampsites

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchUserCampsites() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = auth.currentUser?.uid
                Log.d("ArchiveCampsiteVM", "Current user UID: $userId")
                if (userId == null) {
                    Log.e("ArchiveCampsiteVM", "User not logged in")
                    _error.value = "Please log in to view your campsites"
                    _userCampsites.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val snapshot = db.collection("campsites")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
                Log.d("ArchiveCampsiteVM", "Query returned ${snapshot.size()} documents")

                snapshot.documents.forEach { doc ->
                    val docCreatorId = doc.getString("creatorId")
                    Log.d("ArchiveCampsiteVM", "Document ${doc.id}: creatorId=$docCreatorId, data=${doc.data}")
                }

                val campsites = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Campsite::class.java)?.copy(id = doc.id).also { campsite ->
                            Log.d("ArchiveCampsiteVM", "Parsed campsite: ${campsite?.id}, name=${campsite?.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("ArchiveCampsiteVM", "Error parsing document ${doc.id}: ${e.message}", e)
                        null
                    }
                }

                _userCampsites.value = campsites
                Log.d("ArchiveCampsiteVM", "Fetched ${campsites.size} campsites for user $userId")
                if (campsites.isEmpty()) {
                    _error.value = "No campsites found for this user"
                }
            } catch (e: Exception) {
                Log.e("ArchiveCampsiteVM", "Error fetching user campsites: ${e.message}", e)
                _error.value = "Failed to load campsites: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}