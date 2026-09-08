package io.github.hatake716.dinobones.model

import kotlin.math.*

/** Original educational skull surfaces. Openings are geometric, not painted dots.
 * These are comparative reconstructions, not measurements or copies of a fossil scan. */
internal object SkullSculptor {
    private data class P(val x:Float,val y:Float) {
        operator fun plus(p:P)=P(x+p.x,y+p.y)
        operator fun minus(p:P)=P(x-p.x,y-p.y)
        operator fun times(t:Float)=P(x*t,y*t)
    }
    private data class Hole(val x:Float,val y:Float,val rx:Float,val ry:Float)
    fun build(b:Builder,h:V,length:Float,height:Float,width:Float,kind:String,teeth:Boolean,herbivore:Boolean){
        val narrow=kind in listOf("spinosaurus","mosasaurus","pteranodon")
        val ptero=kind=="pteranodon"
        val bird=kind=="archaeopteryx"
        val beak=herbivore||ptero||bird
        fun p(x:Float,y:Float,z:Float)=h+V(x*length,y*height,z*width)
        fun span(x:Float)=when {
            x<0f -> .65f+(x+.18f)*1.25f
            x<.28f -> .87f+.13f*sin(x/.28f*PI.toFloat())
            else -> if(kind=="parasaurolophus") .94f-.27f*((x-.28f)/.77f) else .96f-.72f*((x-.28f)/.77f)
        }.coerceAtLeast(.16f)
        val outer=listOf(P(-.18f,-.34f),P(-.19f,.16f),P(-.07f,.46f),P(.20f,.53f),P(.47f,.40f),P(.77f,if(narrow).22f else .32f),P(.94f,.13f),P(1.05f,-.12f),P(1.00f,-.35f),P(.65f,-.44f),P(.30f,-.47f),P(-.03f,-.42f))
        val holes=mutableListOf(Hole(.16f,.045f,if(bird).17f else .13f,.29f))
        if(!herbivore&&!ptero){holes.add(Hole(-.08f,.015f,.055f,.17f));holes.add(Hole(.49f,.025f,if(narrow).15f else .17f,.18f))}
        if(ptero){holes.clear();holes.add(Hole(.25f,.06f,.27f,.26f))}
        else holes.add(Hole(if(kind=="brachiosaurus").60f else .82f,if(kind=="brachiosaurus").27f else .145f,.09f,.075f))
        fun distance(q:P,outline:List<P>,voids:List<Hole>):Float {
            var inside=false;var d=Float.MAX_VALUE
            for(i in outline.indices){val a=outline[i];val c=outline[(i+1)%outline.size];val ac=c-a;val aq=q-a
                if((a.y>q.y)!=(c.y>q.y)&&q.x<(c.x-a.x)*(q.y-a.y)/(c.y-a.y)+a.x)inside=!inside
                val t=((aq.x*ac.x+aq.y*ac.y)/(ac.x*ac.x+ac.y*ac.y)).coerceIn(0f,1f);val e=q-(a+ac*t);d=min(d,sqrt(e.x*e.x+e.y*e.y))
            }
            var signed=if(inside)d else -d
            for(v in voids){val ellipse=sqrt(((q.x-v.x)/v.rx).pow(2)+((q.y-v.y)/v.ry).pow(2))-1f;signed=min(signed,ellipse*min(v.rx,v.ry))}
            return signed
        }
        fun side(outline:List<P>,voids:List<Hole>,s:Float,jaw:Boolean){
            val loX=outline.minOf{it.x};val hiX=outline.maxOf{it.x};val loY=outline.minOf{it.y};val hiY=outline.maxOf{it.y}
            fun z(q:P,layer:Float):Float {
                val section=if(jaw)((q.y+.74f)/.38f) else ((q.y+.02f)/.58f)
                val contour=span(q.x)*(if(jaw).82f else 1f)*(.70f+.30f*sqrt(max(.08f,1f-section*section)))
                val relief=.010f*sin(q.x*43f+q.y*23f)*sin(q.y*37f-q.x*19f)
                val depth=if(jaw).085f else .095f
                return s*(contour+.035f*cos(q.y*5f)+relief+layer*depth)
            }
            fun at(q:P,layer:Float)=p(q.x,q.y,z(q,layer))
            fun norm(q:P,layer:Float):V {
                val e=.001f;val a=at(q+P(e,0f),layer)-at(q-P(e,0f),layer);val c=at(q+P(0f,e),layer)-at(q-P(0f,e),layer)
                return a.cross(c).unit()*(s*layer)
            }
            fun triangle(a:P,c:P,d:P){
                val points=listOf(a,c,d);val clipped=mutableListOf<P>();val boundary=mutableListOf<P>()
                for(i in 0..2){val v=points[i];val w=points[(i+1)%3];val dv=distance(v,outline,voids);val dw=distance(w,outline,voids)
                    if(dv>=0f)clipped.add(v)
                    if((dv>=0f)!=(dw>=0f)){val cut=v+(w-v)*(dv/(dv-dw));clipped.add(cut);boundary.add(cut)}
                }
                if(clipped.size<3)return
                for(layer in listOf(-1f,1f))for(i in 1 until clipped.lastIndex){val q0=clipped[0];val q1=clipped[i];val q2=clipped[i+1]
                    b.smooth(at(q0,layer),at(q1,layer),at(q2,layer),norm(q0,layer),norm(q1,layer),norm(q2,layer),1)
                }
                if(boundary.size==2){val u=boundary[0];val v=boundary[1];b.tri(at(u,-1f),at(v,-1f),at(u,1f),1);b.tri(at(u,1f),at(v,-1f),at(v,1f),1)}
            }
            val nx=if(b.theater)24 else 60;val ny=if(b.theater){if(jaw)8 else 16}else if(jaw)18 else 36
            for(ix in 0 until nx)for(iy in 0 until ny){val x=loX+(hiX-loX)*ix/nx;val y=loY+(hiY-loY)*iy/ny;val dx=(hiX-loX)/nx;val dy=(hiY-loY)/ny
                triangle(P(x,y),P(x+dx,y),P(x+dx,y+dy));triangle(P(x,y),P(x+dx,y+dy),P(x,y+dy))
            }
        }
        val jaw=if(ptero)listOf(P(-.13f,-.49f),P(.92f,-.50f),P(1.04f,-.59f),P(.83f,-.69f),P(-.10f,-.74f)) else
            listOf(P(-.16f,-.49f),P(.15f,-.53f),P(.66f,-.61f),P(.98f,-.56f),P(1.04f,-.66f),P(.80f,-.87f),P(.24f,-1.00f),P(-.09f,-.84f))
        val jawHoles=if(!herbivore&&!ptero)listOf(Hole(.25f,-.76f,.15f,.075f)) else emptyList()
        for(s in listOf(-1f,1f)){side(outer,holes,s,false);side(jaw,jawHoles,s,true)}
        // Curved nasal roof and parietal roof bridge the two skull walls.
        fun roof(start:Float,end:Float,y:Float){
            for(i in 0 until 22)for(j in 0 until 12){
                fun surface(u:Int,v:Int):V {val x=start+(end-start)*u/22f;val t=-1f+v/6f;return p(x,y+.10f*(1-t*t)-.28f*x,span(x)*t)}
                val a=surface(i,j);val c=surface(i+1,j);val d=surface(i+1,j+1);val e=surface(i,j+1)
                b.tri(a,c,d,1);b.tri(a,d,e,1)
            }
        }
        roof(-.12f,.03f,.46f);roof(.32f,.94f,.47f)
        b.ball(p(-.13f,-.02f,0f),length*.085f,height*.23f,width*.54f,1)
        // Lower-jaw symphysis and palate. The central palate remains open.
        b.curve(listOf(p(.87f,-.82f,-.25f),p(1f,-.65f,0f),p(.87f,-.82f,.25f)),.035f,1)
        for(s in listOf(-1f,1f))b.curve(listOf(p(-.06f,-.20f,s*.27f),p(.32f,-.18f,s*.34f),p(.70f,-.28f,s*.22f)),.025f,1)
        if(teeth)for(s in listOf(-1f,1f)){
            val count=if(narrow)18 else if(herbivore)13 else 12
            for(i in 0 until count){val t=i.toFloat()/(count-1);val x=if(herbivore).35f+t*.33f else .31f+t*.65f
                val tooth=height*(if(herbivore).075f else if(bird).12f else .18f+.08f*sin(t*PI.toFloat()))
                val radius=if(herbivore).012f else if(narrow).019f else .025f
                for(lower in listOf(false,true)){val root=p(x,if(lower)-.62f else -.41f,s*span(x)*.94f);val direction=if(lower)1f else -1f
                    b.claw((0..7).map{j->val u=j/7f;root+V(-tooth*.21f*u*u,direction*tooth*u,-s*tooth*.045f*u*u)},radius,1)
                }
            }
        }
        if(beak){
            // Edges of the bony premaxilla and predentary, without a keratin covering.
            for(s in listOf(-1f,1f))b.curve(listOf(p(.92f,.07f,s*.3f),p(1.05f,-.14f,s*.20f),p(.99f,-.33f,s*.25f)),.023f,1)
        }
    }
}
