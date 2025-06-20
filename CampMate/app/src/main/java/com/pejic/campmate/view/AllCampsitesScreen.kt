package com.pejic.campmate.view

import ImageViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pejic.campmate.R
import com.pejic.campmate.Routes
import com.pejic.campmate.model.Campsite
import com.pejic.campmate.viewmodel.AllCampsitesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCampsitesScreen(
    navController: NavController,
    viewModel: AllCampsitesViewModel = AllCampsitesViewModel(),
    imageViewModel: ImageViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf(0f) }
    var maxPrice by remember { mutableStateOf(1000f) }
    var showAvailableOnly by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Svi") }
    var typeExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val campsiteTypes = listOf(
        stringResource(R.string.campsite_type_all),
        stringResource(R.string.campsite_type_mountain),
        stringResource(R.string.campsite_type_beach),
        stringResource(R.string.campsite_type_river),
        stringResource(R.string.campsite_type_lake),
        stringResource(R.string.campsite_type_plain),
        stringResource(R.string.campsite_type_desert),
        stringResource(R.string.campsite_type_other)
    )
    val campsites by viewModel.campsites.collectAsState()
    var hasNotificationPermission by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(searchQuery) {
        viewModel.fetchAllCampsites(searchQuery = searchQuery, reset = true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-20).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.all_campsites),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14571C),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
        containerColor = Color(0xFF14571C)
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF14571C)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(stringResource(R.string.search_placeholder)) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF14571C),
                                focusedLabelColor = Color(0xFF14571C),
                                unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                cursorColor = Color(0xFF14571C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        )
                        Box {
                            Button(
                                onClick = { filterMenuExpanded = true },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF14571C)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.filter),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false },
                                modifier = Modifier
                                    .width(300.dp)
                                    .background(Color.White)
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.price_range, minPrice.toInt(), maxPrice.toInt()),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF14571C)
                                        )
                                    )
                                    RangeSlider(
                                        value = minPrice..maxPrice,
                                        onValueChange = { range ->
                                            minPrice = range.start
                                            maxPrice = range.endInclusive
                                        },
                                        valueRange = 0f..1000f,
                                        steps = 100,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF14571C),
                                            activeTrackColor = Color(0xFF14571C),
                                            inactiveTrackColor = Color(0xFF14571C).copy(alpha = 0.3f)
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.show_available_only),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF14571C)
                                            )
                                        )
                                        Switch(
                                            checked = showAvailableOnly,
                                            onCheckedChange = { showAvailableOnly = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF14571C),
                                                checkedTrackColor = Color(0xFF14571C).copy(alpha = 0.5f),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.LightGray
                                            )
                                        )
                                    }
                                    ExposedDropdownMenuBox(
                                        expanded = typeExpanded,
                                        onExpandedChange = { typeExpanded = !typeExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.campsite_type_label)) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF14571C),
                                                focusedLabelColor = Color(0xFF14571C),
                                                unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                                unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                                cursorColor = Color(0xFF14571C)
                                            ),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = typeExpanded,
                                            onDismissRequest = { typeExpanded = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            campsiteTypes.forEach { type ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            type,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = if (type == selectedType) Color(0xFF14571C) else Color.Black
                                                            )
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedType = type
                                                        typeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.fetchAllCampsites(
                                                searchQuery = searchQuery,
                                                minPrice = minPrice,
                                                maxPrice = maxPrice,
                                                showAvailableOnly = showAvailableOnly,
                                                type = if (selectedType == "Svi") null else selectedType,
                                                reset = true
                                            )
                                            filterMenuExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF14571C),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.apply_filters),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                LazyColumn {
                    items(campsites) { campsite ->
                        CampsiteCard(campsite = campsite, imageViewModel = imageViewModel, onClick = {
                            navController.navigate("campsite_detail/${campsite.id}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun CampsiteCard(
    campsite: Campsite,
    imageViewModel: ImageViewModel,
    onClick: () -> Unit,
) {
    val bitmaps by imageViewModel.mainImagesBitmaps.collectAsState()
    val bitmap = bitmaps[campsite.id]
    LaunchedEffect(campsite.id) {
        campsite.mainImageBase64?.let {
            imageViewModel.decodeMainImageBase64(campsite.id, it)
        } ?: run {
            imageViewModel.removeMainImage(campsite.id)
        }
    }

    val averageRating = if (campsite.ratings.isNotEmpty()) {
        campsite.ratings.values.average()
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFF14571C).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_images_available),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF14571C),
                            fontSize = 14.sp
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = campsite.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF14571C)
                    ),
                    maxLines = 2
                )

                Text(
                    text = campsite.location,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    ),
                    maxLines = 2
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = averageRating.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF14571C)
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val selectedRoute = currentRoute?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF14571C)
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (selectedRoute == Routes.PROFILE) Color(0xFF14571C) else Color(0xFF14571C)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.profile),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedRoute == Routes.PROFILE) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedRoute == Routes.PROFILE) Color(0xFF14571C) else Color(0xFF14571C)
                    )
                )
            },
            selected = selectedRoute == Routes.PROFILE,
            onClick = {
                navController.navigate(Routes.PROFILE) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = if (selectedRoute == Routes.CREATE_CAMPSITE) Color(0xFF14571C) else Color(0xFF14571C)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.create_campsite),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedRoute == Routes.CREATE_CAMPSITE) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedRoute == Routes.CREATE_CAMPSITE) Color(0xFF14571C) else Color(0xFF14571C)
                    )
                )
            },
            selected = selectedRoute == Routes.CREATE_CAMPSITE,
            onClick = {
                navController.navigate(Routes.CREATE_CAMPSITE) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = if (selectedRoute == Routes.MY_CAMPSITES) Color(0xFF14571C) else Color(0xFF14571C)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.my_campsites),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedRoute == Routes.MY_CAMPSITES) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedRoute == Routes.MY_CAMPSITES) Color(0xFF14571C) else Color(0xFF14571C)
                    )
                )
            },
            selected = selectedRoute == Routes.MY_CAMPSITES,
            onClick = {
                navController.navigate(Routes.MY_CAMPSITES) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (selectedRoute == Routes.LOCATION_TRACKING) Color(0xFF14571C) else Color(0xFF14571C)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.track_location),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selectedRoute == Routes.LOCATION_TRACKING) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedRoute == Routes.LOCATION_TRACKING) Color(0xFF14571C) else Color(0xFF14571C)
                    )
                )
            },
            selected = selectedRoute == Routes.LOCATION_TRACKING,
            onClick = {
                navController.navigate(Routes.LOCATION_TRACKING) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }
}