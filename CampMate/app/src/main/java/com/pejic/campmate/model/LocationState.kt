package com.pejic.campmate.model

data class LocationState(
    val currentLocation: Pair<Double, Double>? = null,
    val nearestCampsite: Campsite? = null,
    val nearestCampsiteDistance: Double? = null,
    val targetLocation: Pair<Double, Double>? = null,
    val campsites: List<Campsite> = emptyList(),
    val permissionGranted: Boolean = false,
    val error: String? = null
)

