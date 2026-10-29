package io.github.hatake716.dinobones.theater

import io.github.hatake716.dinobones.model.SkeletonFactory
import kotlin.math.*

data class WalkPose(val x:Float=0f,val z:Float=0f,val yaw:Float=-31f,val pitch:Float=0f) {
    fun look(dx:Float,dy:Float)=copy(yaw=(yaw+dx)%360f,pitch=(pitch+dy).coerceIn(-72f,72f))
    fun finite()=listOf(x,z,yaw,pitch).all{it.isFinite()}
}
data class HallBox(val minX:Float,val maxX:Float,val minZ:Float,val maxZ:Float){
    fun blocks(x:Float,z:Float,r:Float=.34f):Boolean {
        val dx=x-x.coerceIn(minX,maxX);val dz=z-z.coerceIn(minZ,maxZ)
        return dx*dx+dz*dz<r*r
    }
}
data class ExhibitSpot(val id:String,val index:Int,val section:Int,val x:Float,val z:Float){
    val dimensions=ExhibitScales.entries.getValue(id)
    val halfWidth=max(.65f,dimensions.width/2+.65f)
    val halfDepth=max(.65f,dimensions.depth/2+.65f)
    val base=HallBox(x-halfWidth,x+halfWidth,z-halfDepth,z+halfDepth)
    fun arrival(height:Float=dimensions.baseY+dimensions.height):WalkPose {
        val dx=min(dimensions.width*.22f,3f);val dz=halfDepth+3f
        val focus=(dimensions.baseY+height)*.5f
        return WalkPose(x+dx,z+dz,atan2(-dx,dz)*180f/PI.toFloat(),atan2(TheaterWorld.EYE_HEIGHT-focus,hypot(dx,dz))*180f/PI.toFloat())
    }
}
/** The two faces flank the existing name plaques, clear of the skeleton silhouette. */
data class IllustrationBoard(val spot:ExhibitSpot,val back:Boolean) {
    val width=1.2f
    val height=.9f
    val x=spot.x-min(if(spot.dimensions.baseY>.5f)1.2f else 1.6f,spot.halfWidth*1.8f)/2-.18f-width/2
    val y=.70f
    val z=if(back)spot.base.minZ-.32f else spot.base.maxZ+.32f
    val base=HallBox(x-width/2-.04f,x+width/2+.04f,z-.18f,z+.18f)
}
data class ExhibitHit(val spot:ExhibitSpot,val artwork:Boolean=false)
data class GardenSpot(val x:Float,val z:Float,val tall:Boolean){val base=HallBox(x-.85f,x+.85f,z-.85f,z+.85f)}
data class BenchSpot(val x:Float,val z:Float){val base=HallBox(x-1.1f,x+1.1f,z-.4f,z+.4f)}
/** One world unit is one metre. Estimated sizes and unresolved reconstructions remain labelled. */
object TheaterWorld {
    const val EYE_HEIGHT=1.7f
    const val MOVE_SPEED=10f
    const val CEILING=32f
    const val SKY_PERIOD=24f
    const val FRONT=12f
    val sectionNames=listOf("大骨格ホール","生命の回廊","発見のホール","進化の回廊","探究のホール")
    private val hero=listOf("carnotaurus","triceratops","tyrannosaurus","patagotitan","spinosaurus","stegosaurus","brachiosaurus","mosasaurus")
    private val ordered=hero+SkeletonFactory.ids.filterNot{it in hero}
    private val separators=mutableListOf<Float>()
    val rowCenters=mutableListOf<Float>()
    val spots=buildList {
        var front=-10f
        for(section in 0..4){
            for(row in 0..4){
                val start=section*20+row*4
                val ds=(0..3).map{ExhibitScales.entries.getValue(ordered[start+it])}
                val hw=ds.map{max(.65f,it.width/2+.65f)}
                val hd=ds.maxOf{max(.65f,it.depth/2+.65f)}
                val z=front-hd;rowCenters.add(z)
                val innerLeft=-(4f+hw[1]);val innerRight=4f+hw[2]
                val xs=listOf(innerLeft-hw[1]-4.5f-hw[0],innerLeft,innerRight,innerRight+hw[2]+4.5f+hw[3])
                for(col in 0..3)add(ExhibitSpot(ordered[start+col],start+col,section,xs[col],z))
                front=z-hd-8f
            }
            if(section<4){separators.add(front);front-=12f}
        }
    }
    val byId=spots.associateBy{it.id}
    val illustrationBoards=spots.flatMap{listOf(IllustrationBoard(it,false),IllustrationBoard(it,true))}
    val halfWidth=ceil(spots.maxOf{max(abs(it.base.minX),abs(it.base.maxX))}+8f)
    val BACK=floor(spots.minOf{it.base.minZ}-12f)
    val walls=separators.flatMap{z->listOf(HallBox(-halfWidth,-4f,z-.6f,z+.6f),HallBox(4f,halfWidth,z-.6f,z+.6f))}
    fun sectionFront(s:Int)=if(s==0)FRONT else separators[s-1]
    fun sectionBack(s:Int)=if(s==4)BACK else separators[s]
    fun section(p:WalkPose)=separators.count{p.z<it}.coerceIn(0,4)
    fun mapDepth(p:WalkPose):Float {val s=section(p);return ((sectionFront(s)-p.z)/(sectionFront(s)-sectionBack(s))).coerceIn(.03f,.97f)}
    val gardens=buildList {
        for(s in spots)add(GardenSpot(s.x,s.base.minZ-1.8f,s.dimensions.width>5f))
        for(s in 0..4)for(sign in listOf(-1f,1f))add(GardenSpot(sign*8f,sectionFront(s)-4f,true))
        for((i,z) in rowCenters.withIndex()){
            for(sign in listOf(-1f,1f)){
                add(GardenSpot(sign*(halfWidth-3f),z,true))
                add(GardenSpot(sign*(halfWidth-3f),z+3.3f,false))
                add(GardenSpot(sign*2.7f,z-2f,i%3==0))
            }
        }
    }
    val benches=rowCenters.filterIndexed{i,_->i%2==0}.flatMap{z->listOf(BenchSpot(-(halfWidth-6f),z+5f),BenchSpot(halfWidth-6f,z+5f))}
    private val furniture=illustrationBoards.map{it.base}+gardens.map{it.base}+benches.map{it.base}+listOf(HallBox(-2.9f,2.9f,8.4f,10.2f))+(0..4).flatMap{s->
        val z=sectionFront(s)-5f;listOf(HallBox(-3.1f,-2.5f,z-.15f,z+.15f),HallBox(2.7f,2.9f,z-.1f,z+.1f))
    }
    fun canStand(x:Float,z:Float)=x in -halfWidth+.7f..halfWidth-.7f&&z in BACK+1f..FRONT-1f&&spots.none{it.base.blocks(x,z)}&&walls.none{it.blocks(x,z)}&&furniture.none{it.blocks(x,z)}
    fun safe(p:WalkPose)=if(p.finite()&&canStand(p.x,p.z))p.copy(pitch=p.pitch.coerceIn(-72f,72f))else WalkPose()
    fun move(p:WalkPose,strafe:Float,forward:Float,seconds:Float):WalkPose {
        if(!p.finite()||!strafe.isFinite()||!forward.isFinite()||!seconds.isFinite())return safe(p)
        val magnitude=max(1f,hypot(strafe,forward));val angle=p.yaw*PI.toFloat()/180
        val amount=seconds.coerceIn(0f,.1f)*MOVE_SPEED
        val dx=(cos(angle)*strafe+sin(angle)*forward)/magnitude*amount
        val dz=(sin(angle)*strafe-cos(angle)*forward)/magnitude*amount
        var x=p.x;var z=p.z;val n=ceil(max(abs(dx),abs(dz))/.12f).toInt().coerceAtLeast(1)
        repeat(n){if(canStand(x+dx/n,z))x+=dx/n;if(canStand(x,z+dz/n))z+=dz/n}
        return p.copy(x=x,z=z)
    }
    fun rayDistance(origin:FloatArray,ray:FloatArray,lo:FloatArray,hi:FloatArray):Float? {
        var near=0f;var far=100f
        for(a in 0..2){
            if(abs(ray[a])<.000001f){if(origin[a]<lo[a]||origin[a]>hi[a])return null}
            else {val t1=(lo[a]-origin[a])/ray[a];val t2=(hi[a]-origin[a])/ray[a];near=max(near,min(t1,t2));far=min(far,max(t1,t2));if(near>far)return null}
        }
        return near.takeIf{far>=0f}
    }
    fun pick(p:WalkPose,nx:Float,ny:Float,aspect:Float,heights:Map<String,Float>):ExhibitSpot? = pickTarget(p,nx,ny,aspect,heights)?.spot
    fun pickTarget(p:WalkPose,nx:Float,ny:Float,aspect:Float,heights:Map<String,Float>):ExhibitHit? {
        val yaw=p.yaw*PI.toFloat()/180;val pitch=p.pitch*PI.toFloat()/180;val t=tan(32.5f*PI.toFloat()/180)
        val forward=floatArrayOf(sin(yaw)*cos(pitch),-sin(pitch),-cos(yaw)*cos(pitch))
        val right=floatArrayOf(cos(yaw),0f,sin(yaw));val up=floatArrayOf(sin(yaw)*sin(pitch),cos(pitch),-cos(yaw)*sin(pitch))
        val ray=FloatArray(3){forward[it]+nx*aspect*t*right[it]+ny*t*up[it]};val origin=floatArrayOf(p.x,EYE_HEIGHT,p.z)
        val lower=walls.mapNotNull{rayDistance(origin,ray,floatArrayOf(it.minX,0f,it.minZ),floatArrayOf(it.maxX,CEILING,it.maxZ))}
        val upper=separators.mapNotNull{rayDistance(origin,ray,floatArrayOf(-halfWidth,12f,it-.6f),floatArrayOf(halfWidth,CEILING,it+.6f))}
        val wall=(lower+upper).minOrNull()?:100f
        val bones=spots.mapNotNull{spot->val b=spot.base;val top=max(1.5f,heights[spot.id]?:spot.dimensions.let{it.baseY+it.height})
            rayDistance(origin,ray,floatArrayOf(b.minX,.1f,b.minZ),floatArrayOf(b.maxX,top,b.maxZ))?.takeIf{it<wall}?.let{ExhibitHit(spot) to it}
        }
        val pictures=illustrationBoards.mapNotNull{b->
            rayDistance(origin,ray,floatArrayOf(b.x-b.width/2,b.y,b.z-.025f),floatArrayOf(b.x+b.width/2,b.y+b.height,b.z+.025f))?.takeIf{it<wall}?.let{ExhibitHit(b.spot,true) to it}
        }
        return (bones+pictures).minByOrNull{it.second}?.first
    }
}
