package org.connecttag.lib.gis

import kotlin.test.*
import kotlinx.serialization.json.*
import org.connecttag.lib.geo.GeoCoordinate

class GisContractTest {
    @Test fun geoJsonEscapesTextAndPreservesNullGeometryAndZeroCoordinates() {
        val output = StringBuilder()
        GeoJson.write(sequenceOf(
            GeoFeature(Geometry.Point(GeoCoordinate(0.0,0.0)),buildJsonObject { put("name","A \"quote\"\nB") }),
            GeoFeature(null),
        )) { output.append(it) }
        val decoded = GeoJson.read(output.toString())
        assertEquals(2,decoded.size)
        assertEquals(0.0,(decoded[0].geometry as Geometry.Point).coordinate.latitude)
        assertNull(decoded[1].geometry)
        assertEquals("A \"quote\"\nB",decoded[0].properties["name"]?.jsonPrimitive?.content)
    }
    @Test fun kmlDoesNotResolveExternalDeclarationsAndEscapesOutput() {
        assertFailsWith<IllegalArgumentException> { KmlParser.parseKmlText("<!DOCTYPE kml [<!ENTITY x SYSTEM 'file:///secret'>]><kml/>") }
        val output = StringBuilder()
        KmlWriter.write("A&B",sequenceOf(GeoFeature(Geometry.Point(GeoCoordinate(15.0,44.0)),buildJsonObject { put("name","<name>") }))) { output.append(it) }
        assertTrue(output.contains("A&amp;B"))
        assertEquals("<name>",KmlParser.parseKmlText(output.toString()).features.single().name)
    }
    @Test fun geoJsonPreservesAltitudeAndRejectsExcessiveNestingBeforeParsing() {
        val output = StringBuilder()
        GeoJson.write(sequenceOf(GeoFeature(Geometry.Point(GeoCoordinate(15.0,44.0,125.5))))) { output.append(it) }
        assertEquals(125.5,(GeoJson.read(output.toString()).single().geometry as Geometry.Point).coordinate.altitude)
        assertFailsWith<IllegalArgumentException> { GeoJson.read("[".repeat(65)+"]".repeat(65)) }
    }
    @Test fun suppliedFoldersAndStylesStayOutsideTheGenericFeatureModel() {
        val output=StringBuilder()
        val options=KmlWriteOptions(styles=listOf(KmlStyleDefinition("track",lineColor=0xFF123456.toInt(),lineWidth=4.0)),
            folderName={"A&B"},styleId={"track"})
        KmlWriter.writeStyled("Document",sequenceOf(GeoFeature(Geometry.Point(GeoCoordinate(0.0,0.0)))),options) { output.append(it) }
        assertTrue(output.contains("<Folder><name>A&amp;B</name>"))
        assertTrue(output.contains("<styleUrl>#track</styleUrl>"))
        assertTrue(output.contains("<color>ff563412</color>"))
    }
}
