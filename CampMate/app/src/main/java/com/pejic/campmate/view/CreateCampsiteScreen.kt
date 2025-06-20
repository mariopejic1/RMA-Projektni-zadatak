package com.pejic.campmate.view

import ImageViewModel
import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pejic.campmate.R
import com.pejic.campmate.Routes
import com.pejic.campmate.viewmodel.CreateCampsiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampsiteScreen(
    navController: NavController,
    viewModel: CreateCampsiteViewModel,
    imageViewModel: ImageViewModel
) {
    val campsite by viewModel.campsite.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(45.5511, 18.6939),
            12f
        )
    }

    val mainImagesBitmaps by imageViewModel.mainImagesBitmaps.collectAsState()
    val otherImagesBitmaps by imageViewModel.otherImagesBitmaps.collectAsState()
    val tempCampsiteId = "temp" // Privremeni ID za slike tijekom kreiranja

    var mainImageBase64 by remember { mutableStateOf<String?>(null) }
    val otherImagesBase64s = remember { mutableStateListOf<String>() }

    val campsiteTypes = listOf(
        stringResource(R.string.campsite_type_mountain),
        stringResource(R.string.campsite_type_beach),
        stringResource(R.string.campsite_type_river),
        stringResource(R.string.campsite_type_lake),
        stringResource(R.string.campsite_type_plain),
        stringResource(R.string.campsite_type_desert),
        stringResource(R.string.campsite_type_other)
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(campsite.type) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> viewModel.updatePermissions(permissions) }

    val mainImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        imageViewModel.updateMainImageUri(tempCampsiteId, uri, context) { base64 ->
            mainImageBase64 = base64
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            imageViewModel.addOtherImageUri(tempCampsiteId, uri, context) { base64 ->
                base64?.let { otherImagesBase64s.add(it) }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageViewModel.getCapturedImageUri()?.let { uri ->
                imageViewModel.addOtherImageUri(tempCampsiteId, uri, context) { base64 ->
                    base64?.let { otherImagesBase64s.add(it) }
                }
            }
        }
    }

    // Očisti slike kada se ekran zatvori
    DisposableEffect(Unit) {
        onDispose {
            imageViewModel.clearImagesForCampsite(tempCampsiteId)
        }
    }

    fun Modifier.inputCard() = this
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White)
        .padding(12.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.create_campsite),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14571C))
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14571C))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Button(
                    onClick = {
                        viewModel.addCampsite(
                            context = context,
                            mainImageBase64 = mainImageBase64,
                            imageBase64s = otherImagesBase64s,
                            onSuccess = {
                                imageViewModel.clearImagesForCampsite(tempCampsiteId) // Očisti slike nakon uspjeha
                                navController.navigate(Routes.ALL_CAMPS) {
                                    popUpTo(Routes.ALL_CAMPS) { inclusive = true }
                                }
                            },
                            onFailure = { e ->
                                Log.e("CreateCampsite", "Failed to create campsite: ${e.message}")
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && campsite.name.isNotBlank() && campsite.location.isNotBlank() && mainImageBase64 != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF14571C),
                        disabledContainerColor = Color.White.copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF14571C).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(R.string.create_campsite),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        color = Color.White
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        },
        containerColor = Color(0xFF14571C),
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            @Composable
            fun inputLabel(text: String) = Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF14571C),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            item {
                Column(modifier = Modifier.inputCard()) {
                    inputLabel(stringResource(R.string.campsite_name))
                    TextField(
                        value = campsite.name,
                        onValueChange = { viewModel.updateName(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp)),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        keyboardOptions = KeyboardOptions.Default,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            item {
                Column(modifier = Modifier.inputCard()) {
                    inputLabel(stringResource(R.string.location))
                    TextField(
                        value = campsite.location,
                        onValueChange = { viewModel.updateLocation(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp)),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        keyboardOptions = KeyboardOptions.Default,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            item {
                Column(modifier = Modifier.inputCard()) {
                    inputLabel(stringResource(R.string.price))
                    TextField(
                        value = campsite.price,
                        onValueChange = { viewModel.updatePrice(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp)),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            item {
                Column(modifier = Modifier.inputCard()) {
                    inputLabel(stringResource(R.string.description))
                    TextField(
                        value = campsite.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp)),
                        maxLines = 5,
                        textStyle = TextStyle(color = Color.Black),
                        keyboardOptions = KeyboardOptions.Default,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.availability_label),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF14571C),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Switch(
                            checked = campsite.availability,
                            onCheckedChange = { viewModel.updateAvailability(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF14571C),
                                uncheckedThumbColor = Color.Gray,
                                checkedTrackColor = Color(0xFF8BC34A),
                                uncheckedTrackColor = Color.LightGray
                            )
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    stringResource(R.string.campsite_type_label),
                                    color = Color(0xFF14571C),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp))
                                .menuAnchor(),
                            textStyle = TextStyle(color = Color.Black),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            campsiteTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        viewModel.updateType(type)
                                        expanded = false
                                        Log.d("CreateCampsiteScreen", "Selected campsite type: $type")
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = true,
                                mapToolbarEnabled = true
                            ),
                            onMapClick = { latLng -> viewModel.updateSelectedLatLng(latLng) }
                        ) {
                            campsite.latitude?.let { lat ->
                                campsite.longitude?.let { lng ->
                                    Marker(
                                        state = MarkerState(position = LatLng(lat, lng)),
                                        title = campsite.name.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.campsite_name)
                                    )
                                }
                            }
                        }
                        locationError?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            item {
                Column(modifier = Modifier.inputCard()) {
                    Text(
                        text = stringResource(R.string.main_image_label),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFF14571C),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { mainImageLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF14571C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.select_main_image), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    mainImagesBitmaps[tempCampsiteId]?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.main_image_label),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                imageViewModel.removeMainImage(tempCampsiteId)
                                mainImageBase64 = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14571C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.remove_image), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.inputCard()) {
                    Text(
                        text = stringResource(R.string.additional_images_label),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFF14571C),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    imageViewModel.createImageFile(context)?.let { uri ->
                                        cameraLauncher.launch(uri)
                                    }
                                } else {
                                    permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14571C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.take_additional_photo), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14571C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.choose_additional_photos), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(otherImagesBitmaps[tempCampsiteId] ?: emptyList()) { index, bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.additional_image),
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        imageViewModel.removeOtherImage(tempCampsiteId, index)
                                        if (index in otherImagesBase64s.indices) otherImagesBase64s.removeAt(index)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.remove_image), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}