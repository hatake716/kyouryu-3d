package io.github.hatake716.dinobones.theater

import kotlin.math.*

/** Original architectural and botanical geometry. No museum imagery or purchased assets. */
internal class HallGeometry {
    private var floats=FloatArray(32768);private var count=0
    fun vertex(x:Float,y:Float,z:Float,nx:Float,ny:Float,nz:Float,color:FloatArray){
        if(count+9>floats.size)floats=floats.copyOf(floats.size*2)
        floats[count++]=x;floats[count++]=y;floats[count++]=z;floats[count++]=nx;floats[count++]=ny;floats[count++]=nz
        floats[count++]=color[0];floats[count++]=color[1];floats[count++]=color[2]
    }
    fun quad(a:FloatArray,b:FloatArray,c:FloatArray,d:FloatArray,n:FloatArray,color:FloatArray){for(p in listOf(a,b,c,a,c,d))vertex(p[0],p[1],p[2],n[0],n[1],n[2],color)}
    private fun p(x:Float,y:Float,z:Float)=floatArrayOf(x,y,z)
    fun box(x:Float,y:Float,z:Float,w:Float,h:Float,depth:Float,color:FloatArray){
        val l=x-w/2;val r=x+w/2;val f=z+depth/2;val back=z-depth/2;val t=y+h
        quad(p(l,y,f),p(r,y,f),p(r,t,f),p(l,t,f),p(0f,0f,1f),color)
        quad(p(r,y,back),p(l,y,back),p(l,t,back),p(r,t,back),p(0f,0f,-1f),color)
        quad(p(l,y,back),p(l,y,f),p(l,t,f),p(l,t,back),p(-1f,0f,0f),color)
        quad(p(r,y,f),p(r,y,back),p(r,t,back),p(r,t,f),p(1f,0f,0f),color)
        quad(p(l,t,f),p(r,t,f),p(r,t,back),p(l,t,back),p(0f,1f,0f),color)
        quad(p(l,y,back),p(r,y,back),p(r,y,f),p(l,y,f),p(0f,-1f,0f),color)
    }
    private fun rod(a:FloatArray,b:FloatArray,r1:Float,r2:Float,color:FloatArray,sides:Int=7){
        val d=FloatArray(3){b[it]-a[it]};val len=sqrt(d.sumOf{it.toDouble()*it}.toFloat());if(len<.00001f)return
        for(i in 0..2)d[i]/=len
        val cross=if(abs(d[1])>.9f)p(1f,0f,0f) else p(0f,1f,0f)
        var u=p(d[1]*cross[2]-d[2]*cross[1],d[2]*cross[0]-d[0]*cross[2],d[0]*cross[1]-d[1]*cross[0]);val ul=sqrt(u.sumOf{it.toDouble()*it}.toFloat());u=FloatArray(3){u[it]/ul}
        val v=p(d[1]*u[2]-d[2]*u[1],d[2]*u[0]-d[0]*u[2],d[0]*u[1]-d[1]*u[0])
        fun normal(t:Float)=FloatArray(3){u[it]*cos(t)+v[it]*sin(t)}
        fun point(o:FloatArray,n:FloatArray,r:Float)=FloatArray(3){o[it]+n[it]*r}
        for(i in 0 until sides){val n=normal(i*2f*PI.toFloat()/sides);val m=normal((i+1)*2f*PI.toFloat()/sides)
            quad(point(a,n,r1),point(a,m,r1),point(b,m,r2),point(b,n,r2),n,color)
        }
    }
    private fun leaf(a:FloatArray,b:FloatArray,width:Float,color:FloatArray){
        val dx=b[0]-a[0];val dz=b[2]-a[2];val len=hypot(dx,dz).coerceAtLeast(.01f)
        val mid=FloatArray(3){a[it]+(b[it]-a[it])*.42f};val left=p(mid[0]-dz/len*width,mid[1]+width*.18f,mid[2]+dx/len*width)
        val right=p(mid[0]+dz/len*width,mid[1]-width*.12f,mid[2]-dx/len*width)
        quad(a,left,b,right,p(-dx*.1f,1f,-dz*.1f),color)
    }
    fun island(s:ExhibitSpot){
        val x=s.x;val z=s.z;val height=s.dimensions.baseY
        val stone=p(.36f,.30f,.23f);val top=p(.48f,.405f,.30f)
        for(i in 0 until 24){val a=i*PI.toFloat()/12;val b=(i+1)*PI.toFloat()/12
            fun point(t:Float,h:Float,k:Float)=p(x+cos(t)*s.halfWidth*k,h,z+sin(t)*s.halfDepth*k)
            quad(point(a,0f,1f),point(b,0f,1f),point(b,height,.98f),point(a,height,.98f),p(cos(a),.12f,sin(a)),stone)
            quad(p(x,height,z),point(a,height,.98f),point(b,height,.98f),p(x,height,z),p(0f,1f,0f),top)
        }
        if(s.dimensions.height>1.2f){
            val brass=p(.40f,.31f,.18f)
            for(sign in listOf(-1f,1f)){
                for(dx in listOf(-s.halfWidth*.84f,s.halfWidth*.84f))rod(p(x+dx,0f,z+sign*s.halfDepth),p(x+dx,1.05f,z+sign*s.halfDepth),.025f,.025f,brass)
                rod(p(x-s.halfWidth*.84f,1.01f,z+sign*s.halfDepth),p(x+s.halfWidth*.84f,1.01f,z+sign*s.halfDepth),.018f,.018f,brass)
            }
        }
        // Small bones keep their physical size on a waist-high plinth.
        if(height<.5f)box(x,.02f,z+s.halfDepth-.07f,.08f,.97f,.08f,p(.20f,.17f,.13f))
    }
    private fun plant(g:GardenSpot,index:Int){
        val outer=abs(g.x)>TheaterWorld.halfWidth*.5f;val factor=if(outer)1f else .7f
        val earth=p(.115f,.09f,.06f);val clay=p(.30f,.33f,.25f)
        val center=p(g.x,.5f,g.z)
        rod(p(g.x,0f,g.z),center,.62f,.80f,clay,12)
        for(i in 0 until 12){val a=i*PI.toFloat()/6;val b=(i+1)*PI.toFloat()/6
            quad(center,p(g.x+cos(a)*.77f,.5f,g.z+sin(a)*.77f),p(g.x+cos(b)*.77f,.5f,g.z+sin(b)*.77f),center,p(0f,1f,0f),earth)
        }
        val trunk=if(g.tall)(3.2f+(index%3)*.45f)*factor else .28f
        if(g.tall){rod(center,p(g.x,.5f+trunk,g.z),.14f*factor,.10f*factor,p(.28f,.21f,.13f),9)
            for(h in 1..8)rod(p(g.x,h*trunk/9+.48f,g.z),p(g.x,h*trunk/9+.52f,g.z),.15f*factor,.15f*factor,p(.34f,.27f,.16f),9)
        }
        val base=.5f+trunk;val length=if(g.tall)2.15f*factor else 1.15f
        val fronds=if(g.tall)12 else 9
        for(f in 0 until fronds){val angle=f*2f*PI.toFloat()/fronds+index*.71f;val dx=cos(angle);val dz=sin(angle)
            fun at(t:Float)=p(g.x+dx*length*t,base+sin(t*PI.toFloat())*length*.36f-t*t*length*.20f,g.z+dz*length*t)
            rod(at(0f),at(.55f),.018f,.010f,p(.30f,.36f,.12f),5);rod(at(.55f),at(1f),.01f,.001f,p(.28f,.36f,.13f),5)
            for(j in 1..11){val t=j/12f;val a=at(t);val side=sin(t*PI.toFloat()).coerceAtLeast(.1f)*length*.24f
                for(sign in listOf(-1f,1f)){
                    val end=p(a[0]-dz*sign*side+dx*length*.15f,a[1]-.06f*factor,a[2]+dx*sign*side+dz*length*.15f)
                    val shade=.025f*((j+f+index)%4);leaf(a,end,length*.075f,p(.12f+shade,.285f+shade,.09f+shade*.6f))
                }
            }
        }
        // Low fern rosette fills the planter around the tall trunk.
        if(g.tall)for(f in 0..10){val a=f*PI.toFloat()*2/11;leaf(p(g.x,.55f,g.z),p(g.x+cos(a)*.9f,.68f,g.z+sin(a)*.9f),.18f,p(.18f,.34f,.10f))}
    }
    private fun fossilPanel(x:Float,z:Float){
        val dark=p(.26f,.23f,.18f);val stone=p(.53f,.45f,.32f);val shell=p(.34f,.29f,.21f)
        box(x,3f,z,5f,5f,.25f,dark);box(x,3.15f,z+.14f,4.7f,4.7f,.06f,stone)
        // Stylised ammonite relief, authored from a mathematical spiral.
        val front=z+.22f
        fun point(t:Float):FloatArray {val angle=t*PI.toFloat()*5.2f;val radius=.08f+1.8f*t*t;return p(x+cos(angle)*radius,5.6f+sin(angle)*radius,front)}
        for(i in 0 until 80){val a=i/80f;val b=(i+1)/80f;rod(point(a),point(b),.028f+a*.105f,.028f+b*.105f,shell,6)
            if(i>28&&i%2==0){val center=point(b);val angle=b*PI.toFloat()*5.2f;rod(p(center[0]-cos(angle)*.13f,center[1]-sin(angle)*.13f,front+.10f),p(center[0]+cos(angle)*.13f,center[1]+sin(angle)*.13f,front+.10f),.025f,.016f,p(.63f,.53f,.36f),5)}
        }
        box(x,8.2f,z+.4f,4.5f,.06f,.15f,p(1.4f,1.18f,.78f))
    }
    private fun person(x:Float,z:Float){
        val c=p(.33f,.31f,.27f)
        for(sign in listOf(-1f,1f)){rod(p(x+sign*.12f,.03f,z),p(x+sign*.11f,.85f,z),.075f,.085f,c);rod(p(x+sign*.19f,1.38f,z),p(x+sign*.27f,.89f,z),.06f,.045f,c)}
        rod(p(x,.80f,z),p(x,1.43f,z),.18f,.21f,c,10);rod(p(x,1.45f,z),p(x,1.7f,z),.105f,.09f,c,10)
    }
    fun array()=floats.copyOf(count)
    companion object {
        /** Mural faces follow the solid walls and leave the central doorways open. */
        fun forestWalls():FloatArray {
            val g=HallGeometry();val w=TheaterWorld.halfWidth
            fun face(x1:Float,z1:Float,x2:Float,z2:Float,nx:Float,nz:Float,bottom:Float=.64f){
                val top=TheaterWorld.CEILING-.18f
                val u1=(if(nx==0f)x1 else z1)/62f;val u2=(if(nx==0f)x2 else z2)/62f
                val a=floatArrayOf(x1,bottom,z1);val b=floatArrayOf(x2,bottom,z2)
                val c=floatArrayOf(x2,top,z2);val d=floatArrayOf(x1,top,z1)
                val low=1f-(bottom-.64f)/(top-.64f)
                val points=listOf(a,b,c,a,c,d);val uv=listOf(u1 to low,u2 to low,u2 to 0f,u1 to low,u2 to 0f,u1 to 0f)
                for(i in points.indices){val p=points[i];g.vertex(p[0],p[1],p[2],nx,0f,nz,floatArrayOf(uv[i].first,uv[i].second,0f))}
            }
            face(-w+.025f,TheaterWorld.BACK+.32f,-w+.025f,TheaterWorld.FRONT-.32f,1f,0f)
            face(w-.025f,TheaterWorld.FRONT-.32f,w-.025f,TheaterWorld.BACK+.32f,-1f,0f)
            face(-w,TheaterWorld.FRONT-.32f,w,TheaterWorld.FRONT-.32f,0f,-1f)
            face(w,TheaterWorld.BACK+.32f,-w,TheaterWorld.BACK+.32f,0f,1f)
            for(b in TheaterWorld.walls){
                face(b.minX,b.maxZ+.025f,b.maxX,b.maxZ+.025f,0f,1f)
                face(b.maxX,b.minZ-.025f,b.minX,b.minZ-.025f,0f,-1f)
            }
            for(s in 1..4){val z=TheaterWorld.sectionFront(s)
                face(-4f,z+.625f,4f,z+.625f,0f,1f,12f)
                face(4f,z-.625f,-4f,z-.625f,0f,-1f,12f)
            }
            return g.array()
        }
        fun building():FloatArray {
            val g=HallGeometry();val width=TheaterWorld.halfWidth;val back=TheaterWorld.BACK;val front=TheaterWorld.FRONT;val roof=TheaterWorld.CEILING
            val middle=(front+back)/2;val depth=front-back;val wall=floatArrayOf(.47f,.405f,.31f);val beam=floatArrayOf(.34f,.31f,.25f);val dark=floatArrayOf(.19f,.17f,.14f)
            val ceiling=floatArrayOf(.76f,.72f,.62f)
            fun c(r:Float,b:Float,d:Float)=floatArrayOf(r,b,d)
            g.box(0f,-.22f,middle,width*2+.8f,.22f,depth,floatArrayOf(-1f,0f,0f))
            for(s in listOf(-1f,1f)){
                g.box(s*(width+.35f),0f,middle,.7f,roof,depth,wall)
                g.box(s*(width-.2f),0f,middle,.35f,.6f,depth,dark)
                // Gallery ledges and carved strata add depth to the long side walls.
                for(h in listOf(3.4f,7.4f,13f,20f))g.box(s*(width-.14f),h,middle,.3f,.16f,depth,c(.56f,.47f,.34f))
                g.box(s*(width-1.2f),11.4f,middle,2.4f,.3f,depth,beam)
                g.box(s*(width-2.1f),12.7f,middle,.07f,.06f,depth,c(.45f,.36f,.23f))
            }
            g.box(0f,0f,front,width*2,roof,.6f,wall);g.box(0f,0f,back,width*2,roof,.6f,wall)
            // Continuous clerestory glazing: actual openings between opaque roof strips.
            var z=floor(back/24f)*24f
            while(z<front+24f){
                val lo=max(back,z-4f);val hi=min(front,z+4f)
                if(hi>lo){
                    g.box(0f,roof,(lo+hi)/2,(width-7f)*2,.10f,hi-lo,c(.65f,.82f,1.4f))
                    for(s in listOf(-1f,1f))g.box(s*(width-3.5f),roof,(lo+hi)/2,7f,.35f,hi-lo,ceiling)
                    var x=-width+7f
                    while(x<=width-7f){g.box(x,roof-.2f,(lo+hi)/2,.10f,.30f,hi-lo,beam);x+=8f}
                    g.box(0f,roof-.2f,z,(width-7f)*2,.32f,.10f,beam)
                }
                val opaqueLo=max(back,z+4f);val opaqueHi=min(front,z+20f)
                if(opaqueHi>opaqueLo)g.box(0f,roof,(opaqueLo+opaqueHi)/2,width*2,.4f,opaqueHi-opaqueLo,ceiling)
                if(z>back&&z<front){
                    g.box(0f,roof-1.2f,z+4.3f,width*2,.8f,.4f,beam)
                    for(s in listOf(-1f,1f)){
                        g.box(s*(width-.55f),0f,z+4.3f,.65f,roof,.8f,beam)
                        g.box(s*(width-1.3f),5.0f,z+4.3f,.14f,1.3f,.42f,c(1.5f,1.27f,.8f))
                    }
                }
                z+=24f
            }
            for(b in TheaterWorld.walls)g.box((b.minX+b.maxX)/2,0f,(b.minZ+b.maxZ)/2,b.maxX-b.minX,roof,b.maxZ-b.minZ,wall)
            for(s in 1..4){val at=TheaterWorld.sectionFront(s)
                g.box(0f,12f,at,8f,roof-12f,1.2f,dark)
                for(sign in listOf(-1f,1f))g.box(sign*4f,0f,at+.68f,.24f,12f,.18f,c(.57f,.43f,.26f))
                g.box(0f,11.8f,at+.68f,8f,.18f,.18f,c(1.4f,1.17f,.8f))
                for(sign in listOf(-1f,1f)){
                    for(x in listOf(13f,27f,41f))if(x<width-5f)g.fossilPanel(sign*x,at+.66f)
                    var x=8f
                    while(x<width-2f){g.box(sign*x,0f,at+.7f,.35f,roof,.3f,beam);x+=8f}
                }
            }
            for(spot in TheaterWorld.spots)g.island(spot)
            TheaterWorld.gardens.forEachIndexed{i,p->g.plant(p,i)}
            for(b in TheaterWorld.benches){for(s in listOf(-1f,1f))g.box(b.x+s*.8f,0f,b.z,.10f,.42f,.6f,dark)
                for(i in 0..3)g.box(b.x,.43f,b.z-.28f+i*.18f,2.2f,.07f,.14f,c(.38f,.25f,.13f))
            }
            for(s in 0..4){
                val at=TheaterWorld.sectionFront(s)-5f
                g.person(-2.8f,at)
                // Metre gauge, human silhouette, and ordinary furniture provide scale anchors.
                g.box(2.8f,0f,at,.10f,3f,.12f,c(.26f,.24f,.19f))
                for(t in 0..30)g.box(2.8f,t*.1f,at+.07f,if(t%10==0).5f else .20f,.014f,.014f,c(.86f,.77f,.55f))
            }
            for(b in TheaterWorld.illustrationBoards){
                g.box(b.x,0f,b.z,.42f,.05f,.34f,c(.28f,.22f,.15f))
                g.box(b.x,.05f,b.z,.055f,b.y,.055f,c(.52f,.40f,.23f))
                g.box(b.x,b.y-.035f,b.z,b.width+.07f,b.height+.07f,.06f,c(.58f,.44f,.24f))
            }
            // Entrance desk sits behind the visitor rather than in the circulation aisle.
            g.box(0f,0f,9.3f,5.5f,1.05f,1.5f,c(.36f,.28f,.19f));g.box(0f,1.05f,9.3f,5.8f,.10f,1.7f,c(.54f,.47f,.35f))
            return g.array()
        }
        fun illustrations():FloatArray {
            val g=HallGeometry()
            for(b in TheaterWorld.illustrationBoards){
                val u=(b.spot.index%10)*204f/2048f;val v=(b.spot.index/10)*204f/2048f
                val uv=listOf(floatArrayOf(u,v+204f/2048f,0f),floatArrayOf(u+204f/2048f,v+204f/2048f,0f),floatArrayOf(u+204f/2048f,v,0f),floatArrayOf(u,v,0f))
                // Both faces remain readable while walking around the stand.
                for(back in listOf(false,true)){
                    val z=b.z+if(back)-.036f else .036f
                    val coords=listOf(floatArrayOf(b.x-b.width/2,b.y,z),floatArrayOf(b.x+b.width/2,b.y,z),floatArrayOf(b.x+b.width/2,b.y+b.height,z),floatArrayOf(b.x-b.width/2,b.y+b.height,z))
                    for(i in listOf(0,1,2,0,2,3)){val p=coords[if(back)listOf(1,0,3,2)[i] else i];g.vertex(p[0],p[1],p[2],0f,0f,if(back)-1f else 1f,uv[i])}
                }
            }
            return g.array()
        }
        fun labels():FloatArray {
            val g=HallGeometry();val side=2048f;val tw=256f;val th=128f
            fun board(index:Int,x:Float,y:Float,z:Float,width:Float,height:Float,back:Boolean=false){
                val u=(index%8)*tw/side;val v=(index/8)*th/side
                val coords=listOf(floatArrayOf(x-width/2,y,z),floatArrayOf(x+width/2,y,z),floatArrayOf(x+width/2,y+height,z),floatArrayOf(x-width/2,y+height,z))
                val uv=listOf(floatArrayOf(u,v+th/side,0f),floatArrayOf(u+tw/side,v+th/side,0f),floatArrayOf(u+tw/side,v,0f),floatArrayOf(u,v,0f))
                for(i in listOf(0,1,2,0,2,3)){val p=coords[if(back)listOf(1,0,3,2)[i] else i];g.vertex(p[0],p[1],p[2],0f,0f,if(back)-1f else 1f,uv[i])}
            }
            for(s in TheaterWorld.spots){
                // Tabletop labels sit below the specimen, so small bones remain visible.
                val small=s.dimensions.baseY>.5f
                val w=min(if(small)1.2f else 1.6f,s.halfWidth*1.8f)
                val y=if(small).42f else .98f;val h=if(small).40f else .48f
                board(s.index,s.x,y,s.base.maxZ,w,h);board(s.index,s.x,y,s.base.minZ,w,h,true)
            }
            for(s in 0..4){val z=if(s==0)-25f else TheaterWorld.sectionFront(s)+.7f;board(100+s,0f,8.5f,z,5f,1.65f)
                board(105,2.8f,3.05f,TheaterWorld.sectionFront(s)-4.92f,1.2f,.60f)
            }
            return g.array()
        }
    }
}
