package org.connecttag.lib.gis

import org.connecttag.lib.geo.GeoCoordinate

public enum class KmlFeatureType { POLYGON, LINE_STRING, POINT }
public data class KmlFeature(
    public val id: String,
    public val name: String,
    public val description: String = "",
    public val type: KmlFeatureType,
    public val coordinates: List<GeoCoordinate>,
    public val strokeColor: Int = 0xFF2563EB.toInt(),
    public val fillColor: Int = 0x332563EB,
    public val strokeWidth: Float = 3f,
)
public data class GeoBounds(public val north: Double, public val east: Double, public val south: Double, public val west: Double)
public data class KmlDocument(public val name: String, public val features: List<KmlFeature>, public val boundingBox: GeoBounds?)
