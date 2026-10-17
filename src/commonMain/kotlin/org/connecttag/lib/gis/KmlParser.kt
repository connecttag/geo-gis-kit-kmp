package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate

/** Bounded, non-resolving reader for Point, LineString and exterior Polygon KML. */
public object KmlParser {
    private data class KmlStyle(val strokeColor: Int = 0xFF2563EB.toInt(), val strokeWidth: Float = 3f, val fillColor: Int = 0x332563EB)
    public fun parseKmlText(
        xml: String,
        sourceFileName: String = "",
        unnamedFeature: (Int) -> String = { "feature_$it" },
    ): KmlDocument {
        require(xml.length <= 32 * 1024 * 1024) { "KML_INPUT_LIMIT" }
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(xml)) { "KML_EXTERNAL_DECLARATION" }
        require(xml.contains("<") && xml.contains(">")) { "KML_DOCUMENT_INVALID" }
        val docName = extractTagContent(xml, "Document")?.let { extractTagContent(it, "name") }
            ?: extractTagContent(xml, "name")
            ?: sourceFileName.substringBeforeLast(".")

        val stylesMap = extractStyles(xml)
        val placemarksXml = extractAllBlocks(xml, "Placemark")
        val features = mutableListOf<KmlFeature>()

        val allLats = mutableListOf<Double>()
        val allLons = mutableListOf<Double>()

        for ((index, placemarkXml) in placemarksXml.withIndex()) {
            val name = extractTagContent(placemarkXml, "name")?.let { cleanCData(it).trim() }
                ?: unnamedFeature(index + 1)
            val description = extractTagContent(placemarkXml, "description")?.let { cleanCData(it).trim() } ?: ""
            val styleUrl = extractTagContent(placemarkXml, "styleUrl")?.trim()?.removePrefix("#")
            val style = (if (styleUrl != null) stylesMap[styleUrl] else null) ?: KmlStyle()

            // 1. فحص المضلع (Polygon)
            val polygonXml = extractTagContent(placemarkXml, "Polygon")
            if (polygonXml != null) {
                val coordsXml = extractTagContent(polygonXml, "coordinates")
                if (coordsXml != null) {
                    val coords = parseCoordinatesString(coordsXml)
                    if (coords.size >= 3) {
                        features.add(
                            KmlFeature(
                                id = "poly_${index}_${name.hashCode()}",
                                name = name,
                                description = description,
                                type = KmlFeatureType.POLYGON,
                                coordinates = coords,
                                strokeColor = style.strokeColor,
                                fillColor = style.fillColor,
                                strokeWidth = style.strokeWidth
                            )
                        )
                        coords.forEach { allLats.add(it.latitude); allLons.add(it.longitude) }
                        continue
                    }
                }
            }

            // 2. فحص المسار / الخط (LineString)
            val lineXml = extractTagContent(placemarkXml, "LineString")
            if (lineXml != null) {
                val coordsXml = extractTagContent(lineXml, "coordinates")
                if (coordsXml != null) {
                    val coords = parseCoordinatesString(coordsXml)
                    if (coords.size >= 2) {
                        features.add(
                            KmlFeature(
                                id = "line_${index}_${name.hashCode()}",
                                name = name,
                                description = description,
                                type = KmlFeatureType.LINE_STRING,
                                coordinates = coords,
                                strokeColor = style.strokeColor,
                                fillColor = 0,
                                strokeWidth = style.strokeWidth.coerceAtLeast(3f)
                            )
                        )
                        coords.forEach { allLats.add(it.latitude); allLons.add(it.longitude) }
                        continue
                    }
                }
            }

            // 3. فحص النقطة (Point)
            val pointXml = extractTagContent(placemarkXml, "Point")
            if (pointXml != null) {
                val coordsXml = extractTagContent(pointXml, "coordinates")
                if (coordsXml != null) {
                    val coords = parseCoordinatesString(coordsXml)
                    if (coords.isNotEmpty()) {
                        features.add(
                            KmlFeature(
                                id = "point_${index}_${name.hashCode()}",
                                name = name,
                                description = description,
                                type = KmlFeatureType.POINT,
                                coordinates = coords.take(1),
                                strokeColor = style.strokeColor,
                                fillColor = style.strokeColor,
                                strokeWidth = style.strokeWidth
                            )
                        )
                        allLats.add(coords[0].latitude)
                        allLons.add(coords[0].longitude)
                        continue
                    }
                }
            }
        }

        val boundingBox = if (allLats.isNotEmpty() && allLons.isNotEmpty()) {
            val maxLat = allLats.maxOrNull() ?: 15.5
            val minLat = allLats.minOrNull() ?: 15.0
            val maxLon = allLons.maxOrNull() ?: 44.5
            val minLon = allLons.minOrNull() ?: 44.0
            GeoBounds(maxLat, maxLon, minLat, minLon)
        } else null

        return KmlDocument(
            name = cleanCData(docName).ifBlank { sourceFileName.substringBeforeLast(".") },
            features = features,
            boundingBox = boundingBox,
        )
    }

    private fun extractStyles(xml: String): Map<String, KmlStyle> {
        val styles = mutableMapOf<String, KmlStyle>()
        val styleRegex = Regex("""<Style\s+id=["']([^"']+)["'][^>]*>(.*?)</Style>""", RegexOption.DOT_MATCHES_ALL)
        val matches = styleRegex.findAll(xml)

        for (match in matches) {
            val styleId = match.groupValues[1]
            val content = match.groupValues[2]

            var strokeColor = 0xFF2563EB.toInt()
            var strokeWidth = 3f
            var fillColor = 0x332563EB

            val lineStyle = extractTagContent(content, "LineStyle")
            if (lineStyle != null) {
                val colorStr = extractTagContent(lineStyle, "color")
                if (colorStr != null) {
                    strokeColor = parseKmlColor(colorStr, strokeColor)
                }
                val widthStr = extractTagContent(lineStyle, "width")
                if (widthStr != null) {
                    strokeWidth = widthStr.trim().toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: strokeWidth
                }
            }

            val polyStyle = extractTagContent(content, "PolyStyle")
            if (polyStyle != null) {
                val colorStr = extractTagContent(polyStyle, "color")
                if (colorStr != null) {
                    fillColor = parseKmlColor(colorStr, (strokeColor and 0x00FFFFFF) or 0x33000000)
                } else {
                    fillColor = (strokeColor and 0x00FFFFFF) or 0x33000000
                }
            }

            styles[styleId] = KmlStyle(strokeColor, strokeWidth, fillColor)
        }
        return styles
    }

    /**
     * تحويل ألوان KML من صيغة aabbggrr إلى لون أندرويد القياسي ARGB (aarrggbb)
     */
    public fun parseKmlColor(hex: String, defaultColor: Int): Int {
        val clean = hex.trim().removePrefix("#")
        return try {
            if (clean.length == 8) {
                val a = clean.substring(0, 2).toInt(16)
                val b = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val r = clean.substring(6, 8).toInt(16)
                argb(a, r, g, b)
            } else if (clean.length == 6) {
                val b = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val r = clean.substring(4, 6).toInt(16)
                argb(255, r, g, b)
            } else {
                defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }

    private fun parseCoordinatesString(coordsText: String): List<GeoCoordinate> {
        val list = mutableListOf<GeoCoordinate>()
        val tokens = cleanCData(coordsText).trim().split(Regex("""[\s\n\r\t]+"""))
        require(tokens.size <= 1_000_000) { "KML_COORDINATE_LIMIT" }
        for (token in tokens) {
            if (token.isBlank()) continue
            val parts = token.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].trim().toDoubleOrNull()
                val lat = parts[1].trim().toDoubleOrNull()
                val altitude = if (parts.size >= 3) parts[2].trim().toDoubleOrNull() else 0.0
                if (lat != null && lon != null && altitude != null && altitude.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    list.add(GeoCoordinate(lat, lon, altitude))
                }
            }
        }
        return list
    }

    private fun extractTagContent(xml: String, tag: String): String? {
        val prefix = "(?:[A-Za-z_][A-Za-z0-9_.-]*:)?"
        val pattern = Regex("<$prefix$tag(?:\\s+[^>]*)?>(.*?)</$prefix$tag\\s*>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(xml)?.groupValues?.get(1)
    }

    private fun extractAllBlocks(xml: String, tag: String): List<String> {
        val prefix = "(?:[A-Za-z_][A-Za-z0-9_.-]*:)?"
        val pattern = Regex("<$prefix$tag(?:\\s+[^>]*)?>(.*?)</$prefix$tag\\s*>", RegexOption.DOT_MATCHES_ALL)
        val blocks = pattern.findAll(xml).take(100_001).map { it.groupValues[1] }.toList()
        require(blocks.size <= 100_000) { "KML_FEATURE_LIMIT" }
        return blocks
    }

    private fun cleanCData(text: String): String {
        return text.replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
    }
    private fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b
}
