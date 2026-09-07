package io.github.hatake716.dinobones.model

import kotlin.math.*

// Original educational geometry. Coordinates are authored here, not traced from a specimen.
data class V(val x: Float, val y: Float, val z: Float) {
    operator fun plus(b: V) = V(x+b.x,y+b.y,z+b.z)
    operator fun minus(b: V) = V(x-b.x,y-b.y,z-b.z)
    operator fun times(f: Float) = V(x*f,y*f,z*f)
    fun cross(b: V) = V(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
    fun length() = sqrt(x*x+y*y+z*z)
    fun unit() = this * (1f/length().coerceAtLeast(0.00001f))
}
data class BoneLine(val a: V, val b: V, val radius: Float, val group: Int)
data class BoneFace(val a: V, val b: V, val c: V, val group: Int)
data class Skeleton(val vertices: FloatArray, val lines: List<BoneLine>, val faces: List<BoneFace>, val radius: Float) {
    val vertexCount get() = vertices.size / 7
}
object BoneGroups {
    val names = listOf("全身", "頭骨", "背骨・尾", "肋骨", "肩・骨盤", "前肢・翼", "後肢", "装飾・装甲")
    val descriptions = listOf(
        "骨の配置と体形を観察します。骨数・関節面・細部の形状は簡略化しています。表示サイズは種ごとに調整され、同じ縮尺ではありません。",
        "頭骨と下顎。歯や角、くちばしの形は食性や分類を考える手がかりになります。穴の位置や歯数は模式的な表現です。",
        "首・胴・尾の椎骨。体を支え、筋肉が付く突起を持ちます。尾は姿勢の安定や推進にも関わります。椎骨数は実測値ではありません。",
        "胸郭を形づくる肋骨。左右から胴を包む立体的な配置を確認できます。腹肋骨や軟骨は省略しています。",
        "四肢を胴につなぐ肩帯と骨盤。形状・関節のつながりは観察しやすいよう簡略化しています。",
        "前肢。翼竜では長い第4指が翼を支え、鳥類の翼とは骨の構成が異なります。翼膜や羽毛は骨ではないため表示しません。",
        "後肢。大腿骨・下腿・足先を示します。関節の可動域を再現する運動シミュレーターではありません。",
        "皮骨や角芯、骨質のとさかなど。ステゴサウルスの板やアンキロサウルスの装甲は皮膚内に生じた骨です。角質の覆いは省略しています。"
    )
}
private class Builder {
    val data = ArrayList<Float>(250000)
    val lines = mutableListOf<BoneLine>()
    val faces = mutableListOf<BoneFace>()
    fun vertex(p: V,n: V,g: Int) { data.add(p.x);data.add(p.y);data.add(p.z);data.add(n.x);data.add(n.y);data.add(n.z);data.add(g.toFloat()) }
    fun tri(a: V,b: V,c: V,g: Int) { val n=(b-a).cross(c-a).unit();vertex(a,n,g);vertex(b,n,g);vertex(c,n,g) }
    fun ball(c: V, sx: Float,sy: Float,sz: Float,g: Int) {
        fun p(t: Int,s: Int): V {val a=PI.toFloat()*t/6;val b=2*PI.toFloat()*s/10;return c+V(sin(a)*cos(b)*sx,cos(a)*sy,sin(a)*sin(b)*sz)}
        for(t in 0 until 6) for(s in 0 until 10) {tri(p(t,s),p(t+1,s),p(t+1,s+1),g);tri(p(t,s),p(t+1,s+1),p(t,s+1),g)}
    }
    fun tube(a: V,b: V,r1: Float,r2: Float,g: Int,record: Boolean=true) {
        val axis=(b-a).unit();if((b-a).length()<0.0001f)return
        val u=axis.cross(if(abs(axis.y)<0.95f)V(0f,1f,0f) else V(1f,0f,0f)).unit(); val v=axis.cross(u)
        for(i in 0 until 8) {
            val t=i*PI.toFloat()/4;val nt=(i+1)*PI.toFloat()/4
            val n=u*cos(t)+v*sin(t);val m=u*cos(nt)+v*sin(nt)
            tri(a+n*r1,b+n*r2,b+m*r2,g);tri(a+n*r1,b+m*r2,a+m*r1,g)
            tri(a,a+m*r1,a+n*r1,g);tri(b,b+n*r2,b+m*r2,g)
        }
        if(record) lines.add(BoneLine(a,b,max(r1,r2),g))
    }
    fun bone(a: V,b: V,r: Float,g: Int) {
        val d=b-a; tube(a,a+d*.2f,r*1.3f,r*.72f,g,false);tube(a+d*.2f,a+d*.8f,r*.72f,r*.7f,g,false);tube(a+d*.8f,b,r*.7f,r*1.3f,g,false)
        ball(a,r*1.4f,r*1.15f,r*1.3f,g);ball(b,r*1.3f,r*1.1f,r*1.35f,g); lines.add(BoneLine(a,b,r,g))
    }
    fun curve(points: List<V>,r: Float,g: Int) {points.zipWithNext().forEach { (a,b)->tube(a,b,r,r*.96f,g) }}
    fun plate(center: V,outline: List<V>,thickness: Float,g: Int) {
        val off=V(0f,0f,thickness)
        outline.indices.forEach { i->val a=outline[i];val b=outline[(i+1)%outline.size];tri(center+off,a+off,b+off,g);tri(center-off,b-off,a-off,g);tri(a+off,a-off,b-off,g);tri(a+off,b-off,b+off,g);faces.add(BoneFace(center,a,b,g)) }
    }
    fun build(): Skeleton {
        val xs=data.filterIndexed { i,_->i%7==0 }; val ys=data.filterIndexed{i,_->i%7==1};val zs=data.filterIndexed{i,_->i%7==2}
        val center=V((xs.min()+xs.max())/2,(ys.min()+ys.max())/2,(zs.min()+zs.max())/2)
        val scale=8f/max(max(xs.max()-xs.min(),ys.max()-ys.min()),zs.max()-zs.min())
        var radius=0f
        for(i in data.indices step 7) {val p=(V(data[i],data[i+1],data[i+2])-center)*scale;data[i]=p.x;data[i+1]=p.y;data[i+2]=p.z;radius=max(radius,p.length())}
        return Skeleton(data.toFloatArray(),lines.map{it.copy(a=(it.a-center)*scale,b=(it.b-center)*scale,radius=it.radius*scale)},faces.map{it.copy(a=(it.a-center)*scale,b=(it.b-center)*scale,c=(it.c-center)*scale)},radius)
    }
}
object SkeletonFactory {
    val ids=listOf("spinosaurus","tyrannosaurus","triceratops","mosasaurus","velociraptor","stegosaurus","ankylosaurus","brachiosaurus","parasaurolophus","giganotosaurus","allosaurus","pteranodon","archaeopteryx")+ModelProfiles.extras.keys
    private val cache = object : LinkedHashMap<String,Skeleton>(4,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,Skeleton>?) = size > 3
    }
    @Synchronized fun get(id: String): Skeleton = cache.getOrPut(id) { create(id) }
    fun create(id: String): Skeleton {
        require(id in ids)
        val b=Builder()
        val profile=ModelProfiles.extras[id]?:ModelProfile(base=id)
        val kind=profile.base
        val quad=profile.quadruped?:(kind in listOf("triceratops","stegosaurus","ankylosaurus","brachiosaurus"))
        val water=kind=="mosasaurus";val ptero=kind=="pteranodon";val bird=kind=="archaeopteryx";val wings=ptero||bird
        val brachio=kind=="brachiosaurus";val spino=kind=="spinosaurus";val anky=kind=="ankylosaurus";val stego=kind=="stegosaurus";val trike=kind=="triceratops";val para=kind=="parasaurolophus";val raptor=kind=="velociraptor"
        val hip=if(water||wings)1.7f else if(quad)1.8f else if(spino)2.0f else 2.7f
        val shoulder=(if(brachio)2.8f else if(stego)1.35f else hip+.12f)+profile.shoulder
        val width=(if(anky)1.05f else if(trike||brachio).85f else if(wings).3f else .58f)*profile.body
        val back=V(-1.25f,hip,0f);val front=V(.9f,shoulder,0f)
        val defaultNeckEnd=when {brachio->V(2.3f,6.1f,0f);stego->V(1.8f,1.05f,0f);anky->V(1.55f,1.65f,0f);trike->V(1.45f,1.9f,0f);water->V(1.4f,1.8f,0f);else->V(1.8f,shoulder+.75f,0f)}
        val neckEnd=front+V((defaultNeckEnd.x-front.x)*profile.neck,(defaultNeckEnd.y-front.y)*profile.neckRise,0f)
        // Separate vertebral centra, dorsal processes, and transverse processes.
        fun vertebra(p: V,r: Float,spine: Float) {
            b.ball(p,r*1.1f,r,r,2)
            b.bone(p+V(0f,r*.25f,0f),p+V(-r*.35f,spine,0f),r*.3f,2)
            for(s in listOf(-1f,1f))b.tube(p,p+V(0f,r*.4f,s*r*2.1f),r*.34f,r*.13f,2)
        }
        val tailLen=(if(ptero).6f else if(brachio)3.0f else if(water)4.8f else if(raptor||bird)4.0f else 3.7f)*profile.tail
        val tailN=if(ptero&&profile.tail==1f)5 else 27
        for(i in 0..tailN) {val t=i.toFloat()/tailN;val p=back+V(-tailLen*t,-.6f*t-.16f*sin(t*PI.toFloat()),.16f*sin(t*PI.toFloat()));val r=.15f*(1-t)+.024f
            vertebra(p,r,if(water||spino).12f+.55f*sin(t*PI.toFloat()) else r*2)
            if((water||spino)&&i<tailN-3)b.tube(p,p+V(.03f,-.25f*sin(t*PI.toFloat())-.12f,0f),r*.35f,.016f,2)
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
        for(i in 1..neckN){val t=i.toFloat()/neckN;val p=front+(neckEnd-front)*t+V(-.20f*sin(t*PI.toFloat()),.13f*sin(t*PI.toFloat()),0f);vertebra(p,if(brachio).16f else .115f,if(profile.crest=="neckspines") .7f else .21f)}
        // Scapulae, coracoids and an open pelvic girdle.
        for(s in listOf(-1f,1f)) {
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
        b.ball(h,headLen*.2f,headH*.49f,headW*.76f,1)
        for(s in listOf(-1f,1f)) {
            val rear=h+V(-.15f,-headH*.12f,s*headW*.65f);val eyeTop=h+V(headLen*.22f,headH*.48f,s*headW)
            val cheek=h+V(headLen*.3f,-headH*.48f,s*headW)
            val nose=h+V(headLen*.75f,headH*.20f,s*headW*.57f);val tip=h+V(headLen,-headH*.08f,s*headW*.27f)
            b.curve(listOf(rear,eyeTop,nose,tip),.055f,1);b.curve(listOf(rear,cheek,tip),.045f,1)
            // Broad nasal and mandibular bone surfaces around open skull windows.
            val roofRear=h+V(headLen*.27f,headH*.44f,s*headW*.83f)
            val roofFront=h+V(headLen*.92f,headH*.08f,s*headW*.3f)
            b.plate((roofRear+roofFront)*.5f,listOf(roofRear,roofFront,roofFront+V(0f,-headH*.18f,0f),roofRear+V(0f,-headH*.16f,0f)),.026f,1)
            val orbitCenter=h+V(headLen*.14f,0f,s*headW*.96f)
            b.curve((0..14).map { j->val a=j*2*PI.toFloat()/14;orbitCenter+V(cos(a)*headLen*.14f,sin(a)*headH*.38f,0f) },.046f,1)
            val jawCenter=h+V(headLen*.49f,-headH*.68f,s*headW*.68f)
            b.plate(jawCenter,listOf(h+V(.01f,-headH*.43f,s*headW*.72f),h+V(headLen*.94f,-headH*.52f,s*headW*.28f),h+V(headLen*.86f,-headH*.67f,s*headW*.32f),h+V(headLen*.24f,-headH*.91f,s*headW*.78f)),.027f,1)
            b.bone(eyeTop,cheek,.052f,1);b.bone(nose,h+V(headLen*.64f,-headH*.3f,s*headW*.67f),.041f,1)
            val jaw=rear+V(.03f,-.12f,0f);val jtip=tip+V(-.05f,-headH*.46f,0f)
            b.curve(listOf(jaw,h+V(headLen*.4f,-headH*.78f,s*headW*.76f),jtip),.058f,1)
            if(profile.teeth?:!ptero)for(i in 0 until if(spino||water)16 else 10){val t=.35f+i*.038f;val pos=h+V(headLen*t,-headH*.36f,s*headW*(1-t*.75f));b.tube(pos,pos+V(.028f,-headH*.27f,0f),if(spino).026f else .034f,0f,1)}
            if(kind=="allosaurus"&&profile.crest=="default")b.plate(eyeTop,listOf(eyeTop+V(-.14f,0f,0f),eyeTop+V(-.02f,.22f,0f),eyeTop+V(.23f,0f,0f)),.045f,7)
        }
        b.bone(h+V(headLen,-headH*.08f,-headW*.27f),h+V(headLen,-headH*.08f,headW*.27f),.06f,1)
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
        return b.build()
    }
}
