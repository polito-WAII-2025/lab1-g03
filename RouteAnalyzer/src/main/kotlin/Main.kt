package org.example
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import kotlin.math.*

data class Waypoint( val timestamp: String, val latitude: Double, val longitude: Double )

data class CustomParameters(
    val earthRadiusKm: Double,
    val geofenceCenterLatitude: Double,
    val geofenceCenterLongitude: Double,
    val geofenceRadiusKm: Double,
    val mostFrequentedAreaRadiusKm: Double? = null // Optional field
)

fun readWaypointsFromCsv(filePath: String): List<Waypoint> {
    val waypoints = mutableListOf<Waypoint>()
    File(filePath).useLines { lines ->
        lines.forEach { line ->  // Skip header
            val parts = line.split(";")
            if (parts.size >= 3) {
                val timestamp = parts[0]
                val latitude = parts[1].toDoubleOrNull()
                val longitude = parts[2].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    waypoints.add(Waypoint(timestamp, latitude, longitude))
                }
            }
        }
    }
    return waypoints
}

fun readCustomParameters(filePath: String): CustomParameters? {
    val file = File(filePath)
    if (!file.exists()) {
        println("YAML file not found: ${file.absolutePath}")
        return null
    }

    val yaml = Yaml()
    FileInputStream(file).use { inputStream ->
        val data: Map<String, Any> = yaml.load(inputStream)

        return CustomParameters(
            earthRadiusKm = (data["earthRadiusKm"] as Number).toDouble(),
            geofenceCenterLatitude = (data["geofenceCenterLatitude"] as Number).toDouble(),
            geofenceCenterLongitude = (data["geofenceCenterLongitude"] as Number).toDouble(),
            geofenceRadiusKm = (data["geofenceRadiusKm"] as Number).toDouble(),
            mostFrequentedAreaRadiusKm = (data["mostFrequentedAreaRadiusKm"] as Number?)?.toDouble()
        )
    }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double, earthRadiusKm: Double): Double {

    val lat1Radians = Math.toRadians(lat1)
    val lat2Radians = Math.toRadians(lat2)
    val lon1Radians = Math.toRadians(lon1)
    val lon2Radians = Math.toRadians(lon2)
    val dLat = lat2Radians - lat1Radians
    val dLon = lon2Radians - lon1Radians
    val a = sin(dLat / 2).pow(2) + cos(lat1Radians) * cos(lat2Radians) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

fun maxDistanceWaypoint(waypoints: List<Waypoint>, params: CustomParameters): Pair<Waypoint, Double> {
    if (waypoints.isEmpty()) return Pair(waypoints.first(), 0.0) // or return null, depending on your needs

    val start = waypoints.first()
    val maxWaypoint = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude, params.earthRadiusKm) }
    val maxDistance = haversine(start.latitude, start.longitude, maxWaypoint!!.latitude, maxWaypoint.longitude, params.earthRadiusKm)

    return Pair(maxWaypoint, maxDistance)
}

fun main( ) {
    val waypoints = readWaypointsFromCsv("evaluation/waypoints.csv")
    val params = readCustomParameters("evaluation/custom-parameters.yml")

    if (waypoints.isNotEmpty() && params != null) {
        val (maxWaypoint, maxDistance) = maxDistanceWaypoint(waypoints, params)
        println("Max Distance: $maxDistance km, Max Waypoint: $maxWaypoint")
    } else {
        println("No valid waypoints found.")
    }
}
