package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate
import kotlinx.serialization.json.*

public sealed interface Geometry {
    public data class Point(public val coordinate: GeoCoordinate) : Geometry
    public data class LineString(public val coordinates: List<GeoCoordinate>) : Geometry
    public data class Polygon(public val rings: List<List<GeoCoordinate>>) : Geometry
}
public data class GeoFeature(
    public val geometry: Geometry?,
    public val properties: JsonObject = JsonObject(emptyMap()),
    public val id: String? = null,
    public val timestampMillis: Long = 0L,
)
public object GeoJson {
    public fun write(features: Sequence<GeoFeature>, append: (String) -> Unit) {
        append("{\"type\":\"FeatureCollection\",\"features\":[")
        var first = true
        features.forEach { feature ->
            if (!first) append(",")
            first = false
            append(buildJsonObject {
                put("type", "Feature")
                feature.id?.let { put("id", it) }
                put("geometry", feature.geometry?.json() ?: JsonNull)
                put("properties", feature.properties)
            }.toString())
        }
        append("]}")
    }
    public fun read(text: String, maxChars: Int = 32 * 1024 * 1024, maxFeatures: Int = 100_000, maxDepth: Int = 64): List<GeoFeature> {
        require(maxChars > 0 && text.length <= maxChars && maxFeatures > 0 && maxDepth > 0) { "GEOJSON_INPUT_LIMIT" }
        checkDepth(text, maxDepth)
        val document = Json.parseToJsonElement(text).jsonObject
        val entries = when (document["type"]?.jsonPrimitive?.content) {
            "FeatureCollection" -> document["features"]?.jsonArray ?: error("GEOJSON_FEATURES_MISSING")
            "Feature" -> JsonArray(listOf(document))
            else -> error("GEOJSON_DOCUMENT_UNSUPPORTED")
        }
        require(entries.size <= maxFeatures) { "GEOJSON_FEATURE_LIMIT" }
        return entries.map { entry ->
            val feature = entry.jsonObject
            require(feature["type"]?.jsonPrimitive?.content == "Feature") { "GEOJSON_FEATURE_INVALID" }
            val geometry = feature["geometry"]?.takeUnless { it == JsonNull }?.jsonObject?.let { objectValue ->
                val coordinates = objectValue["coordinates"] ?: error("GEOJSON_COORDINATES_MISSING")
                when (objectValue["type"]?.jsonPrimitive?.content) {
                    "Point" -> Geometry.Point(coordinate(coordinates.jsonArray))
                    "LineString" -> Geometry.LineString(coordinates.jsonArray.map { coordinate(it.jsonArray) }.also { require(it.size >= 2) })
                    "Polygon" -> Geometry.Polygon(coordinates.jsonArray.map { ring -> ring.jsonArray.map { coordinate(it.jsonArray) }.also { require(it.size >= 4 && it.first() == it.last()) } }.also { require(it.isNotEmpty()) })
                    else -> error("GEOJSON_GEOMETRY_UNSUPPORTED")
                }
            }
            GeoFeature(geometry, feature["properties"]?.takeUnless { it == JsonNull }?.jsonObject ?: JsonObject(emptyMap()), feature["id"]?.jsonPrimitive?.contentOrNull)
        }
    }
    private fun checkDepth(text: String, limit: Int) {
        var depth = 0
        var quoted = false
        var escaped = false
        text.forEach { c ->
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= limit) { "GEOJSON_DEPTH_LIMIT" } }
                '}', ']' -> depth--
            }
        }
    }
    private fun coordinate(values: JsonArray): GeoCoordinate {
        require(values.size >= 2) { "GEOJSON_COORDINATE_INVALID" }
        return GeoCoordinate(values[1].jsonPrimitive.double, values[0].jsonPrimitive.double, values.getOrNull(2)?.jsonPrimitive?.double ?: 0.0)
            .also { require(it.isValid && it.altitude.isFinite()) { "GEOJSON_COORDINATE_INVALID" } }
    }
    private fun GeoCoordinate.json(): JsonArray {
        require(isValid && altitude.isFinite()) { "GEOJSON_COORDINATE_INVALID" }
        val horizontal = listOf(JsonPrimitive(longitude), JsonPrimitive(latitude))
        return JsonArray(if (altitude == 0.0) horizontal else horizontal + JsonPrimitive(altitude))
    }
    private fun Geometry.json(): JsonObject = buildJsonObject {
        when (val geometry = this@json) {
            is Geometry.Point -> { put("type", "Point"); put("coordinates", geometry.coordinate.json()) }
            is Geometry.LineString -> { require(geometry.coordinates.size >= 2); put("type", "LineString"); put("coordinates", JsonArray(geometry.coordinates.map { it.json() })) }
            is Geometry.Polygon -> {
                require(geometry.rings.isNotEmpty())
                put("type", "Polygon")
                put("coordinates", JsonArray(geometry.rings.map { ring ->
                    require(ring.size >= 3)
                    val closed = if (ring.first() == ring.last()) ring else ring + ring.first()
                    JsonArray(closed.map { it.json() })
                }))
            }
        }
    }
}
