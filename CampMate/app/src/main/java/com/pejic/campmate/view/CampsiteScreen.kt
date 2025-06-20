package com.pejic.campmate.view

import ImageViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pejic.campmate.R
import com.pejic.campmate.Routes
import com.pejic.campmate.viewmodel.CampsiteDetailsViewModel
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteScreen(
    navController: NavController,
    campsiteViewModel: CampsiteDetailsViewModel,
    imageViewModel: ImageViewModel,
    campsiteId: String
) {
    val campsite by campsiteViewModel.campsite.collectAsState()
    val currentUserId by campsiteViewModel.currentUserId.collectAsState()
    val error by campsiteViewModel.error.collectAsState(initial = null)
    val mainImagesBitmaps by imageViewModel.mainImagesBitmaps.collectAsState()
    val otherImagesBitmaps by imageViewModel.otherImagesBitmaps.collectAsState()
    val isLoadingImages by imageViewModel.isLoadingImages.collectAsState()
    val context = LocalContext.current

    var cachedMainBitmap by remember(campsiteId) { mutableStateOf(mainImagesBitmaps[campsiteId]) }
    var cachedOtherBitmaps by remember(campsiteId) { mutableStateOf(otherImagesBitmaps[campsiteId] ?: emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var ratingInput by remember { mutableStateOf("") }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(campsiteId) {
        Log.d("CampsiteScreen", "Fetching campsiteId=$campsiteId")
        campsiteViewModel.fetchCampsite(campsiteId)
        onDispose {
            Log.d("CampsiteScreen", "Disposing campsiteId=$campsiteId")
        }
    }

    LaunchedEffect(mainImagesBitmaps, otherImagesBitmaps) {
        cachedMainBitmap = mainImagesBitmaps[campsiteId]
        cachedOtherBitmaps = otherImagesBitmaps[campsiteId] ?: emptyList()
    }

    LaunchedEffect(campsite, campsiteId) {
        if (campsite == null) {
            isLoading = true
            return@LaunchedEffect
        }
        campsite?.mainImageBase64?.let { base64 ->
            if (cachedMainBitmap == null) {
                imageViewModel.decodeMainImageBase64(campsiteId, base64)
            }
        } ?: imageViewModel.removeMainImage(campsiteId)
        campsite?.imageBase64s?.let { base64List ->
            if (cachedOtherBitmaps.isEmpty()) {
                val bitmaps = base64List.mapNotNull { base64 ->
                    imageViewModel.decodeBase64ToBitmap(base64)
                }
                imageViewModel.setOtherImages(campsiteId, bitmaps)
            }
        } ?: imageViewModel.setOtherImages(campsiteId, emptyList())
        isLoading = false
    }

    if (error != null) {
        Log.e("CampsiteScreen", "Error: $error")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = error!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    if (isLoading || isLoadingImages) {
        Log.d("CampsiteScreen", "Loading: isLoading=$isLoading, isLoadingImages=$isLoadingImages")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF14571C))
        }
        return
    }

    val camp = campsite!!
    val allImages = buildList {
        cachedMainBitmap?.let { add(it) }
        addAll(cachedOtherBitmaps)
    }
    val averageRating = if (camp.ratings.isNotEmpty()) {
        DecimalFormat("#.#").format(camp.ratings.values.average())
    } else "0.0"
    val ratingCount = camp.ratings.size

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = camp.name.takeIf { it.isNotBlank() } ?: stringResource(R.string.campsite_name),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14571C))
                )
            },
            containerColor = Color.White
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    cachedMainBitmap?.let {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedImageIndex = 0 }
                        ) {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.main_image_label),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color(0xFF14571C))
                            )
                        }
                    } ?: Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color(0xFF14571C).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_images_available),
                                color = Color(0xFF14571C),
                                fontSize = 16.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFF14571C))
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = camp.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14571C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.rating),
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$averageRating ($ratingCount)",
                                fontSize = 16.sp,
                                color = Color(0xFF14571C)
                            )
                        }
                    }
                }
                item {
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFF14571C).copy(alpha = 0.2f)
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.location_label, camp.location),
                            fontSize = 16.sp,
                            color = Color(0xFF14571C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.price_label, camp.price),
                            fontSize = 16.sp,
                            color = Color(0xFF14571C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.description_label, camp.description),
                            fontSize = 16.sp,
                            color = Color(0xFF14571C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.campsite_availability,
                                if (camp.availability) stringResource(R.string.available) else stringResource(R.string.unavailable)
                            ),
                            fontSize = 16.sp,
                            color = Color(0xFF14571C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.campsite_type, camp.type),
                            fontSize = 16.sp,
                            color = Color(0xFF14571C)
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14571C).copy(alpha = 0.1f))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                                    LatLng(camp.latitude ?: 45.5511, camp.longitude ?: 18.6939),
                                    12f
                                )
                            }
                        ) {
                            camp.latitude?.let { lat ->
                                camp.longitude?.let { lng ->
                                    Marker(
                                        state = MarkerState(position = LatLng(lat, lng)),
                                        title = camp.name
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (cachedOtherBitmaps.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cachedOtherBitmaps) { bitmap ->
                                    val index = allImages.indexOf(bitmap)
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = stringResource(R.string.additional_image),
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedImageIndex = index }
                                            .shadow(4.dp, RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_images_available),
                                color = Color(0xFF14571C).copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                item {
                    if (currentUserId != null && currentUserId != camp.creatorId) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF14571C).copy(alpha = 0.1f))
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = ratingInput,
                                onValueChange = { ratingInput = it },
                                label = { Text(stringResource(R.string.rating_label), color = Color(0xFF14571C)) },
                                placeholder = { Text("1.0 - 10.0", color = Color(0xFF14571C).copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF14571C),
                                    unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                    focusedLabelColor = Color(0xFF14571C),
                                    unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                    cursorColor = Color(0xFF14571C)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val rating = ratingInput.toDoubleOrNull()
                                    if (rating != null && rating in 1.0..10.0) {
                                        campsiteViewModel.rateCampsite(
                                            campsiteId = camp.id,
                                            rating = rating,
                                            context = context,
                                            onSuccess = { ratingInput = "" },
                                            onFailure = { e ->
                                                Log.e("CampsiteScreen", "Rating failed: ${e.message}")
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF14571C),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.submit_rating),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                item {
                    if (currentUserId != null && currentUserId == camp.creatorId) {
                        Button(
                            onClick = {
                                campsiteViewModel.deleteCampsite(
                                    campsiteId = camp.id,
                                    creatorId = camp.creatorId,
                                    onSuccess = {
                                        navController.navigate(Routes.ALL_CAMPS) {
                                            popUpTo(Routes.ALL_CAMPS) { inclusive = true }
                                        }
                                    },
                                    onFailure = { e ->
                                        Log.e("CampsiteScreen", "Delete failed: ${e.message}")
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.delete_campsite),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (selectedImageIndex != null && allImages.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = selectedImageIndex!!,
                pageCount = { allImages.size }
            )
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                HorizontalPager(state = pagerState) { page ->
                    ZoomableImage(
                        bitmap = allImages[page],
                        onDismiss = { selectedImageIndex = null }
                    )
                }
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
                if (pagerState.currentPage < allImages.size - 1) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { selectedImageIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFF14571C).copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2
                        offsetX = offsetX.coerceIn(-maxX, maxX)
                        offsetY = offsetY.coerceIn(-maxY, maxY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    )
}