package com.pejic.campmate.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.R
import com.pejic.campmate.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AuthenticationViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authStatus = MutableStateFlow<Int?>(null)
    val authStatus: StateFlow<Int?> = _authStatus

    fun registerUser(
        firstname: String,
        lastname: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit = {}
    ) {
        _authStatus.value = null
        if (firstname.isBlank() || lastname.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _authStatus.value = R.string.error_empty_fields
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authStatus.value = R.string.error_invalid_email
            return
        }
        if (password != confirmPassword) {
            _authStatus.value = R.string.error_password_mismatch
            return
        }
        if (password.length < 8) {
            _authStatus.value = R.string.error_short_password
            return
        }
        viewModelScope.launch {
            try {
                val result = db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                if (!result.isEmpty) {
                    _authStatus.value = R.string.error_email_exists
                    return@launch
                }
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("User not found after registration")
                val userProfile = User(
                    firstname = firstname,
                    lastname = lastname,
                    email = email
                )
                db.collection("users").document(user.uid).set(userProfile).await()
                _authStatus.value = R.string.success_registration
                onSuccess()
            } catch (e: Exception) {
                _authStatus.value = when (e) {
                    is FirebaseAuthUserCollisionException -> R.string.error_email_already_in_use
                    else -> R.string.error_registration_failed
                }
            }
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        _authStatus.value = null
        if (email.isBlank() || password.isBlank()) {
            _authStatus.value = R.string.error_empty_email_password
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authStatus.value = R.string.error_invalid_email
            return
        }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authStatus.value = R.string.success_login
                onSuccess()
            } catch (e: Exception) {
                _authStatus.value = mapFirebaseError(e)
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authStatus.value = R.string.success_logout
    }

    fun clearStatus() {
        _authStatus.value = null
    }

    private fun mapFirebaseError(exception: Exception?): Int {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> R.string.error_incorrect_password
            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> R.string.error_user_not_found
                    "ERROR_USER_DISABLED" -> R.string.error_user_disabled
                    else -> R.string.error_invalid_user
                }
            }
            is FirebaseAuthUserCollisionException -> R.string.error_email_already_in_use
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.error_email_already_in_use
                    else -> R.string.error_authentication
                }
            }
            else -> R.string.error_unknown
        }
    }
}