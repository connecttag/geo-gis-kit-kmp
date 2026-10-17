package org.connecttag.lib.gis.android

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import org.connecttag.lib.gis.*
import kotlinx.serialization.json.*

public enum class GeoColumnType { TEXT, INTEGER, REAL }
public data class GeoColumn(public val name: String, public val type: GeoColumnType)
public data class GeoPackageLayer(public val name: String, public val columns: List<GeoColumn>, public val features: Sequence<GeoFeature>)

/** Writes new, caller-owned files only. Version 1 supports nullable WGS84 point layers. */
public object GeoPackageWriter {
    public fun write(file: File, layers: List<GeoPackageLayer>) {
        require(!file.exists() || file.length() == 0L) { "GEOPACKAGE_TARGET_NOT_EMPTY" }
        require(layers.map { it.name.lowercase() }.distinct().size == layers.size) { "GEOPACKAGE_DUPLICATE_LAYER" }
        layers.forEach { layer ->
            identifier(layer.name)
            require(!layer.name.startsWith("gpkg_", ignoreCase = true)) { "GEOPACKAGE_RESERVED_LAYER" }
            require(layer.columns.map { it.name.lowercase() }.distinct().size == layer.columns.size)
            layer.columns.forEach { column ->
                identifier(column.name)
                require(column.name.lowercase() !in setOf("id", "geom", "latitude", "longitude", "timestamp")) { "GEOPACKAGE_RESERVED_COLUMN" }
            }
        }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("PRAGMA application_id = 1196444487")
            db.execSQL("PRAGMA user_version = 10300")
            db.beginTransaction()
            try {
                db.execSQL("""CREATE TABLE gpkg_spatial_ref_sys (
                    srs_name TEXT NOT NULL, srs_id INTEGER NOT NULL PRIMARY KEY,
                    organization TEXT NOT NULL, organization_coordsys_id INTEGER NOT NULL,
                    definition TEXT NOT NULL, description TEXT)""")
                db.execSQL("INSERT INTO gpkg_spatial_ref_sys VALUES ('Undefined Cartesian', -1, 'NONE', -1, 'undefined', NULL)")
                db.execSQL("INSERT INTO gpkg_spatial_ref_sys VALUES ('Undefined geographic', 0, 'NONE', 0, 'undefined', NULL)")
                db.execSQL("INSERT INTO gpkg_spatial_ref_sys VALUES ('WGS 84 geodetic', 4326, 'EPSG', 4326, ?, 'WGS 84')",
                    arrayOf("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]"))
                db.execSQL("""CREATE TABLE gpkg_contents (
                    table_name TEXT NOT NULL PRIMARY KEY, data_type TEXT NOT NULL,
                    identifier TEXT UNIQUE, description TEXT DEFAULT '',
                    last_change DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                    min_x DOUBLE, min_y DOUBLE, max_x DOUBLE, max_y DOUBLE,
                    srs_id INTEGER REFERENCES gpkg_spatial_ref_sys(srs_id))""")
                db.execSQL("""CREATE TABLE gpkg_geometry_columns (
                    table_name TEXT NOT NULL, column_name TEXT NOT NULL,
                    geometry_type_name TEXT NOT NULL, srs_id INTEGER NOT NULL,
                    z TINYINT NOT NULL, m TINYINT NOT NULL,
                    PRIMARY KEY(table_name,column_name), UNIQUE(table_name),
                    FOREIGN KEY(table_name) REFERENCES gpkg_contents(table_name),
                    FOREIGN KEY(srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id))""")

                layers.forEach { layer ->
                    val fields = layer.columns.joinToString("") { ", \"${it.name}\" ${it.type.name}" }
                    db.execSQL("CREATE TABLE \"${layer.name}\" (id INTEGER PRIMARY KEY AUTOINCREMENT, geom POINT, latitude DOUBLE, longitude DOUBLE, timestamp INTEGER$fields)")
                    db.execSQL("INSERT INTO gpkg_contents (table_name,data_type,identifier,srs_id) VALUES (?,'features',?,4326)", arrayOf(layer.name, layer.name))
                    db.execSQL("INSERT INTO gpkg_geometry_columns VALUES (?,'geom','POINT',4326,0,0)", arrayOf(layer.name))
                    layer.features.forEach { feature ->
                        require(feature.geometry == null || feature.geometry is Geometry.Point) { "GEOPACKAGE_GEOMETRY_UNSUPPORTED" }
                        val point = (feature.geometry as? Geometry.Point)?.coordinate
                        require(point == null || point.isValid) { "GEOPACKAGE_COORDINATE_INVALID" }
                        val geometry = GeoPackageGeometry.point(point?.latitude, point?.longitude)
                        val row = ContentValues().apply {
                            put("timestamp", feature.timestampMillis)
                            if (geometry == null) {
                                putNull("geom"); putNull("latitude"); putNull("longitude")
                            } else {
                                put("geom", geometry); put("latitude", point!!.latitude); put("longitude", point.longitude)
                            }
                            layer.columns.forEach { column ->
                                val value = feature.properties[column.name]
                                if (value == null || value == JsonNull) putNull(column.name)
                                else when(column.type) {
                                    GeoColumnType.TEXT -> put(column.name, (value as? JsonPrimitive)?.content ?: value.toString())
                                    GeoColumnType.INTEGER -> put(column.name, value.jsonPrimitive.longOrNull ?: error("GEOPACKAGE_INTEGER_INVALID"))
                                    GeoColumnType.REAL -> put(column.name, value.jsonPrimitive.doubleOrNull?.takeIf { it.isFinite() } ?: error("GEOPACKAGE_REAL_INVALID"))
                                }
                            }
                        }
                        db.insertOrThrow(layer.name, null, row)
                    }
                    db.execSQL("""UPDATE gpkg_contents SET
                        min_x=(SELECT MIN(longitude) FROM "${layer.name}" WHERE geom IS NOT NULL),
                        max_x=(SELECT MAX(longitude) FROM "${layer.name}" WHERE geom IS NOT NULL),
                        min_y=(SELECT MIN(latitude) FROM "${layer.name}" WHERE geom IS NOT NULL),
                        max_y=(SELECT MAX(latitude) FROM "${layer.name}" WHERE geom IS NOT NULL)
                        WHERE table_name=?""", arrayOf(layer.name))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }
    private fun identifier(value: String) { require(value.matches(Regex("[A-Za-z][A-Za-z0-9_]{0,62}"))) { "GEOPACKAGE_IDENTIFIER_INVALID" } }
}
