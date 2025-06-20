package com.pejic.campmate.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.R
import com.pejic.campmate.model.Campsite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager

class CreateCampsiteViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _campsite = MutableStateFlow(Campsite())
    val campsite: StateFlow<Campsite> = _campsite

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private var capturedImageUri: Uri? = null

    private val _mainImageBitmap = MutableStateFlow<Bitmap?>(null)
    val mainImageBitmap: StateFlow<Bitmap?> = _mainImageBitmap

    private val _otherImagesBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val otherImagesBitmaps: StateFlow<List<Bitmap>> = _otherImagesBitmaps

    init {
        auth.addAuthStateListener {
            _campsite.value = _campsite.value.copy(creatorId = it.currentUser?.uid ?: "")
        }
    }

    fun setError(message: String?) {
        _error.value = message
    }

    fun setLocationError(message: String?) {
        _locationError.value = message
    }

    fun updateName(name: String) {
        _campsite.value = _campsite.value.copy(name = name)
    }

    fun updateLocation(location: String) {
        _campsite.value = _campsite.value.copy(location = location)
    }

    fun updatePrice(price: String) {
        _campsite.value = _campsite.value.copy(price = price)
    }

    fun updateDescription(description: String) {
        _campsite.value = _campsite.value.copy(description = description)
    }

    fun updateAvailability(available: Boolean) {
        _campsite.value = _campsite.value.copy(availability = available)
    }

    fun updateType(type: String) {
        _campsite.value = _campsite.value.copy(type = type)
    }

    fun updateSelectedLatLng(latLng: LatLng?) {
        _campsite.value = _campsite.value.copy(
            latitude = latLng?.latitude,
            longitude = latLng?.longitude
        )
    }

    fun updateCurrentLocation(latLng: LatLng) {
        _currentLocation.value = latLng
        if (_campsite.value.latitude == null) {
            updateSelectedLatLng(latLng)
        }
        _locationError.value = null
    }

    fun updateMainImageUri(uri: Uri?, context: Context) {
        uri?.let {
            viewModelScope.launch {
                val base64 = compressAndConvertToBase64(context, it)
                base64?.let { base64String ->
                    _campsite.value = _campsite.value.copy(mainImageBase64 = base64String)
                    Log.d("Image", "Main image Base64 updated: ${base64String.length} chars, starts with: ${base64String.take(30)}")
                    decodeMainImageBase64(base64String)
                } ?: run {
                    Log.e("Image", "Failed to convert main image URI to Base64: $uri")
                    _mainImageBitmap.value = null
                }
            }
        }
    }

    fun decodeMainImageBase64(base64String: String?) {
        if (base64String.isNullOrBlank()) {
            _mainImageBitmap.value = null
            return
        }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val cleanBase64 = base64String.substringAfter("base64,", base64String)
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    Log.e("Image", "Error decoding Base64 to Bitmap", e)
                    null
                }
            }
            _mainImageBitmap.value = bitmap
        }
    }

    fun removeMainImage() {
        _campsite.value = _campsite.value.copy(mainImageBase64 = null)
        _mainImageBitmap.value = null
        Log.d("Image", "Main image removed")
    }

    fun addOtherImageUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            val base64 = compressAndConvertToBase64(context, uri)
            base64?.let { base64String ->
                _campsite.value = _campsite.value.copy(
                    imageBase64s = _campsite.value.imageBase64s + base64String
                )
                Log.d("Image", "Other image Base64 added: ${base64String.length} chars")

                val bitmap = decodeBase64ToBitmap(base64String)
                bitmap?.let {
                    _otherImagesBitmaps.value = _otherImagesBitmaps.value + it
                }
            } ?: run {
                Log.e("Image", "Failed to convert other image URI to Base64: $uri")
            }
        }
    }

    private suspend fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cleanBase64 = base64String.substringAfter("base64,", base64String)
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("Image", "Error decoding Base64 to Bitmap", e)
                null
            }
        }
    }

    fun removeOtherImage(base64ToRemove: String) {
        val currentBase64s = _campsite.value.imageBase64s.toMutableList()
        val index = currentBase64s.indexOf(base64ToRemove)
        if (index == -1) {
            Log.w("Image", "Base64 string to remove not found")
            return
        }

        currentBase64s.removeAt(index)
        _campsite.value = _campsite.value.copy(imageBase64s = currentBase64s)

        val currentBitmaps = _otherImagesBitmaps.value.toMutableList()
        if (index < currentBitmaps.size) {
            currentBitmaps.removeAt(index)
            _otherImagesBitmaps.value = currentBitmaps
            Log.d("Image", "Removed other image at index $index")
        } else {
            Log.w("Image", "Bitmap index out of range when removing image")
        }
    }

    fun createImageFile(context: Context): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.cacheDir
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            capturedImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            Log.d("Image", "Image file created: $capturedImageUri")
            capturedImageUri
        } catch (e: Exception) {
            _error.value = context.getString(R.string.camera_error)
            Log.e("Image", "Error creating image file: ${e.message}", e)
            null
        }
    }

    fun getCapturedImageUri(): Uri? = capturedImageUri

    fun checkPermissions(context: Context): List<String> {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            _hasLocationPermission.value = true
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        } else {
            _hasCameraPermission.value = true
        }
        return permissionsToRequest
    }

    fun updatePermissions(permissions: Map<String, Boolean>) {
        _hasLocationPermission.value = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        _hasCameraPermission.value = permissions[android.Manifest.permission.CAMERA] == true
    }

    private suspend fun compressAndConvertToBase64(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inSampleSize = 2
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    if (bitmap == null) {
                        Log.e("Base64", "Failed to decode bitmap from URI: $uri")
                        return@withContext null
                    }
                    val resizedBitmap = resizeBitmap(bitmap, 400)
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    if (byteArray.isEmpty()) {
                        Log.e("Base64", "Compressed byte array is empty for URI: $uri")
                        return@withContext null
                    }
                    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP or Base64.NO_PADDING)
                    Log.d("Base64", "Compressed image size: ${byteArray.size / 1024} KB, Base64 length: ${base64.length}, starts with: ${base64.take(30)}")
                    if (base64.isBlank()) {
                        Log.e("Base64", "Generated Base64 string is empty for URI: $uri")
                        return@withContext null
                    }
                    try {
                        Base64.decode(base64, Base64.DEFAULT)
                        Log.d("Base64", "Base64 string is valid for URI: $uri")
                        "data:image/jpeg;base64,$base64"
                    } catch (e: IllegalArgumentException) {
                        Log.e("Base64", "Invalid Base64 string generated for URI: $uri", e)
                        null
                    }
                } ?: run {
                    Log.e("Base64", "Failed to open input stream for URI: $uri")
                    null
                }
            } catch (e: Exception) {
                Log.e("Base64", "Error compressing/converting URI to Base64: ${e.message}", e)
                null
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun estimateDocumentSize(campsite: Campsite): Long {
        var size = 0L
        size += campsite.id.length * 2
        size += campsite.creatorId.length * 2
        size += campsite.name.length * 2
        size += campsite.location.length * 2
        size += campsite.price.length * 2
        size += campsite.description.length * 2
        size += if (campsite.latitude != null) 8 else 0
        size += if (campsite.longitude != null) 8 else 0
        size += campsite.mainImageBase64?.length?.toLong() ?: 0
        size += campsite.imageBase64s.sumOf { it.length.toLong() }
        size += campsite.ratings.size * 32
        size += if (campsite.availability) 1 else 0
        size += campsite.type.length * 2
        size += 13 * 16
        Log.d("Firestore", "Estimated document size: $size bytes")
        return size
    }

    fun addCampsite(
        context: Context,
        mainImageBase64: String?,
        imageBase64s: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val campsite = _campsite.value
        if (campsite.name.isBlank() || campsite.location.isBlank() || campsite.latitude == null || mainImageBase64 == null) {
            _error.value = context.getString(R.string.error_empty_fields)
            Log.e("Firestore", "Validation failed: empty fields")
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
                val campsiteId = db.collection("campsites").document().id

                val finalCampsite = campsite.copy(
                    id = campsiteId,
                    creatorId = userId,
                    mainImageBase64 = mainImageBase64,
                    imageBase64s = imageBase64s,
                    ratings = emptyMap()
                )

                val estimatedSize = estimateDocumentSize(finalCampsite)
                if (estimatedSize > 1_000_000) {
                    _isLoading.value = false
                    _error.value = context.getString(R.string.document_too_large)
                    Log.e("Firestore", "Document size too large: $estimatedSize bytes")
                    return@launch
                }

                db.collection("campsites").document(campsiteId).set(finalCampsite).await()
                _isLoading.value = false
                _campsite.value = Campsite()
                Log.d("Firestore", "Campsite added successfully: $campsiteId")
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message
                Log.e("Firestore", "Error adding campsite: ${e.message}", e)
                onFailure(e)
            }
        }
    }
}