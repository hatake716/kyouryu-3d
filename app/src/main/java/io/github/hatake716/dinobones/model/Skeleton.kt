package io.github.hatake716.dinobones.model

import kotlin.math.*

// Procedural reconstruction, with measured axial lengths where ModelEvidence supplies them.
data class V(val x: Float, val y: Float, val z: Float) {
    operator fun plus(b: V) = V(x+b.x,y+b.y,z+b.z)
    operator fun minus(b: V) = V(x-b.x,y-b.y,z-b.z)
    operator fun times(f: Float) = V(x*f,y*f,z*f)
    fun cross(b: V) = V(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
    fun length() = sqrt(x*x+y*y+z*z)
    fun unit() = this * (1f/length().coerceAtLeast(0.00001f))
}
data class BoneLine(val a: V, val b: V, val radius: Float, val group: Int, val measurement: String? = null)
data class BoneFace(val a: V, val b: V, val c: V, val group: Int)
data class Skeleton(val vertices: FloatArray, val lines: List<BoneLine>, val faces: List<BoneFace>, val radius: Float, val metresPerUnit: Float? = null, val texturePrefix: String? = null, val fossilMaterial:FossilMaterial? = null) {
    val vertexCount get() = vertices.size / 9
    val groups: Set<Int> by lazy { buildSet { for(i in 6 until vertices.size step 9)add(vertices[i].toInt()) } }
}
object BoneGroups {
    val names = listOf("全身", "頭骨", "背骨・尾", "肋骨", "肩・骨盤", "前肢・翼", "後肢", "装飾・装甲")
    val descriptions = listOf(
        "骨の配置と体形を観察します。使用した標本・計測の範囲は「モデルと実寸資料」で確認できます。画面では全体を見渡せる大きさに調整します。",
        "頭骨と下顎。歯や角、くちばしの形は食性や分類を考える手がかりになります。穴の位置や歯数は模式的な表現です。",
        "首・胴・尾の椎骨。体を支え、筋肉が付く突起を持ちます。尾は姿勢の安定や推進にも関わります。椎骨数は実測値ではありません。",
        "胸郭を形づくる肋骨。左右から胴を包む立体的な配置を確認できます。腹肋骨や軟骨は省略しています。",
        "四肢を胴につなぐ肩帯と骨盤。形状・関節のつながりは観察しやすいよう簡略化しています。",
        "前肢。翼竜では長い第4指が翼を支え、鳥類の翼とは骨の構成が異なります。翼膜や羽毛は骨ではないため表示しません。",
        "後肢。大腿骨・下腿・足先を示します。関節の可動域を再現する運動シミュレーターではありません。",
        "皮骨や角芯、骨質のとさかなど。ステゴサウルスの板やアンキロサウルスの装甲は皮膚内に生じた骨です。角質の覆いは省略しています。"
    )
}
internal class Builder(capacity:Int=400000,private val countOnly:Boolean=false,val theater:Boolean=false) {
    private var data=FloatArray(if(countOnly)0 else capacity)
    private var used=0
    val floatCount get()=used
    val lines=mutableListOf<BoneLine>()
    val faces=mutableListOf<BoneFace>()
    fun vertex(p:V,n:V,g:Int) {
        if(countOnly){used+=9;return}
        if(used+9>data.size)data=data.copyOf(data.size*2)
        for(v in floatArrayOf(p.x,p.y,p.z,n.x,n.y,n.z,g.toFloat(),0f,0f))data[used++]=v
    }
    fun tri(a:V,b:V,c:V,g:Int){val n=(b-a).cross(c-a).unit();vertex(a,n,g);vertex(b,n,g);vertex(c,n,g)}
    fun smooth(a:V,b:V,c:V,na:V,nb:V,nc:V,g:Int){vertex(a,na,g);vertex(b,nb,g);vertex(c,nc,g)}
    fun ball(c:V,sx:Float,sy:Float,sz:Float,g:Int) {
        val latitude=if(theater)4 else 8;val longitude=if(theater)8 else 14
        fun point(t:Int,s:Int):V {val a=PI.toFloat()*t/latitude;val b=2*PI.toFloat()*s/longitude;return c+V(sin(a)*cos(b)*sx,cos(a)*sy,sin(a)*sin(b)*sz)}
        fun normal(p:V):V {val q=p-c;return V(q.x/(sx*sx),q.y/(sy*sy),q.z/(sz*sz)).unit()}
        for(t in 0 until latitude)for(s in 0 until longitude){val a=point(t,s);val b=point(t+1,s);val c1=point(t+1,s+1);val d=point(t,s+1)
            if(t>0)smooth(a,b,d,normal(a),normal(b),normal(d),g)
            if(t<latitude-1)smooth(d,b,c1,normal(d),normal(b),normal(c1),g)
        }
    }
    // Smooth elliptic rings with shared normals. No internal caps at curved-segment seams.
    private fun sweep(input:List<V>,inputRadii:List<Float>,g:Int,ellipticity:Float=1f,sculpt:Boolean=false,frameAxis:V?=null){
        val samples=if(theater&&input.size>4)input.indices.filter{it%3==0||it==input.lastIndex} else input.indices.toList()
        val points=samples.map{input[it]};val radii=samples.map{inputRadii[it]}
        val sides=if(theater)6 else if(sculpt)18 else 12
        val rings=points.indices.map { i->
            val tangent=frameAxis?:(points[min(i+1,points.lastIndex)]-points[max(i-1,0)]).unit()
            val u=tangent.cross(if(abs(tangent.y)<.95f)V(0f,1f,0f) else V(1f,0f,0f)).unit();val v=tangent.cross(u)
            List(sides){j->val angle=2*PI.toFloat()*j/sides
                val lobe=if(sculpt)1f+.12f*cos(angle*2f)+.065f*sin(angle*3f+i*.19f)+.018f*sin(angle*7f+i*1.8f) else 1f
                points[i]+(u*cos(angle)+v*(sin(angle)*ellipticity))*(radii[i]*lobe)
            }
        }
        val normals=rings.indices.map{i->List(sides){j->
            val around=rings[i][(j+1)%sides]-rings[i][(j+sides-1)%sides]
            val along=rings[min(i+1,rings.lastIndex)][j]-rings[max(i-1,0)][j]
            around.cross(along).unit()
        }}
        for(i in 0 until rings.lastIndex)for(j in 0 until sides){val k=(j+1)%sides
            smooth(rings[i][j],rings[i][k],rings[i+1][j],normals[i][j],normals[i][k],normals[i+1][j],g)
            smooth(rings[i][k],rings[i+1][k],rings[i+1][j],normals[i][k],normals[i+1][k],normals[i+1][j],g)
        }
        for(j in 0 until sides){val k=(j+1)%sides;tri(points.first(),rings.first()[k],rings.first()[j],g);tri(points.last(),rings.last()[j],rings.last()[k],g)}
    }
    fun tube(a:V,b:V,r1:Float,r2:Float,g:Int,record:Boolean=true){
        if((b-a).length()<.0001f)return
        sweep(listOf(a,b),listOf(r1,r2.coerceAtLeast(.0002f)),g)
        if(record)lines.add(BoneLine(a,b,max(r1,r2),g))
    }
    fun bone(a:V,b:V,r:Float,g:Int,measurement:String?=null){
        val d=b-a;val axis=d.unit();val side=axis.cross(if(abs(axis.y)<.9f)V(0f,1f,0f) else V(1f,0f,0f)).unit()
        val shape=listOf(.04f,.83f,1.17f,1.30f,1.16f,.94f,.77f,.66f,.63f,.65f,.72f,.91f,1.18f,1.34f,1.25f,.86f,.04f)
        val points=shape.indices.map { i->val t=i.toFloat()/shape.lastIndex;a+d*t+side*(sin(t*PI.toFloat())*r*.34f) }
        sweep(points,shape.map{it*r},g,.83f,true,axis)
        if(g in 5..6&&r>.07f){
            // Two condyles with a groove between them, within the shaft's axial endpoints.
            val end=b-axis*(r*.64f)
            for(s in listOf(-1f,1f)){
                val c=end+side*(s*r*.61f)
                sweep(listOf(c-axis*(r*.5f),c-axis*(r*.22f),c+axis*(r*.2f),c+axis*(r*.5f)),listOf(r*.30f,r*.66f,r*.66f,r*.30f),g,.85f,true,axis)
            }
        }
        lines.add(BoneLine(a,b,r,g,measurement))
    }
    fun claw(points:List<V>,radius:Float,g:Int){
        val radii=points.indices.map{radius*(1f-it.toFloat()/points.lastIndex).pow(.7f)+.0002f}
        sweep(points,radii,g,.65f,true)
        points.zipWithNext().forEach{(a,b)->lines.add(BoneLine(a,b,radius,g))}
    }
    fun curve(points:List<V>,r:Float,g:Int){
        if(points.size<2)return
        val samples=mutableListOf<V>()
        // Catmull-Rom interpolation preserves authored control points and opens curved spaces.
        for(i in 0 until points.lastIndex){val p0=points[max(0,i-1)];val p1=points[i];val p2=points[i+1];val p3=points[min(points.lastIndex,i+2)]
            for(j in 0..2){val t=j/3f;val t2=t*t;val t3=t2*t
                samples.add((p1*2f+(p2-p0)*t+(p0*2f-p1*5f+p2*4f-p3)*t2+(p1*3f-p0-p2*3f+p3)*t3)*.5f)
            }
        }
        samples.add(points.last());sweep(samples,samples.indices.map{r*(1f-.17f*it/samples.lastIndex)},g,.83f)
        points.zipWithNext().forEach{(a,b)->lines.add(BoneLine(a,b,r,g))}
    }
    fun plate(center:V,outline:List<V>,thickness:Float,g:Int){
        val plane=(outline[0]-center).cross(outline[1]-center).unit()
        val normal=if(plane.length()>.5f)plane else V(0f,0f,1f)
        val rings=if(theater)2 else 6;val subdivisions=if(theater)2 else 5;val segments=outline.size*subdivisions
        fun edge(j:Int):V {
            val sector=(j%segments)/subdivisions;val t=(j%subdivisions).toFloat()/subdivisions
            return outline[sector]*(1-t)+outline[(sector+1)%outline.size]*t
        }
        fun point(i:Int,j:Int,side:Float):V{
            val t=i.toFloat()/rings;val e=edge(j)
            val dome=sin(t*PI.toFloat())*.8f+.25f
            val rough=.08f*sin(j*2.1f+i*1.7f)*sin(t*PI.toFloat())
            return center+(e-center)*t+normal*(thickness*side*(dome+rough))
        }
        fun n(i:Int,j:Int,side:Float):V {
            if(i==0)return normal*side
            return (point(i,j+1,side)-point(i,(j+segments-1)%segments,side)).cross(point(min(rings,i+1),j,side)-point(max(0,i-1),j,side)).unit()*-side
        }
        for(side in listOf(-1f,1f))for(i in 0 until rings)for(j in 0 until segments){val k=(j+1)%segments
            val a=point(i,j,side);val b=point(i+1,j,side);val c=point(i+1,k,side);val d=point(i,k,side)
            smooth(a,b,c,n(i,j,side),n(i+1,j,side),n(i+1,k,side),g)
            if(i>0)smooth(a,c,d,n(i,j,side),n(i+1,k,side),n(i,k,side),g)
        }
        for(j in 0 until segments){val k=(j+1)%segments;tri(point(rings,j,1f),point(rings,k,1f),point(rings,j,-1f),g);tri(point(rings,j,-1f),point(rings,k,1f),point(rings,k,-1f),g)}
        outline.indices.forEach{i->faces.add(BoneFace(center,outline[i],outline[(i+1)%outline.size],g))}
    }
    fun build(metresPerAuthorUnit:Float?=null):Skeleton {
        if(countOnly)return Skeleton(FloatArray(0),emptyList(),emptyList(),0f)
        var min=V(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);var max=V(-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE)
        for(i in 0 until used step 9){min=V(min(min.x,data[i]),min(min.y,data[i+1]),min(min.z,data[i+2]));max=V(max(max.x,data[i]),max(max.y,data[i+1]),max(max.z,data[i+2]))}
        val center=(min+max)*.5f;val extent=max-min;val scale=8f/max(max(extent.x,extent.y),extent.z)
        var radius=0f
        for(i in 0 until used step 9){val p=(V(data[i],data[i+1],data[i+2])-center)*scale;data[i]=p.x;data[i+1]=p.y;data[i+2]=p.z;radius=max(radius,p.length())}
        return Skeleton(if(used==data.size)data else data.copyOf(used),lines.map{it.copy(a=(it.a-center)*scale,b=(it.b-center)*scale,radius=it.radius*scale)},faces.map{it.copy(a=(it.a-center)*scale,b=(it.b-center)*scale,c=(it.c-center)*scale)},radius,metresPerAuthorUnit?.div(scale))
    }
}
object SkeletonFactory {
    val ids=listOf("spinosaurus","tyrannosaurus","triceratops","mosasaurus","velociraptor","stegosaurus","ankylosaurus","brachiosaurus","parasaurolophus","giganotosaurus","allosaurus","pteranodon","archaeopteryx")+ModelProfiles.extras.keys
    private val cache=LinkedHashMap<String,Skeleton>(4,.75f,true)
    private val live=mutableMapOf<String,java.lang.ref.WeakReference<Skeleton>>()
    private const val CACHE_BYTES=32*1024*1024
    @Synchronized fun get(id:String,openAsset:((String)->java.io.InputStream)?=null):Skeleton {
        val key=id+if(openAsset!=null)":assets" else ""
        cache[key]?.let{return it}
        // Reuse a large mesh still held by its thumbnail/view even if it exceeds the cache budget.
        live[key]?.get()?.let{return it}
        fun trim(limit:Int){while(cache.values.sumOf{it.vertices.size.toLong()*4}>limit&&cache.isNotEmpty())cache.remove(cache.keys.first())}
        // Leave room for the builder's temporary array / the incoming scan on 192 MB heaps.
        trim(CACHE_BYTES/2)
        val mesh=create(id,openAsset)
        live[key]=java.lang.ref.WeakReference(mesh)
        if(mesh.vertices.size.toLong()*4<=CACHE_BYTES){cache[key]=mesh;trim(CACHE_BYTES)}
        return mesh
    }
    fun create(id:String,openAsset:((String)->java.io.InputStream)?=null):Skeleton {
        if(id in SpecimenLoader.ids&&openAsset!=null)return SpecimenLoader.load(id,openAsset)
        require(id in ids)
        // Count first, then allocate exactly once. Avoid doubling/copying a 48 MB mesh.
        val counter=Builder(countOnly=true);generate(id,counter)
        return generate(id,Builder(counter.floatCount)).copy(fossilMaterial=FossilMaterials.forModel(id))
    }
    /** Offline gallery export; individual observation retains its original detailed mesh. */
    fun createTheater(id:String):Skeleton {
        require(id in ids)
        val counter=Builder(countOnly=true,theater=true);generate(id,counter)
        return generate(id,Builder(counter.floatCount,theater=true)).copy(fossilMaterial=FossilMaterials.forModel(id))
    }
    private fun generate(id:String,b:Builder):Skeleton {
        val measured=ModelEvidence.limbs[id]
        val profile=ModelProfiles.extras[id]?:ModelProfile(base=id)
        val kind=profile.base
        val quad=profile.quadruped?:(kind in listOf("triceratops","stegosaurus","ankylosaurus","brachiosaurus"))
        val water=kind=="mosasaurus";val ptero=kind=="pteranodon";val bird=kind=="archaeopteryx";val wings=ptero||bird
        val brachio=kind=="brachiosaurus";val spino=kind=="spinosaurus";val anky=kind=="ankylosaurus";val stego=kind=="stegosaurus";val trike=kind=="triceratops";val para=kind=="parasaurolophus";val raptor=kind=="velociraptor"
        val hip=if(water||wings)1.7f else if(quad)1.8f else if(spino)2.0f else 2.7f
        val shoulder=(if(brachio)2.8f else if(stego)1.35f else hip+.12f)+profile.shoulder
        val width=(if(anky)1.05f else if(trike||brachio).85f else if(wings).3f else .58f)*profile.body
        val back=V(-1.25f,hip,0f);val front=V(.9f,shoulder,0f)
        val authorMetres=measured?.let{it.femur/sqrt(.55f*.55f+((hip-.15f)*.43f).pow(2)+.11f*.11f)}
        val defaultNeckEnd=when {brachio->V(2.3f,6.1f,0f);stego->V(1.8f,1.05f,0f);anky->V(1.55f,1.65f,0f);trike->V(1.45f,1.9f,0f);water->V(1.4f,1.8f,0f);else->V(1.8f,shoulder+.75f,0f)}
        val neckEnd=front+V((defaultNeckEnd.x-front.x)*profile.neck,(defaultNeckEnd.y-front.y)*profile.neckRise,0f)
        // Separate vertebral centra, dorsal processes, and transverse processes.
        fun vertebra(p: V,r: Float,spine: Float,forward:V=V(1f,0f,0f),axial:Float=r*1.66f) {
            val along=forward.unit();val side=V(0f,0f,1f);val dorsal=side.cross(along).unit()
            fun at(x:Float,y:Float,z:Float)=p+along*x+dorsal*y+side*z
            b.bone(at(-axial*.5f,0f,0f),at(axial*.5f,0f,0f),r*.78f,2)
            b.curve(listOf(at(0f,r*.32f,-r*.72f),at(0f,r*1.30f,-r*.78f),at(0f,r*1.65f,0f),at(0f,r*1.30f,r*.78f),at(0f,r*.32f,r*.72f)),r*.22f,2)
            val tip=max(spine,r*1.85f)
            b.plate(at(-r*.1f,(r*1.55f+tip)*.5f,0f),listOf(at(-r*.40f,r*1.55f,0f),at(-r*.45f,tip*.92f,0f),at(-r*.13f,tip,0f),at(r*.19f,tip*.96f,0f),at(r*.3f,r*1.55f,0f)),r*.18f,2)
            for(s in listOf(-1f,1f)){
                b.bone(at(0f,r*.9f,s*r*.5f),at(-r*.25f,r*.85f,s*r*2.1f),r*.25f,2)
                b.bone(at(-r*.35f,r*1.3f,s*r*.6f),at(-r*1.05f,r*1.4f,s*r*.75f),r*.18f,2)
            }
        }
        val tailLen=(if(ptero).6f else if(brachio)3.0f else if(water)4.8f else if(raptor||bird)4.0f else 3.7f)*profile.tail
        val tailN=if(ptero&&profile.tail==1f)5 else 27
        for(i in 0..tailN) {val t=i.toFloat()/tailN;val p=back+V(-tailLen*t,-.6f*t-.16f*sin(t*PI.toFloat()),.16f*sin(t*PI.toFloat()));val r=.15f*(1-t)+.024f
            vertebra(p,r,if(water||spino).12f+.55f*sin(t*PI.toFloat()) else r*2)
            if(!ptero&&i in 2 until tailN-3){
                val bottom=p+V(.03f,-(if(water||spino).25f else .16f)*sin(t*PI.toFloat())-.12f,0f)
                b.curve(listOf(p+V(0f,-r*.2f,-r*.55f),bottom,p+V(0f,-r*.2f,r*.55f)),r*.22f,2)
                b.tube(bottom,bottom+V(-r*.30f,-r*.55f,0f),r*.20f,.003f,2)
            }
        }
        for(i in 0..14) {
            val t=i/14f;val p=back+(front-back)*t+V(0f,.16f*sin(t*PI.toFloat()),0f)
            vertebra(p,.14f,if(profile.sail>=0f).33f+profile.sail*(if(profile.crest=="hump") max(0f,1f-abs(t-.2f)*6f) else sin(t*PI.toFloat())) else if(spino).38f+1.55f*sin((.1f+t*.8f)*PI.toFloat()) else .33f)
            if(i in 1..13) for(s in listOf(-1f,1f)) {
                val rw=width*(.35f+.7f*sin(t*PI.toFloat()));val depth=if(water).63f else if(wings).48f else 1.15f
                val points=(0..10).map {j->val u=j/10f; p+V(-.25f*sin(u*PI.toFloat()),-depth*(1-cos(u*PI.toFloat()*.84f))*.6f,s*rw*sin(u*PI.toFloat()*.84f))}
                b.curve(points,if(wings).023f else .036f,3)
            }
        }
        val neckN=if(profile.neck>3f)32 else if(brachio)15 else 9
        for(i in 1..neckN){val t=i.toFloat()/neckN;val delta=neckEnd-front;val p=front+delta*t+V(-.20f*sin(t*PI.toFloat()),.13f*sin(t*PI.toFloat()),0f)
            val tangent=delta+V(-.20f*PI.toFloat()*cos(t*PI.toFloat()),.13f*PI.toFloat()*cos(t*PI.toFloat()),0f)
            vertebra(p,if(brachio).16f else .115f,if(profile.crest=="neckspines") .7f else .21f,tangent,delta.length()/neckN*.87f)
        }
        // Scapulae, coracoids and an open pelvic girdle.
        for(s in listOf(-1f,1f)) {
            val scap=front+V(-.3f,-.13f,s*width*.9f)
            b.plate(scap,listOf(scap+V(-.4f,.30f,0f),scap+V(-.27f,.43f,0f),scap+V(.14f,-.36f,0f),scap+V(.03f,-.49f,0f)),.027f,4)
            val ili=back+V(.02f,.12f,s*width*.63f)
            b.plate(ili,listOf(ili+V(-.48f,.08f,0f),ili+V(-.31f,.27f,0f),ili+V(.48f,.20f,0f),ili+V(.6f,.05f,0f),ili+V(.2f,-.1f,0f)),.04f,4)
            b.bone(front+V(-.6f,.12f,s*width*.85f),front+V(.05f,-.65f,s*width*.9f),.09f,4)
            b.bone(back+V(-.4f,.1f,s*width*.6f),back+V(.5f,.14f,s*width*.62f),.13f,4)
            b.bone(back+V(.12f,0f,s*width*.62f),back+V(if(quad)-.4f else .35f,-.85f,s*width*.42f),.075f,4)
            b.bone(back+V(-.2f,0f,s*width*.62f),back+V(-.6f,-.66f,s*width*.3f),.07f,4)
        }
        b.bone(back+V(0f,0f,-width*.7f),back+V(0f,0f,width*.7f),.085f,4)
        fun leg(start: V,fore: Boolean,s: Float) {
            val g=if(fore)5 else 6
            if(water&&profile.crest!="coastal") {
                val elbow=start+V(-.3f,-.25f,s*.65f*profile.flipper);val wrist=elbow+V(-.4f,-.08f,s*.35f)
                b.bone(start,elbow,.105f,g);b.bone(elbow,wrist,.07f,g)
                for(d in 0..4){var a=wrist+V(d*.12f,0f,0f);for(j in 1..5){val end=a+V(-.12f,-.015f,s*(.19f-.016f*j)*profile.flipper);b.bone(a,end,.033f,g);a=end}};return
            }
            if(wings&&fore) {
                val elbow=start+V(-.22f,.08f,s*.7f);val wrist=elbow+V(.28f,.03f,s*.8f)
                b.bone(start,elbow,.075f,g);b.bone(elbow,wrist,.05f,g);b.bone(elbow+V(.08f,0f,0f),wrist+V(.09f,0f,0f),.031f,g)
                if(ptero){var a=wrist;for(j in 1..4){val tip=a+V(-.35f,.02f,s*(.8f-j*.1f));b.bone(a,tip,.05f-j*.008f,g);a=tip}
                    for(d in 0..2)b.bone(wrist,wrist+V(.27f+d*.05f,-.07f,s*d*.09f),.023f,g)
                }else {for(d in 0..2){val tip=wrist+V(.15f-d*.15f,0f,s*(.48f+d*.1f));b.bone(wrist,tip,.03f,g);b.tube(tip,tip+V(.08f,-.1f,s*.1f),.036f,0f,g)}}
                return
            }
            if(measured!=null&&authorMetres!=null&&(!fore||measured.humerus>0f)){
                val unit=authorMetres
                val upper=if(fore)measured.humerus else if(s<0f&&measured.leftFemur>0f)measured.leftFemur else measured.femur
                val lower=if(fore)measured.radius else if(s<0f&&measured.leftTibia>0f)measured.leftTibia else measured.tibia
                val k=start+V(if(fore).17f else .43f,-1f,s*.07f).unit()*(upper/unit)
                val a=k+V(if(fore)-.07f else -.32f,-1f,s*.025f).unit()*(lower/unit)
                val r=if(fore).09f else .115f
                b.bone(start,k,r,g,if(fore)"humerus" else if(s<0)"femur-left" else "femur-right")
                b.bone(k,a,r*.76f,g,if(fore)"radius" else if(s<0)"tibia-left" else "tibia-right")
                if(fore){
                    val dir=(a-k).unit();val offset=V(-.08f,0f,s*.08f)
                    b.bone(a+offset-dir*(measured.ulna/unit),a+offset,r*.58f,g,"ulna")
                }else b.bone(k+V(-.08f,0f,s*.12f),a+V(-.06f,.08f,s*.1f),r*.34f,g)
                val toes=if(fore)5 else 3
                for(d in 0 until toes){
                    val spread=(d-(toes-1)/2f)
                    val mt=if(fore).13f else measured.metatarsal/unit*(if(d==1)1f else .90f)
                    val end=a+V(.12f,-1f,spread*.20f).unit()*mt
                    b.bone(a,end,r*.38f,g,if(!fore&&d==1)"metatarsal-III" else null)
                    val toe=end+V(.23f,-.035f,spread*.07f);b.bone(end,toe,r*.3f,g)
                    if(raptor&&!fore&&d==0)b.curve(listOf(toe,toe+V(.07f,.15f,0f),toe+V(.23f,.13f,0f),toe+V(.27f,-.02f,0f)),.03f,g)
                    else b.tube(toe,toe+V(.14f,-.055f,spread*.02f),r*.32f,0f,g)
                }
                return
            }
            val smallArm=fore&&!quad&&(!para||profile.quadruped==false)
            val armFactor=(if(kind=="tyrannosaurus").48f else if(raptor||bird)1.2f else .86f)*profile.arm
            val knee=if(smallArm)start+V(.12f,-armFactor*.42f,s*.06f) else if(fore)start+V(.12f,-start.y*.48f,s*.08f) else start+V(.55f,-start.y*.43f,s*.11f)
            val ankle=if(smallArm)knee+V(.38f*armFactor,-.18f,s*.11f) else knee+V(if(fore)-.05f else -.5f,-knee.y+.24f,s*.03f)
            val r=if(smallArm).06f else if(wings).047f else if(brachio).15f else .12f
            b.bone(start,knee,r,g);b.bone(knee,ankle,r*.8f,g);b.bone(knee+V(-.08f,0f,s*.12f),ankle+V(-.06f,0f,s*.09f),r*.4f,g)
            val toes=if(smallArm&&profile.fingers>0)profile.fingers else if(smallArm&&kind=="tyrannosaurus")2 else if(quad)4 else 3
            for(d in 0 until toes){val a=ankle+V(.08f,-.08f,s*(d-(toes-1)/2f)*.11f); val end=a+V(if(smallArm).2f else .38f,-.05f,s*(d-(toes-1)/2f)*.07f)
                b.bone(ankle,a,r*.36f,g);b.bone(a,end,r*.33f,g)
                if(raptor&&!fore&&d==0)b.curve(listOf(end,end+V(.08f,.22f,0f),end+V(.23f,.25f,0f),end+V(.28f,.07f,0f)),.035f,g)
                else b.tube(end,end+V(.13f*(if(fore)profile.claw else 1f),-.08f*(if(fore)profile.claw else 1f),0f),r*.38f,0f,g)
            }
        }
        for(s in listOf(-1f,1f)){leg(back+V(0f,-.15f,s*width*.68f),false,s);leg(front+V(0f,-.5f,s*width*.9f),true,s)}
        // Skull: open fenestrae are formed from bone struts, not painted dark areas.
        val headLen=when{spino->1.5f;trike->1.0f;brachio||stego->.52f;anky->.68f;ptero->1.6f;bird->.58f;raptor->.86f;water->1.28f;else->1.25f}*profile.head
        val headH=when{spino||water||ptero->.32f;stego||bird->.23f;brachio->.32f;raptor->.4f;else->.62f}*profile.headHeight
        val headW=when{trike->.53f;anky->.48f;ptero||bird||stego->.16f;spino->.22f;else->.36f}*profile.headWidth
        val h=neckEnd+V(.05f,.03f,0f)
        SkullSculptor.build(b,h,headLen,headH,headW,kind,profile.teeth?:!ptero,quad||para)
        if(kind=="allosaurus"&&profile.crest=="default")for(s in listOf(-1f,1f)){
            val eyeTop=h+V(headLen*.22f,headH*.48f,s*headW)
            b.plate(eyeTop,listOf(eyeTop+V(-.14f,0f,0f),eyeTop+V(-.02f,.22f,0f),eyeTop+V(.23f,0f,0f)),.045f,7)
        }
        if(trike) {
            val c=h+V(-.25f,.33f,0f)
            val outline=(0..14).map{val a=it*PI.toFloat()*2/15; c+V(-.33f*(1+sin(a)),.75f*cos(a),.92f*sin(a))*profile.frill}
            // A solid bony frill; not the paired openings of other ceratopsians.
            for(i in outline.indices){if(!profile.fenestrae){b.tri(c,outline[i],outline[(i+1)%outline.size],7);b.faces.add(BoneFace(c,outline[i],outline[(i+1)%outline.size],7))}else if(i%3==0)b.bone(c,outline[i],.055f,7);b.bone(outline[i],outline[(i+1)%outline.size],.04f,7)}
            if(profile.brow>0f)for(s in listOf(-1f,1f))b.tube(h+V(.2f,.35f,s*.36f),h+V(.2f+.65f*profile.brow,.35f+.75f*profile.brow,s*.38f),.13f,.008f,7)
            if(profile.nose>0f)b.tube(h+V(.9f,.03f,0f),h+V(1.08f,.03f+.43f*profile.nose,0f),.1f,.008f,7)
            for(i in 0 until profile.frillSpikes){val a=(i+1)*PI.toFloat()/(profile.frillSpikes+1);val c=h+V(-.45f,.4f+.45f*sin(a),.75f*cos(a));b.tube(c,c+V(-.2f,.5f*sin(a),.55f*cos(a)),.09f,0f,7)}
        }
        if(para&&profile.crest=="default")b.curve(listOf(h+V(.3f,.3f,0f),h+V(-.1f,.8f,0f),h+V(-1.15f,.9f,0f),h+V(-1.28f,.74f,0f),h+V(-.25f,.58f,0f)),.09f,7)
        if(ptero&&profile.crest=="default")b.plate(h,listOf(h+V(.12f,.18f,0f),h+V(-1.22f,.55f,0f),h+V(-.27f,-.15f,0f)),.036f,7)
        if(stego)for(i in 0..18){val t=i/18f;val x=-3.3f+t*4.9f;val y=if(x< -1.25f)hip-( -1.25f-x)*.17f else hip+(shoulder-hip)*(x+1.25f)/2.15f+.1f
            val height=.23f+1.0f*sin(t*PI.toFloat());val c=V(x,y,if(i%2==0)-.16f else .16f)
            if(profile.crest=="backspikes"&&i<11)b.tube(c,c+V(-.25f,.6f,if(i%2==0)-.7f else .7f),.09f,0f,7) else b.plate(c,listOf(c+V(-.24f,0f,0f),c+V(-.32f,height*.65f,0f),c+V(.02f,height,0f),c+V(.29f,height*.55f,0f),c+V(.24f,0f,0f)),.042f,7)
        }
        if(stego)for(s in listOf(-1f,1f))for(i in 0..1){val c=back+V(-3.1f+i*.48f,-.53f,0f);b.tube(c,c+V(-.37f,.4f,s*.8f),.09f,.005f,7)}
        if(anky){for(i in 0..8)for(j in -3..3){val x=-1.6f+i*.34f;val z=j*.28f;val c=V(x,hip+.34f-.23f*abs(j),z);b.ball(c,.18f,.095f,.14f,7);if(abs(j)==3)b.tube(c,c+V(-.13f,.07f,j*.11f),.12f,0f,7)}
            val c=back+V(-tailLen,-.6f,0f);if(profile.club)b.ball(c,.43f,.23f,.45f,7);for(s in listOf(-1f,1f))b.tube(h+V(-.13f,.07f,s*.38f),h+V(-.4f,.28f,s*.62f),.10f,0f,7)
        }
        when(profile.crest){
            "dome"->b.ball(h+V(.12f,.35f,0f),.34f,.32f,.28f,7)
            "boss"->b.ball(h+V(.55f,.3f,0f),.35f,.16f,.3f,7)
            "nasal","spike"->b.tube(h+V(.48f,.25f,0f),h+V(.38f,.77f,0f),.12f,0f,7)
            "horns"->for(s in listOf(-1f,1f))b.tube(h+V(.05f,.25f,s*.3f),h+V(.08f,.62f,s*.52f),.11f,0f,7)
            "double"->for(s in listOf(-1f,1f)){val c=h+V(.2f,.3f,s*.18f);b.plate(c,listOf(c+V(-.2f,0f,0f),c+V(0f,.4f,0f),c+V(.65f,.15f,0f),c+V(.65f,-.05f,0f)),.03f,7)}
            "fan"->{val c=h+V(.05f,.25f,0f);b.plate(c,listOf(c+V(0f,0f,-.3f),c+V(0f,.4f,-.24f),c+V(0f,.48f,.24f),c+V(0f,0f,.3f)),.03f,7)}
            "casque","keel"->{val c=h+V(.35f,.2f,0f);b.plate(c,listOf(c+V(-.2f,0f,0f),c+V(-.1f,.42f,0f),c+V(.4f,.15f,0f),c+V(.5f,-.1f,0f)),.05f,7)}
            "shoulderspikes"->for(s in listOf(-1f,1f))b.tube(front+V(0f,0f,s*width),front+V(-.25f,.55f,s*(width+.55f)),.15f,0f,7)
            "shell"->{for(i in 0..12){val t=i/12f;val x=back.x+(front.x-back.x)*t;val w=width*sin(t*PI.toFloat());b.curve((0..16).map{j->val a=j*PI.toFloat()/8;V(x,hip+.35f*sin(a),w*cos(a))},.065f,7)}}
            "fish"->{val c=back+V(-tailLen,-.6f,0f);b.curve(listOf(c+V(.25f,.7f,0f),c,c+V(-.25f,-.65f,0f)),.055f,2)}
            "thumb"->for(s in listOf(-1f,1f))b.tube(V(1.08f,.27f,s*(width+.05f)),V(1.16f,.58f,s*(width+.25f)),.085f,0f,5)
        }
        return b.build(authorMetres)
    }
}

/** Physical spacing selected for the normalized viewing floor, not a screen-size claim. */
fun gridMetres(metresPerUnit:Float):Float = when {metresPerUnit<.1f->.1f;metresPerUnit<.5f->.5f;else->1f}
