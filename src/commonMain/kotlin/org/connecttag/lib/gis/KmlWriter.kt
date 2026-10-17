package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate
import kotlinx.serialization.json.*

/** Presentation choices supplied by the consuming application; no renderer or bundled branding. */
public data class KmlStyleDefinition(
    public val id: String,
    public val iconHref: String? = null,
    public val iconScale: Double? = null,
    public val iconColor: Int? = null,
    public val lineColor: Int? = null,
    public val lineWidth: Double? = null,
    public val fillColor: Int? = null,
)
public data class KmlWriteOptions(
    public val description: String? = null,
    public val styles: List<KmlStyleDefinition> = emptyList(),
    /** Consecutive features with the same folder name share a folder without buffering. */
    public val folderName: (GeoFeature) -> String? = { null },
    public val styleId: (GeoFeature) -> String? = { null },
    public val descriptionText: (GeoFeature) -> String? = { null },
)
public object KmlWriter {
    public fun write(title: String, features: Sequence<GeoFeature>, append: (String) -> Unit) {
        writeStyled(title, features, KmlWriteOptions(), append)
    }
    public fun writeStyled(title: String, features: Sequence<GeoFeature>, options: KmlWriteOptions, append: (String) -> Unit) {
        val styleIds = options.styles.map { it.id }.toSet()
        require(styleIds.size == options.styles.size) { "KML_DUPLICATE_STYLE" }
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>" + escape(title) + "</name>")
        options.description?.let { append("<description>" + escape(it) + "</description>") }
        options.styles.forEach { writeStyle(it, append) }
        var currentFolder: String? = null
        features.forEach { feature ->
            val geometry = feature.geometry ?: return@forEach
            val folder = options.folderName(feature)?.takeIf { it.isNotBlank() }
            if (folder != currentFolder) {
                if (currentFolder != null) append("</Folder>")
                if (folder != null) append("<Folder><name>" + escape(folder) + "</name>")
                currentFolder = folder
            }
            append("<Placemark>")
            options.styleId(feature)?.let { id ->
                require(id in styleIds) { "KML_STYLE_MISSING" }
                append("<styleUrl>#" + escape(id) + "</styleUrl>")
            }
            feature.id?.let { append("<ExtendedData><Data name=\"id\"><value>" + escape(it) + "</value></Data></ExtendedData>") }
            val name = feature.properties["name"]?.jsonPrimitive?.contentOrNull ?: feature.id.orEmpty()
            val description = options.descriptionText(feature) ?: feature.properties["description"]?.jsonPrimitive?.contentOrNull ?: feature.properties.toString()
            append("<name>" + escape(name) + "</name><description>" + escape(description) + "</description>")
            when (geometry) {
                is Geometry.Point -> append("<Point><coordinates>" + coordinate(geometry.coordinate) + "</coordinates></Point>")
                is Geometry.LineString -> { require(geometry.coordinates.size >= 2); append("<LineString><coordinates>" + geometry.coordinates.joinToString(" ") { coordinate(it) } + "</coordinates></LineString>") }
                is Geometry.Polygon -> {
                    require(geometry.rings.isNotEmpty())
                    append("<Polygon>")
                    geometry.rings.forEachIndexed { index, ring ->
                        require(ring.size >= 3)
                        val closed = if (ring.first() == ring.last()) ring else ring + ring.first()
                        val boundary = if (index == 0) "outerBoundaryIs" else "innerBoundaryIs"
                        append("<" + boundary + "><LinearRing><coordinates>" + closed.joinToString(" ") { coordinate(it) } + "</coordinates></LinearRing></" + boundary + ">")
                    }
                    append("</Polygon>")
                }
            }
            append("</Placemark>")
        }
        if (currentFolder != null) append("</Folder>")
        append("</Document></kml>")
    }
    private fun writeStyle(style: KmlStyleDefinition, append: (String) -> Unit) {
        require(style.id.matches(Regex("[A-Za-z_][A-Za-z0-9_.-]{0,63}"))) { "KML_STYLE_INVALID" }
        style.iconScale?.let { require(it.isFinite() && it > 0) { "KML_SCALE_INVALID" } }
        style.lineWidth?.let { require(it.isFinite() && it > 0) { "KML_WIDTH_INVALID" } }
        append("<Style id=\"" + escape(style.id) + "\">")
        if (style.iconHref != null || style.iconScale != null || style.iconColor != null) {
            append("<IconStyle>")
            style.iconScale?.let { append("<scale>$it</scale>") }
            style.iconColor?.let { append("<color>" + color(it) + "</color>") }
            style.iconHref?.let { append("<Icon><href>" + escape(it) + "</href></Icon>") }
            append("</IconStyle>")
        }
        if (style.lineColor != null || style.lineWidth != null) {
            append("<LineStyle>")
            style.lineColor?.let { append("<color>" + color(it) + "</color>") }
            style.lineWidth?.let { append("<width>$it</width>") }
            append("</LineStyle>")
        }
        style.fillColor?.let { append("<PolyStyle><color>" + color(it) + "</color><outline>1</outline></PolyStyle>") }
        append("</Style>")
    }
    private fun color(argb: Int): String {
        val abgr = (argb and 0xFF00FF00.toInt()) or ((argb and 0xFF) shl 16) or ((argb ushr 16) and 0xFF)
        return abgr.toUInt().toString(16).padStart(8, '0')
    }
    public fun escape(value: String): String = buildString {
        value.forEach { c -> when(c) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;"); '"' -> append("&quot;"); '\'' -> append("&apos;")
            else -> { require(c >= ' ' || c in listOf('\n','\r','\t')) { "KML_INVALID_CHARACTER" }; append(c) }
        } }
    }
    private fun coordinate(value: GeoCoordinate): String {
        require(value.isValid && value.altitude.isFinite()) { "KML_COORDINATE_INVALID" }
        return "${value.longitude},${value.latitude},${value.altitude}"
    }
}
