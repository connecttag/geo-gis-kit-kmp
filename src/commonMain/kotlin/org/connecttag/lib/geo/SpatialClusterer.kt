package org.connecttag.lib.geo

/**
 * نتيجة تجميع النقاط الجغرافية (عنصر مفرد أو تكتل).
 */
public sealed class SpatialClusterItem<T> {
    public abstract val center: GeoCoordinate
    public abstract val count: Int

    public data class Single<T>(
        val item: T,
        override val center: GeoCoordinate
    ) : SpatialClusterItem<T>() {
        override val count: Int = 1
    }

    public data class Cluster<T>(
        val items: List<T>,
        override val center: GeoCoordinate
    ) : SpatialClusterItem<T>() {
        override val count: Int = items.size
    }
}

/**
 * خوارزمية تجميع جغرافي متعددة المنصات (Pure Kotlin) لتكتيل أهداف الخريطة التكتيكية.
 */
public object SpatialClusterer {

    public fun <T> cluster(
        items: List<T>,
        getGeoCoordinate: (T) -> GeoCoordinate,
        maxDistanceMeters: Double = 150.0
    ): List<SpatialClusterItem<T>> {
        if (items.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<T>>()
        val clusterCenters = mutableListOf<GeoCoordinate>()

        for (item in items) {
            val point = getGeoCoordinate(item)
            var added = false

            for (i in clusterCenters.indices) {
                val center = clusterCenters[i]
                if (GeoMathUtils.haversineDistanceMeters(point, center) <= maxDistanceMeters) {
                    val clusterList = clusters[i]
                    clusterList.add(item)
                    // تحديث المركز بمتوسط الإحداثيات
                    val newLat = (center.latitude * (clusterList.size - 1) + point.latitude) / clusterList.size
                    val newLon = (center.longitude * (clusterList.size - 1) + point.longitude) / clusterList.size
                    clusterCenters[i] = GeoCoordinate(newLat, newLon)
                    added = true
                    break
                }
            }

            if (!added) {
                clusters.add(mutableListOf(item))
                clusterCenters.add(point)
            }
        }

        return clusters.mapIndexed { idx, list ->
            if (list.size == 1) {
                SpatialClusterItem.Single(list.first(), clusterCenters[idx])
            } else {
                SpatialClusterItem.Cluster(list, clusterCenters[idx])
            }
        }
    }
}
