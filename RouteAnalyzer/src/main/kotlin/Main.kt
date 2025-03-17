@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example
import com.uber.h3core.H3Core
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import kotlin.math.*

@Serializable
data class Waypoint(
    val timestamp: Double,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class MaxDistanceFromStart(
    val waypoint: Waypoint,
    val distanceKm: Double,
)

@Serializable
data class MostFrequentedArea(
    val centralWaypoint: Waypoint,
    val areaRadiusKm: Double,
    val entriesCount: Int,
)

@Serializable
data class WaypointsOutsideGeofence(
    val centralWaypoint: Waypoint,
    val areaRadiusKm: Double,
    val count: Int,
    val waypoints: List<Waypoint>,
)

@Serializable
data class Output(
    val maxDistanceFromStart: MaxDistanceFromStart,
    val mostFrequentedArea: MostFrequentedArea,
    val waypointsOutsideGeofence: WaypointsOutsideGeofence,
)

data class CustomParameters(
    val earthRadiusKm: Double,
    val geofenceCenterLatitude: Double,
    val geofenceCenterLongitude: Double,
    val geofenceRadiusKm: Double,
    val mostFrequentedAreaRadiusKm: Double? = null, // Optional field
)

fun readWaypointsFromCsv(filePath: String): List<Waypoint> {
    val waypoints = mutableListOf<Waypoint>()
    File(filePath).useLines { lines ->
        lines.forEach { line ->
            // Skip header
            val parts = line.split(";")
            if (parts.size >= 3) {
                val timestamp = parts[0].toDoubleOrNull()
                val latitude = parts[1].toDoubleOrNull()
                val longitude = parts[2].toDoubleOrNull()
                if (latitude != null && longitude != null && timestamp != null) {
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
            mostFrequentedAreaRadiusKm = (data["mostFrequentedAreaRadiusKm"] as Number?)?.toDouble(),
        )
    }
}

fun haversine(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
    earthRadiusKm: Double,
): Double {
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

fun maxDistanceFromStart(
    waypoints: List<Waypoint>,
    params: CustomParameters,
): Pair<Waypoint, Double> {
    if (waypoints.isEmpty()) return Pair(waypoints.first(), 0.0) // or return null, depending on your needs

    val start = waypoints.first()
    val maxWaypoint = waypoints.maxByOrNull { haversine(start.latitude, start.longitude, it.latitude, it.longitude, params.earthRadiusKm) }
    val maxDistance = haversine(start.latitude, start.longitude, maxWaypoint!!.latitude, maxWaypoint.longitude, params.earthRadiusKm)

    return Pair(maxWaypoint, maxDistance)
}

fun mostFrequentedArea(
    waypoints: List<Waypoint>,
    resolution: Int = 9,
): Pair<List<Waypoint>, Int> {
    val h3 = H3Core.newInstance()
    val frequencyMap = mutableMapOf<Long, Int>()

    // Index waypoints using H3 and count frequency
    waypoints.forEach { waypoint ->
        val h3Index = h3.geoToH3(waypoint.latitude, waypoint.longitude, resolution)
        frequencyMap[h3Index] = frequencyMap.getOrDefault(h3Index, 0) + 1
    }

    // Find the H3 cell with the highest frequency
    val mostFrequentedH3Index = frequencyMap.maxByOrNull { it.value }?.key

    // Filter waypoints that fall within the most frequented H3 cell
    val mostFrequentedWaypoints =
        waypoints.filter { waypoint ->
            val h3Index = h3.geoToH3(waypoint.latitude, waypoint.longitude, resolution)
            h3Index == mostFrequentedH3Index
        }

    return Pair(mostFrequentedWaypoints, mostFrequentedWaypoints.size)
}

fun waypointsOutsideGeofence(
    waypoints: List<Waypoint>,
    centerLat: Double,
    centerLon: Double,
    radiusKm: Double,
    earthRadiusKm: Double,
): List<Waypoint> =
    waypoints.filter {
        // Calculate the distance between the waypoint and the center of the geo-fence
        val distance = haversine(centerLat, centerLon, it.latitude, it.longitude, earthRadiusKm)
        distance > radiusKm // If the distance is greater than the radius, it's outside the geo-fence
    }

fun radiusToResolution(radiusKm: Double): Int {
    // This is a simplified example. You may need to adjust the logic based on your specific requirements.
    return when {
        radiusKm < 0.1 -> 15
        radiusKm < 0.5 -> 14
        radiusKm < 1 -> 13
        radiusKm < 5 -> 12
        radiusKm < 10 -> 11
        radiusKm < 50 -> 10
        radiusKm < 100 -> 9
        radiusKm < 500 -> 8
        else -> 7
    }
}

val json = Json { prettyPrint = true }

fun main() {
    val waypoints = readWaypointsFromCsv("evaluation/waypoints.csv")
    val params = readCustomParameters("evaluation/custom-parameters.yml")

    if (waypoints.isNotEmpty() && params != null) {
        val (maxWaypoint, maxDistance) = maxDistanceFromStart(waypoints, params)
        val mostFrequentedAreaRadiusKm =
            params.mostFrequentedAreaRadiusKm ?: if (maxDistance < 1) {
                0.1
            } else {
                maxDistance / 10
            }

        val resolution = radiusToResolution(mostFrequentedAreaRadiusKm)
        val (mostFrequentedAreaWaypoints, waypointsInside) = mostFrequentedArea(waypoints, resolution)

        val centralWaypoint = Waypoint(0.0, params.geofenceCenterLatitude, params.geofenceCenterLongitude)
        val waypointsOutside =
            waypointsOutsideGeofence(
                waypoints,
                centralWaypoint.latitude,
                centralWaypoint.longitude,
                params.geofenceRadiusKm,
                params.earthRadiusKm,
            )

        val output =
            Output(
                maxDistanceFromStart = MaxDistanceFromStart(maxWaypoint, maxDistance),
                mostFrequentedArea =
                    MostFrequentedArea(
                        centralWaypoint = mostFrequentedAreaWaypoints.first(),
                        areaRadiusKm = mostFrequentedAreaRadiusKm,
                        entriesCount = waypointsInside,
                    ),
                waypointsOutsideGeofence =
                    WaypointsOutsideGeofence(
                        centralWaypoint = centralWaypoint,
                        areaRadiusKm = params.geofenceRadiusKm,
                        count = waypointsOutside.size,
                        waypoints = waypointsOutside,
                    ),
            )

        val jsonString = json.encodeToString(output)
        File("evaluation/output.json").writeText(jsonString)
    } else {
        println("No valid waypoints found.")
    }
}
