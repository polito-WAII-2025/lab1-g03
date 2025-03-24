@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.example.utils.*
import java.io.File

// single json instance for better performance
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
