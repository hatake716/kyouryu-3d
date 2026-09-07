package io.github.hatake716.dinobones

import io.github.hatake716.dinobones.model.*
import io.github.hatake716.dinobones.data.Catalog
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.net.URI

class ModelTest {
    @Test fun everyRequestedAnimalHasDistinctFiniteBoundedGeometry(){
        val signatures=mutableSetOf<Int>()
        for(id in SkeletonFactory.ids){
            val mesh=SkeletonFactory.create(id)
            assertTrue(id,mesh.vertexCount>10000)
            assertEquals(0,mesh.vertexCount%3)
            assertTrue(id,mesh.vertices.all{it.isFinite()})
            assertTrue(id,mesh.radius in 3.9f..6f)
            assertTrue(id,mesh.lines.map{it.group}.containsAll(listOf(1,2,3,4,5,6)))
            assertTrue(id,mesh.lines.all{it.radius>0f && it.a.length()<=6f && it.b.length()<=6f})
            signatures.add(mesh.vertices.contentHashCode())
        }
        assertEquals(13,signatures.size)
    }
    @Test fun cameraCannotCrossPolesOrZoomThroughModel(){
        assertEquals(89f,CameraPose().orbit(100000f,100000f).pitch)
        assertEquals(-89f,CameraPose().orbit(0f,-100000f).pitch)
        assertEquals(5f,CameraPose().scale(100f).zoom)
        assertEquals(.55f,CameraPose().scale(.0001f).zoom)
        assertEquals(6f,CameraPose().pan(100f,100f).panX)
    }
    @Test fun catalogCoversAllModelsWithAttributionAndWorkingUriSyntax(){
        val catalog=Catalog.parse(File("src/main/assets/dinosaurs.json").readText())
        assertEquals(SkeletonFactory.ids.toSet(),catalog.map{it.id}.toSet())
        assertEquals(13,catalog.size)
        for(d in catalog){
            assertTrue(d.name,d.startMa>d.endMa)
            assertTrue(d.name,d.sections.size>=8)
            assertTrue(d.name,d.sections.all{it.text.length>30})
            assertTrue(d.name,d.sources.any{it.url.contains("action=history")})
            ((d.sources+d.videos).map{it.url}+d.wikipedia+d.youtube).forEach{assertEquals("https",URI(it).scheme);assertNotNull(URI(it).host)}
        }
        assertTrue(catalog.first{it.id=="tyrannosaurus"}.matches("てぃらの"))
        assertTrue(catalog.first{it.id=="velociraptor"}.matches("ｳﾞｪﾛｷﾗﾌﾟﾄﾙ"))
        assertEquals("海生爬虫類",catalog.first{it.id=="mosasaurus"}.category)
        assertEquals("翼竜類",catalog.first{it.id=="pteranodon"}.category)
    }
}
