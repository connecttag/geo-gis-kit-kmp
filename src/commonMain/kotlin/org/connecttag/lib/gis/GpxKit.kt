package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate

/**
 * بيانات نقطة مسار أو علامة جغرافية في ملف GPX (1.1).
 */
public data class GpxWaypoint(
    public val coordinate: GeoCoordinate,
    public val name: String? = null,
    public val description: String? = null,
    public val elevationMeters: Double? = null,
    public val timestampIso: String? = null,
    public val speedMps: Double? = null
)

/**
 * مقطع مسار أو مسار متصل في ملف GPX.
 */
public data class GpxTrackSegment(
    public val points: List<GpxWaypoint> = emptyList()
)

/**
 * مسار GPX (Track).
 */
public data class GpxTrack(
    public val name: String? = null,
    public val description: String? = null,
    public val segments: List<GpxTrackSegment> = emptyList()
)

/**
 * مستند GPX كامل.
 */
public data class GpxDocument(
    public val name: String? = null,
    public val description: String? = null,
    public val author: String? = null,
    public val waypoints: List<GpxWaypoint> = emptyList(),
    public val tracks: List<GpxTrack> = emptyList()
)

/**
 * قارئ ومحلل ملفات GPX (GPS Exchange Format 1.1) متعدد المنصات (Pure KMP).
 */
public object GpxParser {

    public fun parse(xml: String, maxChars: Int = 32 * 1024 * 1024): GpxDocument {
        require(xml.length <= maxChars) { "GPX_INPUT_LIMIT" }
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(xml)) { "GPX_EXTERNAL_DECLARATION" }
        require(xml.contains("<gpx", ignoreCase = true) || xml.contains("</gpx>", ignoreCase = true)) { "GPX_DOCUMENT_INVALID" }

        val docName = extractTag(xml, "metadata")?.let { extractTag(it, "name") } ?: extractTag(xml, "name")
        val docDesc = extractTag(xml, "metadata")?.let { extractTag(it, "desc") } ?: extractTag(xml, "desc")
        val docAuthor = extractTag(xml, "metadata")?.let { extractTag(it, "author") }

        // 1. استخراج العلامات الجغرافية الفردية (Waypoints)
        val waypoints = extractBlocks(xml, "wpt").mapNotNull { parseWaypoint(it) }

        // 2. استخراج المسارات (Tracks)
        val tracks = extractBlocks(xml, "trk").map { trkXml ->
            val trkName = extractTag(trkXml, "name")
            val trkDesc = extractTag(trkXml, "desc")
            val segments = extractBlocks(trkXml, "trkseg").map { segXml ->
                val points = extractBlocks(segXml, "trkpt").mapNotNull { parseWaypoint(it) }
                GpxTrackSegment(points)
            }
            GpxTrack(trkName, trkDesc, segments)
        }

        return GpxDocument(
            name = docName,
            description = docDesc,
            author = docAuthor,
            waypoints = waypoints,
            tracks = tracks
        )
    }

    private fun parseWaypoint(xml: String): GpxWaypoint? {
        val latMatch = Regex("""lat\s*=\s*["']([^"']+)["']""").find(xml) ?: return null
        val lonMatch = Regex("""lon\s*=\s*["']([^"']+)["']""").find(xml) ?: return null

        val lat = latMatch.groupValues[1].toDoubleOrNull() ?: return null
        val lon = lonMatch.groupValues[1].toDoubleOrNull() ?: return null
        val ele = extractTag(xml, "ele")?.toDoubleOrNull()
        val time = extractTag(xml, "time")
        val speed = extractTag(xml, "speed")?.toDoubleOrNull()
        val name = extractTag(xml, "name")
        val desc = extractTag(xml, "desc")

        val coordinate = GeoCoordinate(lat, lon, ele ?: 0.0)
        if (!coordinate.isValid) return null

        return GpxWaypoint(
            coordinate = coordinate,
            name = name,
            description = desc,
            elevationMeters = ele,
            timestampIso = time,
            speedMps = speed
        )
    }

    private fun extractTag(xml: String, tag: String): String? {
        val regex = Regex("""<$tag(?:\s[^>]*)?>([\s\S]*?)</$tag>""", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun extractBlocks(xml: String, tag: String): List<String> {
        val regex = Regex("""<$tag(?:\s[^>]*)?>([\s\S]*?)</$tag>""", RegexOption.IGNORE_CASE)
        return regex.findAll(xml).map { it.value }.toList()
    }
}

/**
 * كاتب ومولد ملفات GPX (GPS Exchange Format 1.1) متعدد المنصات (Pure KMP).
 */
public object GpxWriter {

    public fun write(document: GpxDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="ConnectTag Geo-GIS Kit" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")

        sb.append("  <metadata>\n")
        document.name?.let { sb.append("    <name>").append(escapeXml(it)).append("</name>\n") }
        document.description?.let { sb.append("    <desc>").append(escapeXml(it)).append("</desc>\n") }
        sb.append("  </metadata>\n")

        for (wpt in document.waypoints) {
            writeWaypoint(sb, "wpt", wpt)
        }

        for (trk in document.tracks) {
            sb.append("  <trk>\n")
            trk.name?.let { sb.append("    <name>").append(escapeXml(it)).append("</name>\n") }
            trk.description?.let { sb.append("    <desc>").append(escapeXml(it)).append("</desc>\n") }
            for (seg in trk.segments) {
                sb.append("    <trkseg>\n")
                for (pt in seg.points) {
                    writeWaypoint(sb, "trkpt", pt, indent = "      ")
                }
                sb.append("    </trkseg>\n")
            }
            sb.append("  </trk>\n")
        }

        sb.append("</gpx>")
        return sb.toString()
    }

    private fun writeWaypoint(sb: StringBuilder, tag: String, wpt: GpxWaypoint, indent: String = "  ") {
        sb.append(indent).append("<").append(tag)
        sb.append(" lat=\"").append(wpt.coordinate.latitude).append("\"")
        sb.append(" lon=\"").append(wpt.coordinate.longitude).append("\">\n")
        wpt.elevationMeters?.let { sb.append(indent).append("  <ele>").append(it).append("</ele>\n") }
        wpt.timestampIso?.let { sb.append(indent).append("  <time>").append(it).append("</time>\n") }
        wpt.name?.let { sb.append(indent).append("  <name>").append(escapeXml(it)).append("</name>\n") }
        wpt.description?.let { sb.append(indent).append("  <desc>").append(escapeXml(it)).append("</desc>\n") }
        wpt.speedMps?.let { sb.append(indent).append("  <speed>").append(it).append("</speed>\n") }
        sb.append(indent).append("</").append(tag).append(">\n")
    }

    private fun escapeXml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
