package org.connecttag.lib.gis.android

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Standard GeoPackage header and WKB Point, ordered longitude then latitude. */
object GeoPackageGeometry {
    fun point(latitude: Double?, longitude: Double?): ByteArray? {
        if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) return null
        return ByteBuffer.allocate(29).order(ByteOrder.LITTLE_ENDIAN)
            .put(0x47.toByte()).put(0x50.toByte()).put(0.toByte()).put(1.toByte())
            .putInt(4326).put(1.toByte()).putInt(1).putDouble(longitude).putDouble(latitude).array()
    }
}
