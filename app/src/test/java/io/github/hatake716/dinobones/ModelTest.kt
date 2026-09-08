package io.github.hatake716.dinobones

import io.github.hatake716.dinobones.model.*
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.data.GeologicPeriod
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.net.URI

class ModelTest {
    @Test fun erasCoverEveryAnimalAndKeepBoundarySpanningGeneraInBothPeriods(){
        val catalog=Catalog.parse(File("src/main/assets/dinosaurs.json").readText())
        val groups=GeologicPeriod.entries.map{era->catalog.filter(era::contains).map{it.id}.toSet()}
        assertEquals(listOf(5,25,72),groups.map{it.size})
        assertEquals(catalog.map{it.id}.toSet(),groups.flatten().toSet())
        assertTrue("tyrannosaurus" in groups[2]);assertFalse("tyrannosaurus" in groups[1])
        assertTrue("stegosaurus" in groups[1]);assertTrue("herrerasaurus" in groups[0])
        assertTrue("ichthyosaurus" in groups[1]);assertFalse("ichthyosaurus" in groups[0])
        assertEquals(setOf("mamenchisaurus","pliosaurus"),groups[1].intersect(groups[2]))
    }
    @Test fun everyRequestedAnimalHasDistinctFiniteBoundedGeometry(){
        val signatures=mutableMapOf<Int,String>()
        for(id in SkeletonFactory.ids){
            val mesh=SkeletonFactory.create(id){File("src/main/assets",it).inputStream()}
            assertTrue(id,mesh.vertexCount>10000)
            assertEquals(0,mesh.vertexCount%3)
            assertTrue(id,mesh.vertices.all{it.isFinite()})
            assertTrue(id,mesh.radius in 3.9f..6f)
            assertTrue(id,mesh.groups.containsAll(listOf(1,2,3,4,5,6)))
            assertTrue(id,mesh.lines.all{it.radius>0f && it.a.length()<=6f && it.b.length()<=6f})
            val old=signatures.put(mesh.vertices.contentHashCode(),id)
            assertNull("Duplicate geometry: $id and $old",old)
        }
        assertEquals(100,signatures.size)
    }
    @Test fun specimenPreservesMetreBoundsAndSurfaceData(){
        val mesh=SpecimenLoader.load{File("src/main/assets",it).inputStream()}
        assertEquals(449994,mesh.vertexCount)
        assertEquals("specimens/triceratops",mesh.texturePrefix)
        val m=mesh.metresPerUnit!!
        val xyz=List(3){axis->(axis until mesh.vertices.size step 9).map{mesh.vertices[it]}}
        val expected=listOf(5.891464f,2.803518f,1.661696f)
        for(i in 0..2)assertEquals(expected[i],(xyz[i].max()-xyz[i].min())*m,.00001f)
        for(i in mesh.vertices.indices step 9){
            val normal=V(mesh.vertices[i+3],mesh.vertices[i+4],mesh.vertices[i+5])
            assertEquals(1f,normal.length(),.002f)
            assertTrue(mesh.vertices[i+7] in -.001f..1.001f);assertTrue(mesh.vertices[i+8] in -.001f..1.001f)
        }
    }
    @Test fun replicaScanKeepsSurfaceAndNeverClaimsAnUnverifiedPhysicalScale(){
        val scan=SpecimenLoader.load("tyrannosaurus"){File("src/main/assets",it).inputStream()}
        assertEquals(750720,scan.vertexCount)
        assertNull(scan.metresPerUnit)
        assertEquals("specimens/tyrannosaurus",scan.texturePrefix)
        assertEquals(setOf(1,2,3,4,5,6),scan.groups)
        assertTrue(scan.lines.isEmpty())
        for(i in scan.vertices.indices step 9){
            assertEquals(1f,V(scan.vertices[i+3],scan.vertices[i+4],scan.vertices[i+5]).length(),.002f)
            assertTrue(scan.vertices[i+7] in -.001f..1.001f);assertTrue(scan.vertices[i+8] in -.001f..1.001f)
        }
        assertTrue(ModelEvidence.note("tyrannosaurus").contains("Rigsters"))
        assertTrue(ModelEvidence.note("tyrannosaurus").contains("実寸換算を確認できない"))
    }
    @Test fun authoredReconstructionsKeepDetailedSurfacesAndNoInventedMetres(){
        for((id,count) in mapOf("carnotaurus" to 782562,"patagotitan" to 1050537)){
            val mesh=SpecimenLoader.load(id){File("src/main/assets",it).inputStream()}
            assertEquals(count,mesh.vertexCount);assertNull(mesh.metresPerUnit)
            assertEquals(setOf(1,2,3,4,5,6),mesh.groups)
            for(i in mesh.vertices.indices step 9)assertEquals(id,1f,V(mesh.vertices[i+3],mesh.vertices[i+4],mesh.vertices[i+5]).length(),.002f)
            assertTrue(ModelEvidence.note(id).contains("復元モデル"))
        }
        assertTrue(ModelEvidence.note("patagotitan").contains("CC BY-SA 4.0"))
    }
    @Test fun measuredLongBonesKeepTheirPublishedLengthsAfterDisplayNormalization(){
        assertEquals(19,ModelEvidence.limbs.size)
        for((id,m) in ModelEvidence.limbs){
            val mesh=SkeletonFactory.create(id);val unit=mesh.metresPerUnit!!
            val expected=mapOf("femur-right" to m.femur,"femur-left" to m.leftFemur.takeIf{it>0f}.let{it?:m.femur},
                "tibia-right" to m.tibia,"tibia-left" to m.leftTibia.takeIf{it>0f}.let{it?:m.tibia},
                "metatarsal-III" to m.metatarsal,"humerus" to m.humerus,"radius" to m.radius,"ulna" to m.ulna).filterValues{it>0f}
            assertEquals(id,expected.keys,mesh.lines.mapNotNull{it.measurement}.toSet())
            for(line in mesh.lines.filter{it.measurement!=null})assertEquals("$id ${line.measurement}",expected.getValue(line.measurement!!),(line.b-line.a).length()*unit,.00001f)
        }
        assertNull(SkeletonFactory.create("brachiosaurus").metresPerUnit)
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
        assertEquals(100,catalog.size)
        for(d in catalog){
            assertTrue(d.name,d.startMa>=d.endMa)
            assertTrue(d.name,d.sections.size>=6)
            assertEquals("名前の由来と命名",d.sections.first().title)
            assertTrue(d.name,d.sections.take(5).all{it.references.isNotEmpty()})
            assertTrue(d.name,d.sections.all{it.text.length>30})
            assertTrue(d.id,d.rank in 1..100 && d.pageviews>0)
            assertTrue(d.name,d.sources.any{it.url.contains("action=history")})
            ((d.sources+d.videos+d.sections.flatMap{it.references}).map{it.url}+d.wikipedia+d.youtube).forEach{assertEquals("https",URI(it).scheme);assertNotNull(URI(it).host)}
        }
        assertTrue(catalog.first{it.id=="tyrannosaurus"}.matches("てぃらの"))
        assertTrue(catalog.first{it.id=="velociraptor"}.matches("ｳﾞｪﾛｷﾗﾌﾟﾄﾙ"))
        assertEquals("海生爬虫類",catalog.first{it.id=="mosasaurus"}.category)
        assertEquals("翼竜類",catalog.first{it.id=="pteranodon"}.category)
    }
}
