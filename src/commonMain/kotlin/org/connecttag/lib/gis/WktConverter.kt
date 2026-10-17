package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate

/**
 * محول تنسيق WKT (Well-Known Text - OGC Standard) لتمثيل المعالم الجغرافية نصياً لـ KMP.
 */
public object WktConverter {

    public fun toWkt(geometry: Geometry): String = when (geometry) {
        is Geometry.Point -> "POINT (${geometry.coordinate.longitude} ${geometry.coordinate.latitude})"
        is Geometry.LineString -> {
            val coords = geometry.coordinates.joinToString(", ") { "${it.longitude} ${it.latitude}" }
            "LINESTRING ($coords)"
        }
        is Geometry.Polygon -> {
            val rings = geometry.rings.joinToString(", ") { ring ->
                val closed = if (ring.isNotEmpty() && ring.first() == ring.last()) ring else ring + ring.first()
                "(" + closed.joinToString(", ") { "${it.longitude} ${it.latitude}" } + ")"
            }
            "POLYGON ($rings)"
        }
    }

    public fun parseWkt(wkt: String): Geometry? {
        val trimmed = wkt.trim()
        val type = trimmed.substringBefore("(").trim().uppercase()
        val body = trimmed.substringAfter("(").substringBeforeLast(")").trim()

        return when (type) {
            "POINT" -> {
                val parts = body.split(Regex("""\s+""")).filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull() ?: return null
                    val lat = parts[1].toDoubleOrNull() ?: return null
                    Geometry.Point(GeoCoordinate(lat, lon))
                } else null
            }
            "LINESTRING" -> {
                val points = body.split(",").mapNotNull { coordStr ->
                    val parts = coordStr.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        val lon = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                        val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                        GeoCoordinate(lat, lon)
                    } else null
                }
                if (points.size >= 2) Geometry.LineString(points) else null
            }
            "POLYGON" -> {
                val rings = Regex("""\(([^)]+)\)""").findAll(body).map { ringMatch ->
                    ringMatch.groupValues[1].split(",").mapNotNull { coordStr ->
                        val parts = coordStr.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
                        if (parts.size >= 2) {
                            val lon = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                            val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                            GeoCoordinate(lat, lon)
                        } else null
                    }
                }.filter { it.size >= 3 }.toList()
                if (rings.isNotEmpty()) Geometry.Polygon(rings) else null
            }
            else -> null
        }
    }
}
