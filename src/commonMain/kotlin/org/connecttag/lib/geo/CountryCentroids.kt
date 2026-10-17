package org.connecttag.lib.geo

/**
 * Standard registry of approximate country centroids (WGS84 Latitude, Longitude)
 * for map visualizations and geoblocking analysis.
 */
public object CountryCentroids {

    private val CENTROIDS: Map<String, GeoCoordinate> = mapOf(
        // Middle East & North Africa
        "SA" to GeoCoordinate(23.8859, 45.0792),
        "AE" to GeoCoordinate(23.4241, 53.8478),
        "EG" to GeoCoordinate(26.8206, 30.8025),
        "QA" to GeoCoordinate(25.3548, 51.1839),
        "KW" to GeoCoordinate(29.3117, 47.4818),
        "BH" to GeoCoordinate(26.0667, 50.5577),
        "OM" to GeoCoordinate(21.4735, 55.9754),
        "JO" to GeoCoordinate(30.5852, 36.2384),
        "IQ" to GeoCoordinate(33.2232, 43.6793),
        "YE" to GeoCoordinate(15.5527, 48.5164),
        "SY" to GeoCoordinate(34.8021, 38.9968),
        "LB" to GeoCoordinate(33.8547, 35.8623),
        "PS" to GeoCoordinate(31.9522, 35.2332),
        "MA" to GeoCoordinate(31.7917, -7.0926),
        "DZ" to GeoCoordinate(28.0339, 1.6596),
        "TN" to GeoCoordinate(33.8869, 9.5375),
        "LY" to GeoCoordinate(26.3351, 17.2283),
        "SD" to GeoCoordinate(12.8628, 30.2176),
        "TR" to GeoCoordinate(38.9637, 35.2433),
        "IR" to GeoCoordinate(32.4279, 53.6880),

        // Americas
        "US" to GeoCoordinate(37.0902, -95.7129),
        "CA" to GeoCoordinate(56.1304, -106.3468),
        "MX" to GeoCoordinate(23.6345, -102.5528),
        "BR" to GeoCoordinate(-14.2350, -51.9253),
        "AR" to GeoCoordinate(-38.4161, -63.6167),
        "CL" to GeoCoordinate(-35.6751, -71.5430),
        "CO" to GeoCoordinate(4.5709, -74.2973),
        "PE" to GeoCoordinate(-9.1900, -75.0152),
        "VE" to GeoCoordinate(6.4238, -66.5897),

        // Europe
        "GB" to GeoCoordinate(55.3781, -3.4360),
        "DE" to GeoCoordinate(51.1657, 10.4515),
        "FR" to GeoCoordinate(46.2276, 2.2137),
        "NL" to GeoCoordinate(52.1326, 5.2913),
        "IT" to GeoCoordinate(41.8719, 12.5674),
        "ES" to GeoCoordinate(40.4637, -3.7492),
        "SE" to GeoCoordinate(60.1282, 18.6435),
        "CH" to GeoCoordinate(46.8182, 8.2275),
        "PL" to GeoCoordinate(51.9194, 19.1451),
        "RU" to GeoCoordinate(61.5240, 105.3188),
        "UA" to GeoCoordinate(48.3794, 31.1656),
        "IE" to GeoCoordinate(53.1424, -7.6921),
        "NO" to GeoCoordinate(60.4720, 8.4689),
        "FI" to GeoCoordinate(61.9241, 25.7482),
        "BE" to GeoCoordinate(50.5039, 4.4699),
        "AT" to GeoCoordinate(47.5162, 14.5501),
        "DK" to GeoCoordinate(56.2639, 9.5018),
        "RO" to GeoCoordinate(45.9432, 24.9668),
        "GR" to GeoCoordinate(39.0742, 21.8243),
        "PT" to GeoCoordinate(39.3999, -8.2245),
        "CZ" to GeoCoordinate(49.8175, 15.4730),
        "BY" to GeoCoordinate(53.7098, 27.9534),

        // Asia & Pacific
        "CN" to GeoCoordinate(35.8617, 104.1954),
        "JP" to GeoCoordinate(36.2048, 138.2529),
        "KR" to GeoCoordinate(35.9078, 127.7669),
        "KP" to GeoCoordinate(40.3399, 127.5101),
        "IN" to GeoCoordinate(20.5937, 78.9629),
        "SG" to GeoCoordinate(1.3521, 103.8198),
        "TW" to GeoCoordinate(23.6978, 120.9605),
        "HK" to GeoCoordinate(22.3193, 114.1694),
        "VN" to GeoCoordinate(14.0583, 108.2772),
        "ID" to GeoCoordinate(-0.7893, 113.9213),
        "MY" to GeoCoordinate(4.2105, 101.9758),
        "TH" to GeoCoordinate(15.8700, 100.9925),
        "PK" to GeoCoordinate(30.3753, 69.3451),
        "PH" to GeoCoordinate(12.8797, 121.7740),
        "BD" to GeoCoordinate(23.6850, 90.3563),
        "AU" to GeoCoordinate(-25.2744, 133.7751),
        "NZ" to GeoCoordinate(-40.9006, 174.8860),

        // Africa
        "ZA" to GeoCoordinate(-30.5595, 22.9375),
        "NG" to GeoCoordinate(9.0820, 8.6753),
        "KE" to GeoCoordinate(-0.0236, 37.9062),
        "GH" to GeoCoordinate(7.9465, -1.0232),
        "ET" to GeoCoordinate(9.1450, 40.4897)
    )

    /**
     * Resolves the approximate geographical centroid coordinate for an ISO country code.
     */
    public fun getCountryCentroid(countryCode: String?): GeoCoordinate {
        if (countryCode.isNullOrBlank() || countryCode.equals("LOCAL", ignoreCase = true)) {
            return GeoCoordinate(24.0, 45.0) // Fallback central Middle East coordinate
        }
        val code = countryCode.trim().uppercase()
        return CENTROIDS[code] ?: GeoCoordinate(30.0, 30.0) // General global fallback
    }

    /**
     * Returns true if the given ISO country code is recognized in the centroid catalog.
     */
    public fun containsCountry(countryCode: String?): Boolean {
        if (countryCode.isNullOrBlank()) return false
        return CENTROIDS.containsKey(countryCode.trim().uppercase())
    }
}
