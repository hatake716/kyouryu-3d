package io.github.hatake716.dinobones.model

import kotlin.math.*

/** Perspective fit of the model bounds, including depth, in the actual GL viewport. */
class CameraFraming(vertices:FloatArray) {
    private val min=FloatArray(3){Float.POSITIVE_INFINITY}
    private val max=FloatArray(3){Float.NEGATIVE_INFINITY}
    init {require(vertices.isNotEmpty());for(i in vertices.indices step 9)for(a in 0..2){min[a]=min(min[a],vertices[i+a]);max[a]=max(max[a],vertices[i+a])}}
    val center=V((min[0]+max[0])/2,(min[1]+max[1])/2,(min[2]+max[2])/2)
    // A single box wastes most of the frame around thin wings and long necks.
    // Tight bounds of occupied spatial cells still contain every vertex, while
    // limiting each frame to a small set of corners instead of millions of vertices.
    private val corners:List<V> = buildList {
        val cells=arrayOfNulls<FloatArray>(512)
        fun cellAxis(value:Float,a:Int)=(((value-min[a])/(max[a]-min[a]).coerceAtLeast(.0001f))*8).toInt().coerceIn(0,7)
        for(i in vertices.indices step 9){
            val key=cellAxis(vertices[i],0)+cellAxis(vertices[i+1],1)*8+cellAxis(vertices[i+2],2)*64
            val cell=cells[key]?:FloatArray(6){if(it<3)Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY}.also{cells[key]=it}
            for(a in 0..2){cell[a]=min(cell[a],vertices[i+a]);cell[a+3]=max(cell[a+3],vertices[i+a])}
        }
        for(c in cells.filterNotNull())for(i in 0..7)add(V(c[if(i and 1==0)0 else 3],c[if(i and 2==0)1 else 4],c[if(i and 4==0)2 else 5])-center)
    }
    fun distance(aspect:Float,yawDegrees:Float,pitchDegrees:Float):Float {
        val yaw=yawDegrees*PI.toFloat()/180;val pitch=pitchDegrees*PI.toFloat()/180
        val right=V(cos(yaw),0f,-sin(yaw));val up=V(-sin(yaw)*sin(pitch),cos(pitch),-cos(yaw)*sin(pitch))
        val eye=V(sin(yaw)*cos(pitch),sin(pitch),cos(yaw)*cos(pitch))
        fun dot(a:V,b:V)=a.x*b.x+a.y*b.y+a.z*b.z
        val tanY=tan(19f*PI.toFloat()/180)*.88f;val tanX=tanY*aspect.coerceAtLeast(.1f)
        return corners.maxOf{p->val depth=dot(p,eye);depth+max(.2f,max(abs(dot(p,right))/tanX,abs(dot(p,up))/tanY))}
    }
}
