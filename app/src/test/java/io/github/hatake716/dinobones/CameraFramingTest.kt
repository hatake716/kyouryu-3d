package io.github.hatake716.dinobones

import io.github.hatake716.dinobones.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.*

class CameraFramingTest {
    @Test fun completeSkeletonFitsPortraitAndLandscapeAtResetAndViewPresets(){
        for(id in listOf("triceratops","tyrannosaurus","patagotitan","brachiosaurus","pteranodon","archaeopteryx","mosasaurus")){
            val mesh=SkeletonFactory.create(id){File("src/main/assets",it).inputStream()}
            val frame=CameraFraming(mesh.vertices)
            for(aspect in listOf(.65f,1.9f,3f))for((yaw,pitch) in listOf(18f to 9f,35f to 38f,0f to 0f,90f to 0f,0f to 89f,0f to -89f)){
                val distance=frame.distance(aspect,yaw,pitch)
                val y=yaw*PI/180;val p=pitch*PI/180
                val sy=sin(y);val cy=cos(y);val sp=sin(p);val cp=cos(p)
                var largest=0.0;var nearest=Double.POSITIVE_INFINITY
                for(i in mesh.vertices.indices step 9){
                    val x=mesh.vertices[i]-frame.center.x;val h=mesh.vertices[i+1]-frame.center.y;val z=mesh.vertices[i+2]-frame.center.z
                    val vx=cy*x-sy*z
                    val vy=-sy*sp*x+cp*h-cy*sp*z
                    val depth=distance-(sy*cp*x+sp*h+cy*cp*z)
                    nearest=min(nearest,depth)
                    val extent=max(abs(vx)/(depth*tan(19.0*PI/180)*aspect),abs(vy)/(depth*tan(19.0*PI/180)))
                    largest=max(largest,extent)
                }
                assertTrue("In front of near plane: $id",nearest>.06)
                assertTrue("No clipping $id $aspect $yaw $pitch: $largest",largest<=.881)
                assertTrue("Useful initial size $id $aspect: $largest",largest>.42)
            }
            if(id=="triceratops")assertTrue("Landscape fit brings the camera closer",frame.distance(1.9f,18f,9f)<mesh.radius/sin(19f*PI.toFloat()/180)*.9f)
        }
    }
}
