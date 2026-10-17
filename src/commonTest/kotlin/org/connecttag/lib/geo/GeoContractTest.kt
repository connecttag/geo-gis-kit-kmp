package org.connecttag.lib.geo

import kotlin.test.*

class GeoContractTest {
    @Test fun sphericalDistanceUsesMetres() {
        assertEquals(0.0,GeoMathUtils.haversineDistanceMeters(0.0,0.0,0.0,0.0))
        assertEquals(111194.9266,GeoMathUtils.haversineDistanceMeters(0.0,0.0,0.0,1.0),0.01)
        assertEquals(90.0,GeoMathUtils.calculateBearing(0.0,0.0,0.0,1.0),0.0001)
    }
    @Test fun projectionRoundTripsAndRejectsNonFiniteInput() {
        val x = GeoMathUtils.lonToTileX(44.0,12.0)
        val y = GeoMathUtils.latToTileY(15.0,12.0)
        assertEquals(44.0,GeoMathUtils.tileXToLon(x,12.0),0.00001)
        assertEquals(15.0,GeoMathUtils.tileYToLat(y,12.0),0.00001)
        assertFailsWith<IllegalArgumentException> { GeoMathUtils.clampGeoCoordinate(0.0,Double.POSITIVE_INFINITY) }
        assertEquals(180.0,GeoMathUtils.clampGeoCoordinate(0.0,540.0).longitude)
        assertEquals(-180.0,GeoMathUtils.clampGeoCoordinate(0.0,-540.0).longitude)
    }
}
