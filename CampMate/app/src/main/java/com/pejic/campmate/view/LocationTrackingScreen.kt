package com.pejic.campmate.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pejic.campmate.R
import com.pejic.campmate.viewmodel.LocationTrackingViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackingScreen(
    navController: NavController,
    viewModel: LocationTrackingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        viewModel.setPermissionGranted(granted)
        if (!granted) showPermissionRationale = true
        else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                showGpsDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted && coarseLocationGranted) {
            viewModel.setPermissionGranted(true)
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                showGpsDialog = true
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(state.targetLocation) {
        state.targetLocation?.let { (lat, lon) ->
            coroutineScope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14f),
                    durationMs = 1000
                )
            }
        } ?: state.currentLocation?.let { (lat, lon) ->
            coroutineScope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14f),
                    durationMs = 1000
                )
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
                            text = stringResource(R.string.location_tracking_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.nearestCampsite?.let { campsite ->
                state.nearestCampsiteDistance?.let { distance ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.targetLocation?.let { (lat, lon) ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14f),
                                            durationMs = 1000
                                        )
                                    }
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = campsite.name, // Ispravljeno: samo naziv kampa
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.campsite_address, campsite.location),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.campsite_distance, "%.1f".format(distance)), // Ispravljeno: bez "km" u formatu
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                        }
                    }
                }
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = state.permissionGranted)
            ) {
                state.currentLocation?.let { (lat, lon) ->
                    Marker(
                        state = MarkerState(LatLng(lat, lon)),
                        title = stringResource(R.string.your_location)
                    )
                }
                state.nearestCampsite?.let { campsite ->
                    if (campsite.latitude != null && campsite.longitude != null) {
                        Marker(
                            state = MarkerState(LatLng(campsite.latitude, campsite.longitude)),
                            title = campsite.name,
                            snippet = stringResource(R.string.find_nearest_campsite)
                        )
                    }
                }
                state.campsites.forEach { campsite ->
                    if (campsite.latitude != null && campsite.longitude != null) {
                        Marker(
                            state = MarkerState(LatLng(campsite.latitude, campsite.longitude)),
                            title = campsite.name,
                            snippet = campsite.location
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.requestSingleLocation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF14571C)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.find_nearest_campsite),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text(stringResource(R.string.permission_required)) },
                text = { Text(stringResource(R.string.location_permission_rationale)) },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationale = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showGpsDialog) {
            AlertDialog(
                onDismissRequest = { showGpsDialog = false },
                title = { Text(stringResource(R.string.gps_disabled_title)) },
                text = { Text(stringResource(R.string.gps_disabled_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showGpsDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) {
                        Text(stringResource(R.string.open_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGpsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}