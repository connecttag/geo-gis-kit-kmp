package org.connecttag.lib.geo

import kotlinx.serialization.Serializable

/** WGS84 latitude/longitude in degrees; altitude and horizontal accuracy in metres. */
@Serializable
public data class GeoCoordinate(
    public val latitude: Double,
    public val longitude: Double,
    public val altitude: Double = 0.0,
    public val accuracy: Float? = null,
) {
    public val isValid: Boolean get() = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
}
