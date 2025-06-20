package com.pejic.campmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        fetchUser()
    }

    private fun fetchUser() {
        viewModelScope.launch {
            auth.currentUser?.let { firebaseUser ->
                try {
                    val snapshot = db.collection("users").document(firebaseUser.uid).get().await()
                    val user = snapshot.toObject(User::class.java) ?: User(
                        email = firebaseUser.email ?: ""
                    )
                    _user.value = user
                } catch (e: Exception) {
                    _user.value = User(email = firebaseUser.email ?: "")
                }
            }
        }
    }

    fun updateUserProfile(
        firstname: String,
        lastname: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            auth.currentUser?.let { firebaseUser ->
                try {
                    val updatedUser = User(
                        firstname = firstname,
                        lastname = lastname,
                        email = firebaseUser.email ?: ""
                    )
                    db.collection("users").document(firebaseUser.uid).set(updatedUser).await()
                    _user.value = updatedUser
                    onSuccess()
                } catch (e: Exception) {
                    onFailure(e)
                }
            } ?: onFailure(Exception("No user logged in"))
        }
    }

    fun changePassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            auth.currentUser?.let { firebaseUser ->
                try {
                    if (newPassword.length < 8) {
                        throw Exception("Password must be at least 8 characters long")
                    }
                    firebaseUser.updatePassword(newPassword).await()
                    onSuccess()
                } catch (e: Exception) {
                    onFailure(e)
                }
            } ?: onFailure(Exception("No user logged in"))
        }
    }
}