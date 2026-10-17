package org.connecttag.lib.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldMapGeometryTest {

    @Test
    fun projectsCoordinatesCorrectly() {
        // (0, 0) is the center of the world map -> xRatio = 0.5, yRatio = 0.5
        val (xNorm, yNorm) = WorldMapGeometry.projectNormalized(0.0, 0.0)
        assertEquals(0.5f, xNorm, 0.001f)
        assertEquals(0.5f, yNorm, 0.001f)

        // Project onto 1000x500 canvas
        val (x, y) = WorldMapGeometry.project(0.0, 0.0, 1000f, 500f)
        assertEquals(500f, x, 0.001f)
        assertEquals(250f, y, 0.001f)
    }

    @Test
    fun resolvesKnownCentroids() {
        val saCentroid = CountryCentroids.getCountryCentroid("SA")
        assertTrue(saCentroid.isValid)
        assertEquals(23.8859, saCentroid.latitude, 0.0001)
        assertEquals(45.0792, saCentroid.longitude, 0.0001)

        val localCentroid = CountryCentroids.getCountryCentroid("LOCAL")
        assertTrue(localCentroid.isValid)

        assertTrue(CountryCentroids.containsCountry("US"))
        assertTrue(CountryCentroids.containsCountry("EG"))
        assertTrue(CountryCentroids.containsCountry("DE"))
    }

    @Test
    fun continentPolygonsAreComplete() {
        assertEquals(6, WorldMapGeometry.CONTINENT_POLYGONS.size)
        WorldMapGeometry.CONTINENT_POLYGONS.forEach { polygon ->
            assertTrue(polygon.size >= 8)
            polygon.forEach { coord ->
                assertTrue(coord.isValid)
            }
        }
    }
}
