@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

import com.uber.h3core.H3Core
import org.assertj.core.api.Assertions.assertThat
import org.example.*
import org.example.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.round

class MyTest {
    private lateinit var waypoints: List<Waypoint>
    private lateinit var params: CustomParameters

    @Test
    fun test() {
        println("Test")
    }

    @BeforeEach
    fun setup() {
        // Sample waypoints
        waypoints =
            listOf(
                Waypoint(1.0, 37.7749, -122.4194), // San Francisco
                Waypoint(2.0, 34.0522, -118.2437), // Los Angeles
                Waypoint(3.0, 40.7128, -74.0060), // New York
            )

        params =
            CustomParameters(
                earthRadiusKm = 6371.0,
                geofenceCenterLatitude = 37.7749,
                geofenceCenterLongitude = -122.4194,
                geofenceRadiusKm = 50.0,
                mostFrequentedAreaRadiusKm = 10.0,
            )
    }

    @Test
    fun haversine() {
        val distance = haversine(37.7749, -122.4194, 34.0522, -118.2437, 6371.0)
        assertEquals(559.1, round(distance * 10) / 10, "Haversine distance is incorrect")
    }

    @Test
    fun `test maxDistanceFromStart`() {
        val (farthestWaypoint, distance) = maxDistanceFromStart(waypoints, params)
        assertEquals(40.7128, farthestWaypoint.latitude, "Farthest waypoint latitude mismatch")
        assertEquals(-74.0060, farthestWaypoint.longitude, "Farthest waypoint longitude mismatch")
        assertTrue(distance > 2000, "Max distance should be greater than 2000 km")
    }

    @Test
    fun `test waypointsOutsideGeofence`() {
        val outsideWaypoints = waypointsOutsideGeofence(waypoints, 37.7749, -122.4194, 50.0, 6371.0)
        assertThat(outsideWaypoints).hasSize(2) // LA & NY should be outside
    }

    @Test
    fun `test readWaypointsFromCsv`() {
        val tempFile =
            File.createTempFile("waypoints", ".csv").apply {
                writeText("1.0;37.7749;-122.4194\n2.0;34.0522;-118.2437")
            }

        val waypoints = readWaypointsFromCsv(tempFile.absolutePath)
        assertEquals(2, waypoints.size, "Waypoints count mismatch")
        assertEquals(37.7749, waypoints[0].latitude, "First waypoint latitude incorrect")
    }

    @Test
    fun `test readCustomParameters`() {
        val tempFile =
            File.createTempFile("config", ".yaml").apply {
                writeText(
                    """
                    earthRadiusKm: 6371.0
                    geofenceCenterLatitude: 37.7749
                    geofenceCenterLongitude: -122.4194
                    geofenceRadiusKm: 50.0
                    mostFrequentedAreaRadiusKm: 10.0
                    """.trimIndent(),
                )
            }

        val params = readCustomParameters(tempFile.absolutePath)
        assertNotNull(params, "Parameters should not be null")
        assertEquals(6371.0, params!!.earthRadiusKm, "Earth radius mismatch")
    }

    @Test
    fun `test radiusToResolution`() {
        assertEquals(9, radiusToResolution(1.7), "Incorrect H3 resolution for given radius")
        assertEquals(5, radiusToResolution(85.0), "Incorrect H3 resolution for given radius")
    }

    @Test
    fun `haversine same point`() {
        val distance = haversine(37.7749, -122.4194, 37.7749, -122.4194, 6371.0)
        assertEquals(0.0, distance, "Distance between the same points should be 0")
    }

    @Test
    fun `haversine antipodal points`() {
        val distance = haversine(0.0, 0.0, 0.0, 180.0, 6371.0)
        assertTrue(distance > 20000, "Distance between antipodal points should be near Earth's diameter")
    }

    @Test
    fun `maxDistanceFromStart single waypoint`() {
        val waypoints = listOf(Waypoint(1.0, 37.7749, -122.4194))
        val (farthestWaypoint, distance) = maxDistanceFromStart(waypoints, params)

        assertEquals(37.7749, farthestWaypoint.latitude, "Should return the only waypoint")
        assertEquals(0.0, distance, "Distance should be zero for a single waypoint")
    }

    @Test
    fun `maxDistanceFromStart nearby points`() {
        val waypoints =
            listOf(
                Waypoint(1.0, 37.7749, -122.4194),
                Waypoint(2.0, 37.7750, -122.4195), // A very close point
            )
        val (_, distance) = maxDistanceFromStart(waypoints, params)

        assertTrue(distance < 1.0, "Distance should be very small")
    }

    @Test
    fun `waypointsOutsideGeofence all inside`() {
        val insideWaypoints =
            listOf(
                Waypoint(1.0, 37.7749, -122.4194),
                Waypoint(2.0, 37.7750, -122.4195),
            )

        val result = waypointsOutsideGeofence(insideWaypoints, 37.7749, -122.4194, 100.0, 6371.0)
        assertThat(result).isEmpty()
    }

    @Test
    fun `waypointsOutsideGeofence all outside`() {
        val outsideWaypoints =
            listOf(
                Waypoint(1.0, 34.0522, -118.2437),
                Waypoint(2.0, 40.7128, -74.0060),
            )

        val result = waypointsOutsideGeofence(outsideWaypoints, 37.7749, -122.4194, 50.0, 6371.0)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `waypointsOutsideGeofence on boundary`() {
        val boundaryPoint = Waypoint(1.0, 37.7749, -122.4194 + (50.0 / 111.0)) // Approx. 50 km in longitude
        val result = waypointsOutsideGeofence(listOf(boundaryPoint), 37.7749, -122.4194, 50.0, 6371.0)

        assertThat(result).isEmpty() // Should be inside the geofence
    }

    @Test
    fun `radiusToResolution smallest valid radius`() {
        assertEquals(15, radiusToResolution(0.005), "Should return max resolution for small radius")
    }

    @Test
    fun `radiusToResolution too small radius`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                radiusToResolution(0.001)
            }
        assertTrue(exception.message!!.contains("Radius too small"))
    }

    @Test
    fun `mostFrequentedArea single H3 cell`() {
        val closeWaypoints =
            listOf(
                Waypoint(1.0, 37.7749, -122.4194),
                Waypoint(2.0, 37.7750, -122.4195),
                Waypoint(3.0, 37.7751, -122.4196),
            )

        val h3 = H3Core.newInstance()
        closeWaypoints.forEach { wp ->
            val h3Index = h3.geoToH3(wp.latitude, wp.longitude, 8)
            println("Waypoint at (${wp.latitude}, ${wp.longitude}) -> H3 Index: $h3Index")
        }

        val (frequentedWaypoints, count) = mostFrequentedArea(closeWaypoints, 8)

        assertThat(frequentedWaypoints).hasSize(3)
        assertEquals(3, count, "All waypoints should belong to the same H3 cell")
    }

    @Test
    fun `mostFrequentedArea multiple cells`() {
        val spreadWaypoints =
            listOf(
                Waypoint(1.0, 37.7749, -122.4194), // SF
                Waypoint(2.0, 34.0522, -118.2437), // LA
                Waypoint(3.0, 40.7128, -74.0060), // NY
            )

        val (frequentedWaypoints, _) = mostFrequentedArea(spreadWaypoints, 9)
        assertThat(frequentedWaypoints.size).isLessThan(3)
    }

    @Test
    fun `test malformed CSV`() {
        val tempFile =
            File.createTempFile("waypoints", ".csv").apply {
                writeText("invalid,data,here\n1.0;37.7749;INVALID\n;;\n1.0;37.7749;-122.4194")
            }

        val waypoints = readWaypointsFromCsv(tempFile.absolutePath)
        assertEquals(1, waypoints.size, "Only one valid waypoint should be parsed")
    }

    @Test
    fun `test geofence boundary precision`() {
        val boundaryPoint = Waypoint(1.0, 37.7749, -122.4194 + (50.0 / 111.0))
        val result = waypointsOutsideGeofence(listOf(boundaryPoint), 37.7749, -122.4194, 50.0, 6371.0)

        assertThat(result).isEmpty() // Should be considered within the geofence
    }

    @Test
    fun `test H3 resolution boundaries`() {
        assertEquals(0, radiusToResolution(11070.0), "Incorrect resolution for large radius")
        assertEquals(15, radiusToResolution(0.004), "Incorrect resolution for smallest valid radius")
    }

    @Test
    fun `test large dataset performance`() {
        val largeWaypoints =
            (1..100_000).map {
                Waypoint(it.toDouble(), 37.7749 + it * 0.00001, -122.4194 + it * 0.00001)
            }

        val (farthestWaypoint, distance) = maxDistanceFromStart(largeWaypoints, params)

        assertNotNull(farthestWaypoint, "Farthest waypoint should not be null for large dataset")
        assertTrue(distance > 0, "Distance should be positive")
    }
}
