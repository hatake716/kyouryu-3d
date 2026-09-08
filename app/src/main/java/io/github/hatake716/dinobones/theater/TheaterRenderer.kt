package io.github.hatake716.dinobones.theater

import android.content.Context
import android.graphics.*
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.model.FossilMaterials
import io.github.hatake716.dinobones.model.FossilSurface
import io.github.hatake716.dinobones.model.FossilUniforms
import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

internal data class DisplayInfo(val id:String,val scale:Float,val minY:Float,val maxY:Float,val triangles:Int)
private data class GpuPart(val vertex:Int,val index:Int,val count:Int,val bytes:Int)
private data class GpuDisplay(val parts:List<GpuPart>){val bytes get()=parts.sumOf{it.bytes}}

class TheaterRenderer(private val context:Context,private val dispatch:((()->Unit)->Unit),private val report:(String?)->Unit):GLSurfaceView.Renderer {
    @Volatile var pose=WalkPose()
    @Volatile var forward=0f
    @Volatile var strafe=0f
    @Volatile var frameCount=0L
    @Volatile var renderedIds:Set<String> = emptySet()
    @Volatile var loadedCount=0
    @Volatile var lastFrameMillis=0f
    @Volatile var failed:String?=null
    @Volatile var glError=GL_NO_ERROR
    @Volatile var muralReady=false
        private set
    @Volatile var onUpdate:((WalkPose,ExhibitHit?,Int)->Unit)?=null
    private val info=context.assets.open("theater/manifest.json").bufferedReader().use{JSONArray(it.readText())}.let{a->(0 until a.length()).associate{i->val o=a.getJSONObject(i);o.getString("id") to DisplayInfo(o.getString("id"),o.getDouble("scale").toFloat(),o.getJSONArray("min").getDouble(1).toFloat(),o.getJSONArray("max").getDouble(1).toFloat(),o.getInt("triangles"))}}
    val exhibitHeights=ExhibitScales.entries.mapValues{(_,d)->d.baseY+d.height}
    private val worker=Executors.newSingleThreadExecutor()
    private var loading:String?=null
    private val errors=mutableSetOf<String>()
    private val displays=LinkedHashMap<String,GpuDisplay>(24,.75f,true)
    private var desired=emptyList<ExhibitSpot>()
    @Volatile private var generation=0
    private data class Pending(val token:Int,val id:String,val mesh:TheaterMesh?)
    private val pending=ConcurrentLinkedQueue<Pending>()
    @Volatile private var released=false
    private var eglDisplay:EGLDisplay?=null
    private var eglContext:EGLContext?=null
    private var eglConfig:EGLConfig?=null
    private var program=0;private var pos=0;private var normal=0;private var color=0;private var vpLoc=0;private var modelLoc=0
    private var fossilUniforms:FossilUniforms?=null
    @Volatile var renderedFossilIds:Set<String> = emptySet();private set
    private var room=0;private var roomCount=0;private var signs=0;private var signCount=0;private var atlas=0
    private var illustrations=0;private var illustrationCount=0;private var illustrationTexture=0
    @Volatile var illustrationsReady=false;private set
    private var mural=0;private var muralCount=0;private var forestTexture=0
    private var width=1;private var height=1;private var lastTime=0L;private var lastUpdate=0L
    private val transfer=ByteBuffer.allocateDirect(256*1024).order(ByteOrder.nativeOrder())
    private val projection=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16)
    private fun shader(type:Int,source:String):Int{val id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);val ok=IntArray(1);glGetShaderiv(id,GL_COMPILE_STATUS,ok,0);check(ok[0]!=0){glGetShaderInfoLog(id)};return id}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        if(released)return
        val egl=EGLContext.getEGL() as EGL10;eglDisplay=egl.eglGetCurrentDisplay();eglContext=egl.eglGetCurrentContext();eglConfig=config
        generation++;pending.clear();displays.clear();loading=null;errors.clear();failed=null;loadedCount=0;renderedIds=emptySet();lastTime=0;muralReady=false;illustrationsReady=false
        val vertex=shader(GL_VERTEX_SHADER,"""
            uniform mat4 uVP;uniform mat4 uModel;
            attribute vec3 aPosition;attribute vec3 aNormal;attribute vec3 aColor;
            varying vec3 vPosition;varying vec3 vNormal;varying vec3 vColor;varying vec3 vLocal;
            void main(){vec4 p=uModel*vec4(aPosition,1.0);vPosition=p.xyz;vLocal=aPosition;vNormal=mat3(uModel)*aNormal;vColor=aColor;gl_Position=uVP*p;}
        """.trimIndent())
        val derivatives=if((glGetString(GL_EXTENSIONS)?:"").contains("GL_OES_standard_derivatives"))"#extension GL_OES_standard_derivatives : enable\n#define FOSSIL_DERIVATIVES\n" else ""
        val fragment=shader(GL_FRAGMENT_SHADER,derivatives+"""
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            varying vec3 vPosition;varying vec3 vNormal;varying vec3 vColor;varying vec3 vLocal;
            uniform vec3 uEye;uniform float uAtlas;uniform float uBone;uniform sampler2D uTexture;uniform float uSelected;uniform float uHallHalf;
            FOSSIL_MATERIAL_SHADER
            void main(){
                vec3 c=vColor;float emission=0.0;
                if(uAtlas>1.5){
                    // Mirror the panorama at each repeat; clamp sampling also works on GLES2.
                    vec2 uv=vec2(1.0-abs(mod(vColor.x,2.0)-1.0),clamp(vColor.y,0.0,1.0));
                    c=texture2D(uTexture,uv).rgb;emission=1.0;
                }else if(uAtlas>0.5){c=texture2D(uTexture,vColor.xy).rgb;emission=1.0;}
                else if(c.b>1.2){
                    float cloud=.5+.5*sin(vPosition.x*.11+sin(vPosition.z*.07))*sin(vPosition.z*.13);
                    c=mix(vec3(.66,.83,.94),vec3(.98,.98,.94),cloud*.70);emission=1.0;
                }else if(c.r<0.0){
                    vec2 tile=abs(fract(vPosition.xz)-.5);
                    float grout=step(.486,max(tile.x,tile.y))*(1.0-smoothstep(22.0,65.0,distance(vPosition,uEye)));
                    vec2 major=abs(fract(vPosition.xz*.1)-.5);
                    float joint=step(.497,max(major.x,major.y));
                    float grain=sin(vPosition.x*2.1+sin(vPosition.z*.33))*sin(vPosition.z*1.7)*.008;
                    c=mix(vec3(.38,.345,.285)+grain,vec3(.24,.215,.175),max(grout*.65,joint));
                }else if(c.r>1.1){emission=1.0;c=clamp(c,0.0,1.0);}
                else if(uBone<.5)c*=.97+.03*sin(vPosition.y*18.0+sin(vPosition.x*5.0+vPosition.z*3.0));
                vec3 n=normalize(vNormal);float fossilAo=1.0;
                if(uBone>.5&&uFossilOn>.5){
                    FossilSample fossil=fossilSurface(vLocal);c=fossil.color;fossilAo=fossil.occlusion;
                    n=fossilNormal(n,vLocal,fossil.relief);
                }
                vec3 sun=normalize(vec3(.35,1.0,.20));
                float key=max(dot(n,sun),0.0);
                if(uBone<.5&&c.g>max(c.r,c.b)*1.15)key=abs(dot(n,sun));
                float fill=max(dot(n,normalize(vec3(-.5,.45,-.45))),0.0);
                vec2 roofPoint=vPosition.xz+(32.0-vPosition.y)*vec2(.35,.20);
                float band=abs(mod(roofPoint.y+12.0,24.0)-12.0);
                float aperture=(1.0-smoothstep(3.6,4.15,band))*(1.0-smoothstep(uHallHalf-7.2,uHallHalf-6.8,abs(roofPoint.x)));
                float mullion=smoothstep(.06,.17,abs(mod(roofPoint.x+uHallHalf-7.0+4.0,8.0)-4.0))*smoothstep(.04,.12,band);
                float sunlight=aperture*mullion;
                float light=uBone>.5?.68+key*.34+fill*.18:.64+key*.25+fill*.15;
                if(uBone<.5&&n.y<-.5&&vPosition.y>24.0)light=max(light,.92);
                c*=mix(light,1.0,emission)*fossilAo;
                c+=vec3(1.0,.91,.70)*sunlight*key*(1.0-emission)*(uBone>.5?.16:.22);
                if(uBone>.5&&uSelected>.5)c=mix(c,vec3(.95,.75,.43),.09);
                float fog=smoothstep(75.0,185.0,distance(vPosition,uEye));
                if(uAtlas>1.5)fog*=.25;
                gl_FragColor=vec4(mix(c,vec3(.38,.42,.34),fog),1.0);
            }
        """.trimIndent().replace("FOSSIL_MATERIAL_SHADER",FossilSurface.GLSL))
        program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program)
        val ok=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,ok,0);check(ok[0]!=0){glGetProgramInfoLog(program)};glDeleteShader(vertex);glDeleteShader(fragment)
        fossilUniforms=FossilUniforms(program)
        pos=glGetAttribLocation(program,"aPosition");normal=glGetAttribLocation(program,"aNormal");color=glGetAttribLocation(program,"aColor");vpLoc=glGetUniformLocation(program,"uVP");modelLoc=glGetUniformLocation(program,"uModel")
        val geometry=HallGeometry.building();room=uploadVertices(geometry);roomCount=geometry.size/9
        val labels=HallGeometry.labels();signs=uploadVertices(labels);signCount=labels.size/9
        val walls=HallGeometry.forestWalls();mural=uploadVertices(walls);muralCount=walls.size/9
        val pictures=HallGeometry.illustrations();illustrations=uploadVertices(pictures);illustrationCount=pictures.size/9
        illustrationTexture=createIllustrationAtlas();illustrationsReady=true
        atlas=createAtlas();forestTexture=createForestTexture();muralReady=true
        glEnable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glClearColor(.38f,.42f,.34f,1f)
    }
    private fun uploadVertices(data:FloatArray):Int {
        val ids=IntArray(1);glGenBuffers(1,ids,0);glBindBuffer(GL_ARRAY_BUFFER,ids[0]);glBufferData(GL_ARRAY_BUFFER,data.size*4,null,GL_STATIC_DRAW)
        val buffer=transfer.asFloatBuffer();var offset=0
        while(offset<data.size){val n=min(buffer.capacity(),data.size-offset);buffer.clear();buffer.put(data,offset,n);buffer.flip();glBufferSubData(GL_ARRAY_BUFFER,offset*4,n*4,buffer);offset+=n}
        return ids[0]
    }
    private fun upload(mesh:TheaterMesh)=GpuDisplay(mesh.parts.map{part->
        val v=uploadVertices(part.vertices);val ids=IntArray(1);glGenBuffers(1,ids,0);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ids[0])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,part.indices.size*2,null,GL_STATIC_DRAW)
        val data=transfer.asShortBuffer();var offset=0
        while(offset<part.indices.size){val count=min(data.capacity(),part.indices.size-offset);data.clear();data.put(part.indices,offset,count);data.flip();glBufferSubData(GL_ELEMENT_ARRAY_BUFFER,offset*2,count*2,data);offset+=count}
        GpuPart(v,ids[0],part.indices.size,part.vertices.size*4+part.indices.size*2)
    })
    private fun createAtlas():Int {
        val bitmap=Bitmap.createBitmap(2048,2048,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap);c.drawColor(Color.rgb(38,29,22))
        val p=Paint(Paint.ANTI_ALIAS_FLAG);val catalog=Catalog.load(context).associateBy{it.id}
        fun line(text:String,x:Float,y:Float,max:Float,size:Float){p.textSize=size;while(p.measureText(text)>max&&p.textSize>10)p.textSize-=1;c.drawText(text,x,y,p)}
        for(i in 0..105){val x=(i%8)*256f;val y=(i/8)*128f
            p.color=Color.rgb(171,126,67);c.drawRect(x+10,y+10,x+246,y+12,p)
            if(i<100){val d=catalog.getValue(TheaterWorld.spots[i].id);p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.color=Color.rgb(238,214,166);line(d.name,x+12,y+51,232f,26f)
                p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);p.color=Color.rgb(190,174,143);line(d.latin,x+12,y+80,232f,16f);line(ExhibitScales.entries.getValue(d.id).label,x+12,y+110,232f,14f)
            }else if(i==105){p.color=Color.rgb(235,219,179);line("1 m / 床の目地",x+12,y+49,232f,23f);line("人の高さ 1.7 m",x+12,y+90,232f,23f)}else{val s=i-100;p.color=Color.rgb(216,170,104);line("GALLERY  0${s+1}",x+12,y+45,232f,22f);p.color=Color.rgb(243,225,192);line(TheaterWorld.sectionNames[s],x+12,y+86,232f,26f)}
        }
        return uploadTexture(bitmap)
    }
    private fun createIllustrationAtlas():Int {
        val bitmap=Bitmap.createBitmap(2048,2048,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.drawColor(Color.rgb(236,221,191))
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val catalog=Catalog.load(context).associateBy{it.id}
        for(s in TheaterWorld.spots){
            val art=checkNotNull(catalog.getValue(s.id).artwork)
            val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            context.assets.open(art.asset).use{BitmapFactory.decodeStream(it,null,options)}
            options.inJustDecodeBounds=false;options.inSampleSize=1
            while(max(options.outWidth,options.outHeight)/options.inSampleSize>256)options.inSampleSize*=2
            val picture=context.assets.open(art.asset).use{checkNotNull(BitmapFactory.decodeStream(it,null,options))}
            val x=(s.index%10)*204f;val y=(s.index/10)*204f
            val factor=min(184f/picture.width,151f/picture.height)
            val w=picture.width*factor;val h=picture.height*factor
            canvas.drawBitmap(picture,null,RectF(x+102-w/2,y+82-h/2,x+102+w/2,y+82+h/2),paint);picture.recycle()
            paint.color=Color.rgb(66,46,28);paint.textAlign=Paint.Align.CENTER;paint.textSize=18f;paint.typeface=Typeface.DEFAULT_BOLD
            canvas.drawText("復元イラスト",x+102,y+180,paint)
            paint.textSize=10f;paint.typeface=Typeface.DEFAULT;canvas.drawText("タップして拡大・出典",x+102,y+195,paint)
        }
        return uploadTexture(bitmap)
    }
    private fun createForestTexture():Int {
        val original=context.assets.open("theater/mesozoic-forest.png").use{checkNotNull(BitmapFactory.decodeStream(it))}
        // A power-of-two upload allows trilinear mipmaps on every supported GLES2 GPU.
        val bitmap=Bitmap.createScaledBitmap(original,2048,1024,true)
        if(bitmap!==original)original.recycle()
        return uploadTexture(bitmap)
    }
    private fun uploadTexture(bitmap:Bitmap):Int {
        val ids=IntArray(1);glGenTextures(1,ids,0);glBindTexture(GL_TEXTURE_2D,ids[0]);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0);glGenerateMipmap(GL_TEXTURE_2D);bitmap.recycle();return ids[0]
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){width=w.coerceAtLeast(1);height=h.coerceAtLeast(1);glViewport(0,0,width,height)}
    private fun attributes(buffer:Int){glBindBuffer(GL_ARRAY_BUFFER,buffer);for(i in listOf(pos,normal,color))glEnableVertexAttribArray(i)
        glVertexAttribPointer(pos,3,GL_FLOAT,false,36,0);glVertexAttribPointer(normal,3,GL_FLOAT,false,36,12);glVertexAttribPointer(color,3,GL_FLOAT,false,36,24)}
    override fun onDrawFrame(gl:GL10?){
        if(released)return
        val now=System.nanoTime();
        pending.poll()?.let{result->if(result.token==generation){
            loading=null
            if(result.mesh!=null){if(desired.any{it.id==result.id}){displays[result.id]=upload(result.mesh);trim()}}
            else{errors.add(result.id);failed=result.id;dispatch{report(result.id)}}
        }};val dt=if(lastTime==0L)0f else (now-lastTime)/1e9f;lastTime=now
        val camera=synchronized(this){pose=TheaterWorld.move(pose,strafe,forward,dt);pose};val yaw=camera.yaw*PI.toFloat()/180;val pitch=camera.pitch*PI.toFloat()/180
        Matrix.perspectiveM(projection,0,65f,width.toFloat()/height,.06f,200f)
        Matrix.setLookAtM(view,0,camera.x,TheaterWorld.EYE_HEIGHT,camera.z,camera.x+sin(yaw)*cos(pitch),TheaterWorld.EYE_HEIGHT-sin(pitch),camera.z-cos(yaw)*cos(pitch),0f,1f,0f)
        Matrix.multiplyMM(vp,0,projection,0,view,0);glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);glUseProgram(program);glUniformMatrix4fv(vpLoc,1,false,vp,0)
        glUniform3f(glGetUniformLocation(program,"uEye"),camera.x,TheaterWorld.EYE_HEIGHT,camera.z)
        val target=TheaterWorld.pickTarget(camera,0f,0f,width.toFloat()/height,exhibitHeights)
        glUniform1f(glGetUniformLocation(program,"uHallHalf"),TheaterWorld.halfWidth);glUniform1f(glGetUniformLocation(program,"uSelected"),0f)
        Matrix.setIdentityM(model,0);glUniformMatrix4fv(modelLoc,1,false,model,0);glUniform1f(glGetUniformLocation(program,"uAtlas"),0f);glUniform1f(glGetUniformLocation(program,"uBone"),0f)
        attributes(room);glDrawArrays(GL_TRIANGLES,0,roomCount)
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,forestTexture);glUniform1i(glGetUniformLocation(program,"uTexture"),0);glUniform1f(glGetUniformLocation(program,"uAtlas"),2f)
        attributes(mural);glDrawArrays(GL_TRIANGLES,0,muralCount)
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,atlas);glUniform1i(glGetUniformLocation(program,"uTexture"),0);glUniform1f(glGetUniformLocation(program,"uAtlas"),1f);attributes(signs);glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glDrawArrays(GL_TRIANGLES,0,signCount);glDisable(GL_CULL_FACE)
        glBindTexture(GL_TEXTURE_2D,illustrationTexture);attributes(illustrations);glEnable(GL_CULL_FACE);glDrawArrays(GL_TRIANGLES,0,illustrationCount);glDisable(GL_CULL_FACE)
        glUniform1f(glGetUniformLocation(program,"uAtlas"),0f);glUniform1f(glGetUniformLocation(program,"uBone"),1f)
        fun distance(s:ExhibitSpot)=hypot(camera.x-camera.x.coerceIn(s.base.minX,s.base.maxX),camera.z-camera.z.coerceIn(s.base.minZ,s.base.maxZ))
        desired=TheaterWorld.spots.sortedBy{distance(it)}.take(18).filter{distance(it)<90f}
        val drawn=mutableSetOf<String>();val fossilIds=mutableSetOf<String>()
        for(s in desired){
            val dx=s.x-camera.x;val dz=s.z-camera.z;if(dx*sin(yaw)-dz*cos(yaw)< -(s.halfWidth+s.halfDepth))continue
            val display=displays[s.id]?:continue;val d=info.getValue(s.id)
            val fossil=FossilMaterials.forModel(s.id);fossilUniforms?.bind(fossil);if(fossil!=null)fossilIds.add(s.id)
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,s.x,s.dimensions.baseY-d.minY*d.scale,s.z);Matrix.scaleM(model,0,d.scale,d.scale,d.scale);glUniformMatrix4fv(modelLoc,1,false,model,0)
            glUniform1f(glGetUniformLocation(program,"uSelected"),if(s.id==target?.spot?.id)1f else 0f)
            for(part in display.parts){attributes(part.vertex);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,part.index);glDrawElements(GL_TRIANGLES,part.count,GL_UNSIGNED_SHORT,0)}
            drawn.add(s.id)
        }
        renderedIds=drawn;renderedFossilIds=fossilIds;frameCount++;loadedCount=displays.size
        loadNext()
        if(now-lastUpdate>200_000_000){lastUpdate=now;val update=onUpdate;dispatch{update?.invoke(camera,target,drawn.size)}}
        val error=glGetError();if(error!=GL_NO_ERROR)glError=error
        lastFrameMillis=(System.nanoTime()-now)/1e6f
    }
    private fun loadNext(){
        if(loading!=null||worker.isShutdown)return
        val next=desired.firstOrNull{it.id !in displays&&it.id !in errors}?:return
        val token=generation;loading=next.id
        worker.execute {
            try {
                val mesh=TheaterMesh.read(context.assets.open("theater/${next.id}.tmsz"))
                if(token==generation&&!worker.isShutdown)pending.add(Pending(token,next.id,mesh))
            }catch(e:Exception){if(token==generation&&!worker.isShutdown)pending.add(Pending(token,next.id,null))}
        }
    }
        private fun trim(){
        val wanted=desired.map{it.id}.toSet()
        while(displays.size>20||displays.values.sumOf{it.bytes}>48*1024*1024){val key=displays.keys.firstOrNull{it !in wanted}?:displays.keys.first();val old=displays.remove(key)?:break
            for(p in old.parts)glDeleteBuffers(2,intArrayOf(p.vertex,p.index),0)
        }
    }
    /** A paused GLSurfaceView keeps its context but unbinds its window surface.
     * Bind a tiny compatible pbuffer on that same GL thread before deleting objects. */
    fun disposeGpuWithContext(){
        if(program==0)return
        val egl=EGLContext.getEGL() as EGL10
        val display=eglDisplay?:return;val context=eglContext?:return;val config=eglConfig?:return
        if(egl.eglGetCurrentContext()==context){disposeGpu();return}
        val buffer=egl.eglCreatePbufferSurface(display,config,intArrayOf(EGL10.EGL_WIDTH,1,EGL10.EGL_HEIGHT,1,EGL10.EGL_NONE))
        if(buffer==null||buffer==EGL10.EGL_NO_SURFACE)return
        try {
            if(egl.eglMakeCurrent(display,buffer,buffer,context)){
                disposeGpu();glFinish()
                egl.eglMakeCurrent(display,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_CONTEXT)
            }
        }finally{egl.eglDestroySurface(display,buffer)}
    }
    private fun disposeGpu(){
        for(display in displays.values)for(p in display.parts)glDeleteBuffers(2,intArrayOf(p.vertex,p.index),0)
        displays.clear();glDeleteBuffers(4,intArrayOf(room,signs,mural,illustrations),0);glDeleteTextures(3,intArrayOf(atlas,forestTexture,illustrationTexture),0);glDeleteProgram(program)
        illustrations=0;illustrationTexture=0;illustrationsReady=false;room=0;signs=0;atlas=0;mural=0;forestTexture=0;muralReady=false;program=0;fossilUniforms=null;renderedIds=emptySet();renderedFossilIds=emptySet();loadedCount=0
    }
    fun retry(){errors.clear();failed=null;dispatch{report(null)}}
    fun release(){released=true;generation++;pending.clear();worker.shutdownNow();onUpdate=null;forward=0f;strafe=0f}
}
