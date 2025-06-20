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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageViewModel : ViewModel() {

    private val _mainImagesBitmaps = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
    val mainImagesBitmaps: StateFlow<Map<String, Bitmap?>> = _mainImagesBitmaps

    private val _otherImagesBitmaps = MutableStateFlow<Map<String, List<Bitmap>>>(emptyMap())
    val otherImagesBitmaps: StateFlow<Map<String, List<Bitmap>>> = _otherImagesBitmaps

    private var capturedImageUri: Uri? = null

    private val _isLoadingImages = MutableStateFlow(false)
    val isLoadingImages: StateFlow<Boolean> = _isLoadingImages

    fun updateMainImageUri(campsiteId: String, uri: Uri?, context: Context, onBase64Ready: (String?) -> Unit) {
        uri?.let {
            viewModelScope.launch {
                _isLoadingImages.value = true
                val base64 = compressAndConvertToBase64(context, it)
                if (base64 != null) {
                    Log.d("ImageVM", "Main image Base64 updated for campsiteId: $campsiteId, length: ${base64.length} chars")
                    decodeMainImageBase64(campsiteId, base64)
                    onBase64Ready(base64)
                } else {
                    Log.e("ImageVM", "Failed to convert main image URI to Base64: $uri for campsiteId: $campsiteId")
                    removeMainImage(campsiteId)
                    onBase64Ready(null)
                }
                _isLoadingImages.value = false
            }
        } ?: run {
            removeMainImage(campsiteId)
            onBase64Ready(null)
        }
    }

    fun decodeMainImageBase64(campsiteId: String, base64String: String?) {
        viewModelScope.launch {
            _isLoadingImages.value = true
            base64String?.let {
                val bitmap = decodeBase64ToBitmap(it)
                _mainImagesBitmaps.value = _mainImagesBitmaps.value.toMutableMap().apply {
                    put(campsiteId, bitmap)
                }
                Log.d("ImageVM", "Main image decoded for campsiteId: $campsiteId, bitmap: ${bitmap != null}")
            } ?: removeMainImage(campsiteId)
            _isLoadingImages.value = false
        }
    }

    fun removeMainImage(campsiteId: String) {
        _mainImagesBitmaps.value = _mainImagesBitmaps.value.toMutableMap().apply {
            put(campsiteId, null)
        }
        Log.d("ImageVM", "Main image removed for campsiteId: $campsiteId")
    }

    fun addOtherImageUri(campsiteId: String, uri: Uri, context: Context, onBase64Ready: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoadingImages.value = true
            val base64 = compressAndConvertToBase64(context, uri)
            if (base64 != null) {
                Log.d("ImageVM", "Other image Base64 added for campsiteId: $campsiteId, length: ${base64.length} chars")
                val bitmap = decodeBase64ToBitmap(base64)
                bitmap?.let {
                    val currentList = _otherImagesBitmaps.value[campsiteId]?.toMutableList() ?: mutableListOf()
                    currentList.add(it)
                    _otherImagesBitmaps.value = _otherImagesBitmaps.value.toMutableMap().apply {
                        put(campsiteId, currentList)
                    }
                }
                onBase64Ready(base64)
            } else {
                Log.e("ImageVM", "Failed to convert other image URI to Base64: $uri for campsiteId: $campsiteId")
                onBase64Ready(null)
            }
            _isLoadingImages.value = false
        }
    }

    suspend fun decodeBase64ToBitmap(base64String: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val cleanBase64 = base64String.substringAfter("base64,", base64String).trim()
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("ImageVM", "Error decoding Base64: ${e.message}")
            null
        }
    }

    fun removeOtherImage(campsiteId: String, index: Int) {
        val currentBitmaps = _otherImagesBitmaps.value[campsiteId]?.toMutableList() ?: return
        if (index in currentBitmaps.indices) {
            currentBitmaps.removeAt(index)
            _otherImagesBitmaps.value = _otherImagesBitmaps.value.toMutableMap().apply {
                put(campsiteId, currentBitmaps)
            }
            Log.d("ImageVM", "Removed other image at index $index for campsiteId: $campsiteId")
        } else {
            Log.w("ImageVM", "Index out of range when removing other image for campsiteId: $campsiteId")
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
            Log.d("ImageVM", "Image file created: $capturedImageUri")
            capturedImageUri
        } catch (e: Exception) {
            Log.e("ImageVM", "Error creating image file: ${e.message}", e)
            null
        }
    }

    fun getCapturedImageUri(): Uri? = capturedImageUri

    fun checkPermissions(context: Context): List<String> {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }
        return permissionsToRequest
    }

    fun updatePermissions(
        permissions: Map<String, Boolean>,
        hasLocationPermission: (Boolean) -> Unit,
        hasCameraPermission: (Boolean) -> Unit
    ) {
        hasLocationPermission(permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true)
        hasCameraPermission(permissions[android.Manifest.permission.CAMERA] == true)
    }

    suspend fun compressAndConvertToBase64(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inSampleSize = 2 // Smanji rezoluciju na pola
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    if (bitmap == null) {
                        Log.e("ImageVM", "Failed to decode bitmap from URI: $uri")
                        return@withContext null
                    }
                    val resizedBitmap = resizeBitmap(bitmap, 550)
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    if (byteArray.isEmpty()) {
                        Log.e("ImageVM", "Compressed byte array is empty for URI: $uri")
                        return@withContext null
                    }
                    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    Log.d("ImageVM", "Compressed image size: ${byteArray.size / 1024} KB, Base64 length: ${base64.length}")
                    try {
                        Base64.decode(base64, Base64.DEFAULT)
                        "data:image/jpeg;base64,$base64"
                    } catch (e: IllegalArgumentException) {
                        Log.e("ImageVM", "Invalid Base64 string generated for URI: $uri", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageVM", "Error compressing/converting URI to Base64: ${e.message}", e)
                null
            }
        }
    }

    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
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

    fun setOtherImages(campsiteId: String, bitmaps: List<Bitmap>) {
        _otherImagesBitmaps.value = _otherImagesBitmaps.value.toMutableMap().apply {
            put(campsiteId, bitmaps)
        }
    }

    fun clearImagesForCampsite(campsiteId: String) {
        _mainImagesBitmaps.value = _mainImagesBitmaps.value.toMutableMap().apply {
            remove(campsiteId)
        }
        _otherImagesBitmaps.value = _otherImagesBitmaps.value.toMutableMap().apply {
            remove(campsiteId)
        }
        Log.d("ImageVM", "Cleared images for campsiteId: $campsiteId")
    }

    fun clearAllImages() {
        _mainImagesBitmaps.value = emptyMap()
        _otherImagesBitmaps.value = emptyMap()
        capturedImageUri = null
        Log.d("ImageVM", "Cleared all images")
    }
}