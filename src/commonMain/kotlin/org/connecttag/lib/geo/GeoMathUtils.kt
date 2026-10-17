package org.connecttag.lib.geo

import kotlin.math.*

/**
 * حسابات رياضية وجغرافية موحدة لمنظومة رصد (KMP).
 * تشمل صيغ هافرسين، حساب المسافات، زوايا الاتجاه (Bearing)،
 * إسقاط ميركاتور للبلاطات الخرائطية، والجيوهاش التكتيكي.
 */
public object GeoMathUtils {

    public const val EARTH_RADIUS_METERS: Double = 6371000.0

    public fun toRadians(deg: Double): Double = deg * (PI / 180.0)
    public fun toDegrees(rad: Double): Double = rad * (180.0 / PI)
    private fun asinh(x: Double): Double = ln(x + sqrt(x * x + 1.0))

    /**
     * حساب المسافة بالمتر بين نقطتين جغرافيتين باستخدام صيغة هافرسين.
     */
    public fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val rLat1 = toRadians(lat1)
        val rLat2 = toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) + cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * asin(sqrt(a).coerceIn(0.0, 1.0))
        return EARTH_RADIUS_METERS * c
    }

    public fun haversineDistanceMeters(p1: GeoCoordinate, p2: GeoCoordinate): Double =
        haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)

    /**
     * حساب زاوية الاتجاه الأولية (Azimuth / Bearing) بالدرجات (0..360).
     */
    public fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = toRadians(lat1)
        val phi2 = toRadians(lat2)
        val deltaLambda = toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (toDegrees(theta) + 360.0) % 360.0
    }

    /**
     * حساب إحداثيات نقطة الوصول بناءً على نقطة البداية والمسافة وزاوية السير.
     */
    public fun destinationPoint(startPoint: GeoCoordinate, distanceMeters: Double, bearingDegrees: Double): GeoCoordinate {
        val delta = distanceMeters / EARTH_RADIUS_METERS
        val theta = toRadians(bearingDegrees)
        val phi1 = toRadians(startPoint.latitude)
        val lambda1 = toRadians(startPoint.longitude)

        val phi2 = asin(sin(phi1) * cos(delta) + cos(phi1) * sin(delta) * cos(theta))
        val lambda2 = lambda1 + atan2(
            sin(theta) * sin(delta) * cos(phi1),
            cos(delta) - sin(phi1) * sin(phi2)
        )
        return GeoCoordinate(toDegrees(phi2), toDegrees(lambda2))
    }

    /**
     * حساب النقطة المركزية (Centroid) لمجموعة نقاط.
     */
    public fun calculateCentroid(points: List<GeoCoordinate>): GeoCoordinate? {
        if (points.isEmpty()) return null
        val avgLat = points.map { it.latitude }.average()
        val avgLon = points.map { it.longitude }.average()
        return GeoCoordinate(avgLat, avgLon)
    }

    /**
     * حساب المسافة التراكمية لمسار خطي (Polyline) بالمتر.
     */
    public fun calculatePolylineDistanceMeters(points: List<GeoCoordinate>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineDistanceMeters(points[i], points[i + 1])
        }
        return total
    }

    /**
     * فحص هل تقع النقطة داخل مضلع جغرافي (Ray-Casting Algorithm).
     */
    public fun isPointInPolygon(point: GeoCoordinate, polygon: List<GeoCoordinate>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].latitude
            val yi = polygon[i].longitude
            val xj = polygon[j].latitude
            val yj = polygon[j].longitude

            val intersect = ((yi > point.longitude) != (yj > point.longitude)) &&
                    (point.latitude < (xj - xi) * (point.longitude - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mercator Tile Computations
    // ──────────────────────────────────────────────────────────────────────────

    public fun lonToTileX(lon: Double, zoom: Double): Double =
        ((lon + 180.0) / 360.0) * 2.0.pow(zoom)

    public fun latToTileY(lat: Double, zoom: Double): Double {
        val latRad = toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return (1.0 - asinh(tan(latRad)) / PI) / 2.0 * 2.0.pow(zoom)
    }

    public fun tileXToLon(x: Double, zoom: Double): Double =
        x / 2.0.pow(zoom) * 360.0 - 180.0

    public fun tileYToLat(y: Double, zoom: Double): Double {
        val n = PI - 2.0 * PI * y / 2.0.pow(zoom)
        return toDegrees(atan(sinh(n)))
    }

    public fun clampGeoCoordinate(lat: Double, lon: Double): GeoCoordinate {
        require(lat.isFinite() && lon.isFinite()) { "NON_FINITE_COORDINATE" }
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val remainder = lon % 360.0
        val clampedLon = when { remainder > 180.0 -> remainder - 360.0; remainder < -180.0 -> remainder + 360.0; else -> remainder }
        return GeoCoordinate(clampedLat, clampedLon)
    }

    /**
     * حساب المساحة الجيوديسية الحقيقية للمضلع بالمتر المربع باستخدام الإسقاط المستوي وصيغة Shoelace
     */
    public fun calculateGeodesicPolygonAreaSqMeters(points: List<GeoCoordinate>): Double {
        if (points.size < 3) return 0.0
        val centerLat = points.map { it.latitude }.average()
        val centerLon = points.map { it.longitude }.average()
        val r = 6378137.0 // WGS84 Earth equatorial radius in meters
        val latRad = toRadians(centerLat)
        val cosLat = cos(latRad)

        val projected = points.map { p ->
            val x = r * toRadians(p.longitude - centerLon) * cosLat
            val y = r * toRadians(p.latitude - centerLat)
            Pair(x, y)
        }

        var area = 0.0
        val n = projected.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += projected[i].first * projected[j].second
            area -= projected[j].first * projected[i].second
        }
        return abs(area) / 2.0
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Geohash Encoding
    // ──────────────────────────────────────────────────────────────────────────

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    public fun encodeGeoHash(latitude: Double, longitude: Double, precision: Int = 8): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val geohash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2
                if (longitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = mid
                } else {
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }
}
