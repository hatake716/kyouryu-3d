package io.github.hatake716.dinobones

import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.theater.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import kotlin.math.*

class TheaterWorldTest {
    @Test fun everyIllustrationHasItsOwnReachableStandAndDistinctTapTarget(){
        val catalog=Catalog.parse(File("src/main/assets/dinosaurs.json").readText()).associateBy{it.id}
        assertEquals(200,TheaterWorld.illustrationBoards.size)
        for(b in TheaterWorld.illustrationBoards){
            val art=checkNotNull(catalog.getValue(b.spot.id).artwork)
            assertTrue(File("src/main/assets",art.asset).isFile)
            assertTrue(art.artist.isNotBlank()&&art.source.startsWith("https://")&&art.license.isNotBlank())
            val candidates=listOf(2f,1.3f,2.7f,3.3f).flatMap{distance->val dz=if(b.back)-distance else distance
                listOf(0f,-.8f,.8f,-1.5f,1.5f).map{dx->WalkPose(b.x+dx,b.z+dz,atan2(-dx,dz)*180f/PI.toFloat(),atan2(TheaterWorld.EYE_HEIGHT-b.y-b.height/2,hypot(dx,dz))*180f/PI.toFloat())}}
            val p=candidates.firstOrNull{TheaterWorld.canStand(it.x,it.z)&&TheaterWorld.pickTarget(it,0f,0f,1f,emptyMap())==ExhibitHit(b.spot,true)}
            assertNotNull("Accessible ${b.spot.id} ${b.back}",p)
            assertFalse(TheaterWorld.canStand(b.x,b.z))
            for(aspect in listOf(.45f,2.2f)){
                val hit=TheaterWorld.pickTarget(p!!,0f,0f,aspect,emptyMap())
                assertEquals(b.spot.id,hit?.spot?.id);assertEquals(true,hit?.artwork)
            }
            val named=b.spot.arrival()
            assertEquals("Skeleton remains a record target: ${b.spot.id}",false,TheaterWorld.pickTarget(named,0f,0f,.45f,emptyMap())?.artwork)
        }
    }
    @Test fun everyExhibitHasOneReachablePositionAndAValidPickingRay(){
        val catalog=Catalog.parse(File("src/main/assets/dinosaurs.json").readText())
        assertEquals(100,TheaterWorld.spots.size)
        assertEquals(catalog.map{it.id}.toSet(),TheaterWorld.spots.map{it.id}.toSet())
        assertEquals(List(5){20},TheaterWorld.spots.groupBy{it.section}.values.map{it.size})
        for(s in TheaterWorld.spots){
            assertTrue(s.id,TheaterWorld.canStand(s.arrival().x,s.arrival().z))
            assertEquals(s.id,TheaterWorld.pick(s.arrival(),0f,0f,.45f,emptyMap())?.id)
            assertFalse("Cannot walk into ${s.id}",TheaterWorld.canStand(s.x,s.z))
        }
        // A visitor can walk the entire central aisle through all five galleries.
        var p=WalkPose(yaw=0f);repeat(ceil((p.z-TheaterWorld.BACK)/TheaterWorld.MOVE_SPEED/.1f).toInt()+10){p=TheaterWorld.move(p,0f,1f,.1f)}
        assertTrue(p.z<TheaterWorld.BACK+2f);assertEquals(4,TheaterWorld.section(p))
    }
    @Test fun movementRespectsCameraDirectionCollisionAndFiniteBounds(){
        val p=WalkPose(yaw=0f,pitch=0f)
        val f=TheaterWorld.move(p,0f,1f,.1f);assertTrue(f.z<p.z);assertEquals(p.x,f.x,0f)
        val r=TheaterWorld.move(p.copy(yaw=90f),0f,1f,.1f);assertTrue(r.x>p.x)
        val diagonal=TheaterWorld.move(p,1f,1f,.1f)
        assertEquals(hypot(f.x-p.x,f.z-p.z),hypot(diagonal.x-p.x,diagonal.z-p.z),.0001f)
        val partition=TheaterWorld.sectionFront(1)
        var wall=WalkPose(10f,partition+2f,0f,0f);repeat(40){wall=TheaterWorld.move(wall,0f,1f,.1f)}
        assertTrue("Partition stops movement",wall.z>partition+.9f)
        val trice=TheaterWorld.byId.getValue("triceratops")
        var stand=WalkPose(trice.x,trice.base.maxZ+1f,0f,0f)
        repeat(100){stand=TheaterWorld.move(stand,0f,1f,.1f)}
        assertTrue(stand.z>trice.base.maxZ+.3f)
        assertEquals(WalkPose(),TheaterWorld.safe(WalkPose(Float.NaN)))
        assertEquals(72f,p.look(720f,999f).pitch,0f)
        assertNull("Solid wall blocks exhibit beyond",TheaterWorld.pick(WalkPose(10f,partition+2f,0f,0f),0f,0f,1f,emptyMap()))
        assertTrue(TheaterWorld.move(p,0f,1f,Float.NaN).finite())
    }
    @Test fun allGalleryAssetsHaveValidIndicesUniqueGeometryAndVerifiedProvenance(){
        val entries=JSONArray(File("src/main/assets/theater/manifest.json").readText());assertEquals(100,entries.length())
        val digests=mutableSetOf<String>()
        for(i in 0 until entries.length()){
            val e=entries.getJSONObject(i);val file=File("src/main/assets/theater/${e.getString("id")}.tmsz")
            val digest=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)}
            assertEquals(e.getString("sha256"),digest);assertTrue(digests.add(digest))
            val displayHeight=e.getJSONObject("calibration").getDouble("baseMeters").toFloat()+(e.getJSONArray("max").getDouble(1)-e.getJSONArray("min").getDouble(1)).toFloat()*e.getDouble("scale").toFloat()
            val spot=TheaterWorld.byId.getValue(e.getString("id"));val arrival=spot.arrival(displayHeight)
            assertTrue(TheaterWorld.canStand(arrival.x,arrival.z));assertEquals(spot.id,TheaterWorld.pick(arrival,0f,0f,.45f,mapOf(spot.id to displayHeight))?.id)
            val mesh=TheaterMesh.read(file.inputStream());assertEquals(e.getInt("triangles"),mesh.triangles)
            assertTrue(mesh.bytes<8*1024*1024)
            val bounds=FloatArray(6){if(it<3)Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY}
            for(part in mesh.parts)for(v in part.vertices.indices step 9){
                for(a in 0..2){bounds[a]=min(bounds[a],part.vertices[v+a]);bounds[a+3]=max(bounds[a+3],part.vertices[v+a])}
                val n=sqrt((3..5).sumOf{part.vertices[v+it].toDouble().pow(2)}).toFloat()
                assertEquals(e.getString("id"),1f,n,.005f)
                assertTrue((6..8).all{part.vertices[v+it] in 0f..1f})
            }
            for(a in 0..2){assertEquals(e.getJSONArray("min").getDouble(a).toFloat(),bounds[a],.00001f);assertEquals(e.getJSONArray("max").getDouble(a).toFloat(),bounds[a+3],.00001f)}
            val physical=ExhibitScales.entries.getValue(spot.id)
            assertEquals(e.getDouble("scale").toFloat(),physical.scale,.000001f)
            assertTrue((bounds[3]-bounds[0])*physical.scale<spot.halfWidth*2)
            assertTrue((bounds[5]-bounds[2])*physical.scale<spot.halfDepth*2)
            assertTrue(displayHeight+2f<TheaterWorld.CEILING)
            assertTrue(spot.base.minX> -TheaterWorld.halfWidth+3f&&spot.base.maxX<TheaterWorld.halfWidth-3f)
            assertTrue(e.getJSONObject("calibration").getJSONArray("sources").length()>0)
        }
    }
    @Test fun metreCalibrationKeepsSmallSpecimensSmallAndFitsGiants(){
        val d=ExhibitScales.entries
        assertEquals(100,d.size)
        assertEquals(37.5f,d.getValue("patagotitan").width,.002f)
        assertEquals(12.3f,d.getValue("tyrannosaurus").width,.002f)
        assertEquals(22f,d.getValue("brachiosaurus").width,.002f)
        assertEquals(.5f,d.getValue("archaeopteryx").width,.002f)
        assertEquals(10.5f,d.getValue("quetzalcoatlus").depth,.002f)
        assertEquals(5.891464f,d.getValue("triceratops").width,.002f)
        assertEquals(1.550022f,d.getValue("velociraptor").width,.002f)
        assertEquals(19,d.values.count{it.basis=="specimen_units"||it.basis=="measured_bones"})
        assertEquals(5,d.values.count{it.basis=="unresolved"})
        assertEquals(.95f,d.getValue("archaeopteryx").baseY,0f)
        assertEquals(.18f,d.getValue("patagotitan").baseY,0f)
        val p=WalkPose(yaw=0f,pitch=0f)
        assertEquals(1f,p.z-TheaterWorld.move(p,0f,1f,.1f).z,.0001f)
        assertEquals(.5f,p.z-TheaterWorld.move(p,0f,.5f,.1f).z,.0001f)
        assertTrue(TheaterWorld.halfWidth>38f)
        assertEquals(260,TheaterWorld.gardens.size)
        for(g in TheaterWorld.gardens)assertFalse(TheaterWorld.canStand(g.x,g.z))
    }

}
