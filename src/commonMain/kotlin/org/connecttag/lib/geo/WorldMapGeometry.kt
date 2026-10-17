package org.connecttag.lib.geo

/**
 * Geometric models and projection algorithms for world map rendering and geospatial overlays.
 */
public object WorldMapGeometry {

    /**
     * Projects spherical coordinates (Latitude, Longitude) into normalized 2D plane coordinates
     * using the Equirectangular (Plate Carrée) projection.
     *
     * @return Pair of (xRatio, yRatio), each in the range [0.0..1.0] where (0,0) is Top-Left.
     */
    public fun projectNormalized(latitude: Double, longitude: Double): Pair<Float, Float> {
        val xRatio = ((longitude + 180.0) / 360.0).coerceIn(0.0, 1.0)
        val yRatio = ((90.0 - latitude) / 180.0).coerceIn(0.0, 1.0)
        return Pair(xRatio.toFloat(), yRatio.toFloat())
    }

    /**
     * Projects spherical coordinates onto 2D canvas dimensions [width] x [height].
     */
    public fun project(latitude: Double, longitude: Double, width: Float, height: Float): Pair<Float, Float> {
        val (xRatio, yRatio) = projectNormalized(latitude, longitude)
        return Pair(xRatio * width, yRatio * height)
    }

    public fun project(coord: GeoCoordinate, width: Float, height: Float): Pair<Float, Float> {
        return project(coord.latitude, coord.longitude, width, height)
    }

    /**
     * Resolves the country's centroid and projects it onto the 2D canvas plane.
     */
    public fun projectCountry(countryCode: String?, width: Float, height: Float): Pair<Float, Float> {
        val coords = CountryCentroids.getCountryCentroid(countryCode)
        return project(coords.latitude, coords.longitude, width, height)
    }

    /**
     * Simplified continental boundary polygon paths for lightweight map vector rendering.
     * Each polygon is a list of [GeoCoordinate] (Latitude, Longitude).
     */
    public val CONTINENT_POLYGONS: List<List<GeoCoordinate>> = listOf(
        // 1. North America
        listOf(
            GeoCoordinate(65.0, -168.0), GeoCoordinate(70.0, -140.0), GeoCoordinate(72.0, -90.0),
            GeoCoordinate(60.0, -60.0), GeoCoordinate(48.0, -52.0), GeoCoordinate(42.0, -70.0),
            GeoCoordinate(25.0, -80.0), GeoCoordinate(20.0, -97.0), GeoCoordinate(20.0, -105.0),
            GeoCoordinate(32.0, -117.0), GeoCoordinate(48.0, -125.0), GeoCoordinate(60.0, -165.0)
        ),
        // 2. South America
        listOf(
            GeoCoordinate(11.0, -75.0), GeoCoordinate(0.0, -50.0), GeoCoordinate(-7.0, -35.0),
            GeoCoordinate(-22.0, -40.0), GeoCoordinate(-34.0, -52.0), GeoCoordinate(-54.0, -65.0),
            GeoCoordinate(-45.0, -75.0), GeoCoordinate(-20.0, -70.0), GeoCoordinate(-5.0, -80.0),
            GeoCoordinate(8.0, -77.0)
        ),
        // 3. Europe
        listOf(
            GeoCoordinate(36.0, -10.0), GeoCoordinate(43.0, 0.0), GeoCoordinate(48.0, -5.0),
            GeoCoordinate(54.0, 5.0), GeoCoordinate(60.0, 10.0), GeoCoordinate(70.0, 20.0),
            GeoCoordinate(70.0, 30.0), GeoCoordinate(65.0, 40.0), GeoCoordinate(50.0, 45.0),
            GeoCoordinate(45.0, 30.0), GeoCoordinate(36.0, 25.0), GeoCoordinate(38.0, 15.0),
            GeoCoordinate(36.0, -5.0)
        ),
        // 4. Africa
        listOf(
            GeoCoordinate(15.0, -17.0), GeoCoordinate(35.0, -10.0), GeoCoordinate(37.0, 10.0),
            GeoCoordinate(32.0, 25.0), GeoCoordinate(28.0, 34.0), GeoCoordinate(12.0, 43.0),
            GeoCoordinate(10.0, 51.0), GeoCoordinate(-5.0, 40.0), GeoCoordinate(-25.0, 35.0),
            GeoCoordinate(-35.0, 20.0), GeoCoordinate(-30.0, 15.0), GeoCoordinate(-10.0, 12.0),
            GeoCoordinate(5.0, 0.0), GeoCoordinate(10.0, -15.0)
        ),
        // 5. Asia & Middle East
        listOf(
            GeoCoordinate(32.0, 35.0), GeoCoordinate(40.0, 45.0), GeoCoordinate(60.0, 50.0),
            GeoCoordinate(70.0, 60.0), GeoCoordinate(76.0, 100.0), GeoCoordinate(72.0, 140.0),
            GeoCoordinate(65.0, 170.0), GeoCoordinate(45.0, 145.0), GeoCoordinate(35.0, 130.0),
            GeoCoordinate(22.0, 120.0), GeoCoordinate(10.0, 105.0), GeoCoordinate(22.0, 90.0),
            GeoCoordinate(8.0, 75.0), GeoCoordinate(25.0, 70.0), GeoCoordinate(25.0, 60.0),
            GeoCoordinate(15.0, 55.0), GeoCoordinate(12.0, 45.0), GeoCoordinate(28.0, 36.0)
        ),
        // 6. Australia
        listOf(
            GeoCoordinate(-22.0, 115.0), GeoCoordinate(-14.0, 125.0), GeoCoordinate(-12.0, 135.0),
            GeoCoordinate(-15.0, 145.0), GeoCoordinate(-28.0, 153.0), GeoCoordinate(-38.0, 150.0),
            GeoCoordinate(-38.0, 140.0), GeoCoordinate(-32.0, 130.0), GeoCoordinate(-34.0, 115.0)
        )
    )
}
