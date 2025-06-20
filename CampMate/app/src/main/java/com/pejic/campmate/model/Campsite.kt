package com.pejic.campmate.model

data class Campsite(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val price: String = "",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val creatorId: String = "",
    val mainImageBase64: String? = null,
    val imageBase64s: List<String> = emptyList(),
    val ratings: Map<String, Double> = emptyMap(),
    val availability: Boolean = true,
    val type: String = "Ostalo"
)