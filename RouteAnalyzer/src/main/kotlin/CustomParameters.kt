package org.example

data class CustomParameters(
    val earthRadiusKm: Double,
    val geofenceCenterLatitude: Double,
    val geofenceCenterLongitude: Double,
    val geofenceRadiusKm: Double,
    val mostFrequentedAreaRadiusKm: Double? = null,
)