package io.github.hatake716.dinobones.model

import android.content.Context
import io.github.hatake716.dinobones.data.EarthColors
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.opengl.GLUtils
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

data class CameraPose(val yaw: Float = 18f, val pitch: Float = 9f, val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    fun orbit(dx: Float,dy: Float) = copy(yaw=(yaw+dx)%360f,pitch=(pitch+dy).coerceIn(-89f,89f))
    fun scale(factor: Float) = copy(zoom=(zoom*factor).coerceIn(.55f,5f))
    fun pan(dx: Float,dy: Float) = copy(panX=(panX+dx).coerceIn(-6f,6f),panY=(panY+dy).coerceIn(-6f,6f))
}
class SkeletonView(context: Context): GLSurfaceView(context) {
    val museumRenderer = MuseumRenderer(context.applicationContext)
    private var lastX=0f;private var lastY=0f;private var lastSpan=0f;private var pointers=0
    init {
        setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,0,24,0)
        preserveEGLContextOnPause=true;setRenderer(museumRenderer);renderMode=RENDERMODE_WHEN_DIRTY
        contentDescription="3D骨格。1本指で回転、2本指で拡大縮小と平行移動。下のボタンでも操作できます。"
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    fun setModel(mesh: Skeleton){museumRenderer.pendingMesh=mesh;requestRender()}
    private var artworkFrame=floatArrayOf(0f,0f,1f,1f)
    fun background(bitmap:Bitmap?,strength:Float,frame:FloatArray,fill:Boolean){museumRenderer.artwork=bitmap;museumRenderer.artworkStrength=strength;artworkFrame=frame;museumRenderer.backgroundFill=fill;updateBackgroundBounds();requestRender()}
    private fun updateBackgroundBounds(){
        val location=IntArray(2);getLocationInWindow(location)
        val w=artworkFrame[2].coerceAtLeast(1f);val h=artworkFrame[3].coerceAtLeast(1f)
        museumRenderer.backgroundBounds=floatArrayOf((location[0]-artworkFrame[0])/w,(location[1]-artworkFrame[1])/h,width/w,height/h,w/h)
    }
    override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int){super.onLayout(changed,left,top,right,bottom);updateBackgroundBounds();requestRender()}
    fun camera(pose: CameraPose){museumRenderer.pose=pose;requestRender()}
    fun highlight(part: Int,isolate: Boolean){museumRenderer.selected=part;museumRenderer.isolate=isolate;requestRender()}
    fun rotate(auto: Boolean){museumRenderer.auto=auto;renderMode=if(auto)RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY;requestRender()}
    override fun performClick(): Boolean {super.performClick();return true}
    override fun onDetachedFromWindow(){
        // Delete objects before GLSurfaceView exits its GL thread, even when already paused.
        val done=java.util.concurrent.CountDownLatch(1)
        queueEvent{try{museumRenderer.disposeGpuWithContext()}finally{done.countDown()}}
        done.await(1500,java.util.concurrent.TimeUnit.MILLISECONDS)
        super.onDetachedFromWindow()
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        val count=e.pointerCount;val x=(0 until count).sumOf {e.getX(it).toDouble()}.toFloat()/count;val y=(0 until count).sumOf {e.getY(it).toDouble()}.toFloat()/count
        val span=if(count>=2)hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0)) else 0f
        if(e.actionMasked==MotionEvent.ACTION_MOVE&&count==pointers){
            val dx=x-lastX;val dy=y-lastY
            museumRenderer.pose=if(count==1)museumRenderer.pose.orbit(-dx*.32f,dy*.27f) else museumRenderer.pose.scale(if(lastSpan>1f)span/lastSpan else 1f).pan(dx/width*8f/museumRenderer.pose.zoom,-dy/height*8f/museumRenderer.pose.zoom)
            requestRender()
        }
        if(e.actionMasked==MotionEvent.ACTION_UP){performClick();parent?.requestDisallowInterceptTouchEvent(false)}
        if(e.actionMasked==MotionEvent.ACTION_CANCEL)parent?.requestDisallowInterceptTouchEvent(false)
        pointers=if(e.actionMasked==MotionEvent.ACTION_POINTER_UP)0 else count;lastX=x;lastY=y;lastSpan=span;return true
    }
}
class MuseumRenderer(private val context:Context):GLSurfaceView.Renderer {
    private val baseRed=((EarthColors.Background shr 16) and 255)/255f
    private val baseGreen=((EarthColors.Background shr 8) and 255)/255f
    private val baseBlue=(EarthColors.Background and 255)/255f
    @Volatile var pose=CameraPose()
    @Volatile var pendingMesh:Skeleton?=null
    @Volatile var selected=0
    @Volatile var isolate=false
    @Volatile var auto=false
    @Volatile var frameCount=0L
    @Volatile var renderedVertexCount=0
    @Volatile var artwork:Bitmap?=null
    @Volatile var artworkStrength=.32f
    @Volatile var backgroundFill=true
    @Volatile var backgroundBounds=floatArrayOf(0f,0f,1f,1f,1f)
    @Volatile var backgroundReady=false
    private var uploadedArtwork:Bitmap?=null
    private var backgroundProgram=0;private var texture=0
    private val quad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(floatArrayOf(-1f,1f,-1f,-1f,1f,1f,1f,-1f));position(0)}
    private var firstMeshFrame=true
    private var mesh:Skeleton?=null
    private var framing:CameraFraming?=null
    @Volatile var fittedDistance=0f;private set
    @Volatile var viewportWidth=0;private set
    @Volatile var viewportHeight=0;private set
    private var program=0;private var buffer=0;private var floor=0;private var floorCount=0;private var width=1;private var height=1;private var lastFrame=0L
    private val projection=FloatArray(16);private val view=FloatArray(16);private val mvp=FloatArray(16)
    private var shadeBuffer=0;private var shadeTexture=0;private var shadeDepth=0;private var shadeWidth=0;private var shadeHeight=0
    @Volatile var contactShadowsReady=false
    private var uvLoc=0;private val boneTextures=IntArray(3);private var textured=false
    @Volatile var surfaceTexturesReady=false
    @Volatile var normalMappingSupported=false
    private var fossilUniforms:FossilUniforms?=null
    @Volatile var renderedMaterialId:String?=null;private set
    @Volatile var gpuResourcesReleased=false;private set
    private var eglDisplay:EGLDisplay?=null
    private var eglContext:EGLContext?=null
    private var eglConfig:EGLConfig?=null
    private var posLoc=0;private var normalLoc=0;private var groupLoc=0;private var matrixLoc=0;private var selectLoc=0;private var isolateLoc=0
    private fun shader(type: Int,src: String): Int {val s=glCreateShader(type);glShaderSource(s,src);glCompileShader(s);val ok=IntArray(1);glGetShaderiv(s,GL_COMPILE_STATUS,ok,0);check(ok[0]!=0){glGetShaderInfoLog(s)};return s}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        val egl=EGLContext.getEGL() as EGL10
        eglDisplay=egl.eglGetCurrentDisplay();eglContext=egl.eglGetCurrentContext();eglConfig=config;gpuResourcesReleased=false
        renderedVertexCount=0;firstMeshFrame=true
        normalMappingSupported=(glGetString(GL_EXTENSIONS)?:"").contains("GL_OES_standard_derivatives")
        val vs=shader(GL_VERTEX_SHADER,"""
            uniform mat4 uMatrix;
            attribute vec3 aPosition; attribute vec3 aNormal; attribute float aGroup; attribute vec2 aUv;
            varying vec3 vNormal; varying vec3 vPosition; varying float vGroup; varying vec2 vUv;
            void main(){gl_Position=uMatrix*vec4(aPosition,1.0);vPosition=aPosition;vNormal=aNormal;vGroup=aGroup;vUv=aUv;}
        """.trimIndent())
        val derivatives=if(normalMappingSupported)"#extension GL_OES_standard_derivatives : enable\n#define FOSSIL_DERIVATIVES\n" else ""
        val bump=if(normalMappingSupported)"""
            if(uTextured>0.5){
                vec3 dp1=dFdx(vPosition),dp2=dFdy(vPosition);vec2 duv1=dFdx(vUv),duv2=dFdy(vUv);
                vec3 p1=cross(dp2,n),p2=cross(n,dp1);
                vec3 t=p1*duv1.x+p2*duv2.x,b=p1*duv1.y+p2*duv2.y;
                float inv=inversesqrt(max(max(dot(t,t),dot(b,b)),0.00001));
                vec3 detail=texture2D(uNormalMap,vUv).xyz*2.0-1.0;
                n=normalize(mat3(t*inv,b*inv,n)*detail);
            }
        """.trimIndent() else ""
        val fs=shader(GL_FRAGMENT_SHADER,derivatives+"""
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            varying vec3 vNormal; varying vec3 vPosition; varying float vGroup; varying vec2 vUv;
            uniform float uSelected; uniform float uIsolate; uniform float uTextured;
            uniform float uDepthPass; uniform float uContactShadows; uniform vec2 uViewport; uniform sampler2D uDepthMap;
            uniform mat4 uView; uniform sampler2D uColor; uniform sampler2D uOcclusion; uniform sampler2D uNormalMap;
            FOSSIL_MATERIAL_SHADER
            vec3 packDepth(float z){vec3 c=fract(z*vec3(1.0,255.0,65025.0));c.xy-=c.yz/255.0;return c;}
            float occlusionAt(vec2 uv,float z){
                vec3 c=texture2D(uDepthMap,uv).rgb;float sampleZ=dot(c,vec3(1.0,1.0/255.0,1.0/65025.0))*180.0;
                float d=z-sampleZ;return smoothstep(0.018,0.07,d)*(1.0-smoothstep(0.12,0.65,d));
            }
            void main(){
                if(uIsolate>0.5 && uSelected>0.5 && abs(vGroup-uSelected)>0.2 && vGroup>0.0) discard;
                if(uDepthPass>0.5){gl_FragColor=vec4(packDepth(-(uView*vec4(vPosition,1.0)).z/180.0),1.0);return;}
                if(vGroup<0.0){gl_FragColor=vec4(0.23,0.17,0.12,1.0);return;}
                vec3 n=normalize(vNormal);
                vec3 color;float ao=1.0;float roughness=.5;
                if(uTextured>0.5){color=texture2D(uColor,vUv).rgb;ao=texture2D(uOcclusion,vUv).r;}
                else {
                    FossilSample fossil=fossilSurface(vPosition);
                    color=fossil.color;ao=fossil.occlusion;roughness=fossil.roughness;
                    n=fossilNormal(n,vPosition,fossil.relief);
                }
                if(uContactShadows>0.5){
                    vec2 uv=gl_FragCoord.xy/uViewport;
                    float z=-(uView*vec4(vPosition,1.0)).z;
                    vec2 r=vec2(13.0)/uViewport;
                    float shadow=occlusionAt(uv+r*vec2(1.0,0.0),z)+occlusionAt(uv+r*vec2(-1.0,0.0),z)
                        +occlusionAt(uv+r*vec2(0.0,1.0),z)+occlusionAt(uv+r*vec2(0.0,-1.0),z)
                        +occlusionAt(uv+r*vec2(0.5,0.6),z)+occlusionAt(uv+r*vec2(-0.5,0.6),z)
                        +occlusionAt(uv+r*vec2(0.5,-0.6),z)+occlusionAt(uv+r*vec2(-0.5,-0.6),z);
                    ao*=1.0-shadow*0.075;
                }
                BUMP_CODE
                if(uSelected>0.5){if(abs(vGroup-uSelected)<0.2)color=mix(color,vec3(1.0,0.64,0.24),0.6);else color*=0.37;}
                vec3 nv=normalize(mat3(uView)*n);vec3 eye=normalize(-(uView*vec4(vPosition,1.0)).xyz);
                vec3 key=normalize(vec3(-0.45,0.75,0.8)),fill=normalize(vec3(0.7,0.12,0.5));
                float diffuse=0.32+0.65*max(dot(nv,key),0.0)+0.20*max(dot(nv,fill),0.0);
                float spec=(uTextured>.5?.045:mix(.024,.003,roughness))*pow(max(dot(nv,normalize(key+eye)),0.0),uTextured>.5?18.0:7.0);
                float rim=(uTextured>.5?.07:.025)*pow(1.0-max(dot(nv,eye),0.0),3.0);
                vec3 linear=pow(color,vec3(2.2))*(diffuse*(0.30+0.70*ao))+vec3(spec+rim)*ao;
                gl_FragColor=vec4(pow(max(linear,vec3(0.0)),vec3(1.0/2.2)),1.0);
            }
        """.trimIndent().replace("BUMP_CODE",bump).replace("FOSSIL_MATERIAL_SHADER",FossilSurface.GLSL))
        program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(program)};glDeleteShader(vs);glDeleteShader(fs)
        fossilUniforms=FossilUniforms(program)
        uvLoc=glGetAttribLocation(program,"aUv");posLoc=glGetAttribLocation(program,"aPosition");normalLoc=glGetAttribLocation(program,"aNormal");groupLoc=glGetAttribLocation(program,"aGroup")
        matrixLoc=glGetUniformLocation(program,"uMatrix");selectLoc=glGetUniformLocation(program,"uSelected");isolateLoc=glGetUniformLocation(program,"uIsolate")
        glGenTextures(3,boneTextures,0);surfaceTexturesReady=false
        val buffers=IntArray(2);glGenBuffers(2,buffers,0);buffer=buffers[0];floor=buffers[1]
        glEnable(GL_DEPTH_TEST);glClearColor(baseRed,baseGreen,baseBlue,1f)
        val bvs=shader(GL_VERTEX_SHADER,"attribute vec2 aPoint; varying vec2 vUv; void main(){gl_Position=vec4(aPoint,0.0,1.0);vUv=vec2((aPoint.x+1.0)*0.5,(1.0-aPoint.y)*0.5);}")
        val bfs=shader(GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 vUv; uniform sampler2D uImage; uniform vec4 uBounds; uniform vec4 uCrop; uniform float uStrength; uniform vec3 uBase; void main(){vec2 uv=uCrop.xy+(uBounds.xy+vUv*uBounds.zw)*uCrop.zw;vec4 art=texture2D(uImage,uv);if(uv.x<0.0||uv.x>1.0||uv.y<0.0||uv.y>1.0)art.a=0.0;gl_FragColor=vec4(mix(uBase,art.rgb,uStrength*art.a),1.0);}")
        backgroundProgram=glCreateProgram();glAttachShader(backgroundProgram,bvs);glAttachShader(backgroundProgram,bfs);glLinkProgram(backgroundProgram)
        glGetProgramiv(backgroundProgram,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(backgroundProgram)};glDeleteShader(bvs);glDeleteShader(bfs)
        val textures=IntArray(1);glGenTextures(1,textures,0);texture=textures[0];uploadedArtwork=null;backgroundReady=false
        // Names from the previous EGL context cannot be reused after recreation.
        shadeBuffer=0;shadeTexture=0;shadeDepth=0;shadeWidth=0;shadeHeight=0;contactShadowsReady=false
        mesh?.let{upload(it)}
    }
    private fun drawBackground(){
        val bitmap=artwork?:run{backgroundReady=false;return}
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture)
        if(uploadedArtwork!==bitmap){
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0);uploadedArtwork=bitmap;backgroundReady=true
        }
        val bounds=backgroundBounds;val imageAspect=bitmap.width.toFloat()/bitmap.height;val windowAspect=bounds[4]
        val cropW=if(backgroundFill)min(1f,windowAspect/imageAspect) else max(1f,windowAspect/imageAspect);val cropH=if(backgroundFill)min(1f,imageAspect/windowAspect) else max(1f,imageAspect/windowAspect)
        glDisable(GL_DEPTH_TEST);glUseProgram(backgroundProgram);glBindBuffer(GL_ARRAY_BUFFER,0)
        val loc=glGetAttribLocation(backgroundProgram,"aPoint");glEnableVertexAttribArray(loc);quad.position(0);glVertexAttribPointer(loc,2,GL_FLOAT,false,0,quad)
        glUniform3f(glGetUniformLocation(backgroundProgram,"uBase"),baseRed,baseGreen,baseBlue)
        glUniform1i(glGetUniformLocation(backgroundProgram,"uImage"),0);glUniform1f(glGetUniformLocation(backgroundProgram,"uStrength"),artworkStrength)
        glUniform4f(glGetUniformLocation(backgroundProgram,"uBounds"),bounds[0],bounds[1],bounds[2],bounds[3])
        glUniform4f(glGetUniformLocation(backgroundProgram,"uCrop"),(1f-cropW)/2f,(1f-cropH)/2f,cropW,cropH)
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(loc);glEnable(GL_DEPTH_TEST)
    }
    private fun upload(mesh:Skeleton){
        renderedVertexCount=0;firstMeshFrame=true;framing=CameraFraming(mesh.vertices)
        textured=mesh.texturePrefix!=null;surfaceTexturesReady=false
        if(textured){
            val maxSize=IntArray(1);glGetIntegerv(GL_MAX_TEXTURE_SIZE,maxSize,0)
            val available=context.assets.list("specimens")?.toSet().orEmpty()
            listOf("color","occlusion","normal").forEachIndexed{i,name->
                val base="${mesh.texturePrefix}-$name"
                val asset=base+if(available.contains(base.substringAfterLast('/')+".png"))".png" else ".jpg"
                val bitmap=try {
                    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    context.assets.open(asset).use{BitmapFactory.decodeStream(it,null,bounds)}
                    val limit=min(2048,maxSize[0]);var sample=1
                    while(max(bounds.outWidth,bounds.outHeight)/sample>limit)sample*=2
                    val opts=BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
                    context.assets.open(asset).use{BitmapFactory.decodeStream(it,null,opts)}?:error("Invalid specimen texture")
                }catch(e:java.io.FileNotFoundException){
                    check(i>0){"Missing specimen color texture"}
                    // A color-only scan has neutral AO and a flat normal map, not invented scan detail.
                    Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(if(i==1)0xFFFFFFFF.toInt() else 0xFF8080FF.toInt())}
                }
                glActiveTexture(GL_TEXTURE1+i);glBindTexture(GL_TEXTURE_2D,boneTextures[i])
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0);glGenerateMipmap(GL_TEXTURE_2D);bitmap.recycle()
            }
            surfaceTexturesReady=true;glActiveTexture(GL_TEXTURE0)
        }

        glBindBuffer(GL_ARRAY_BUFFER,buffer);glBufferData(GL_ARRAY_BUFFER,mesh.vertices.size*4,null,GL_STATIC_DRAW)
        // Reuse a small staging buffer instead of duplicating a 38 MB scan on the native heap.
        val staging=ByteBuffer.allocateDirect(256*1024).order(ByteOrder.nativeOrder()).asFloatBuffer()
        var offset=0
        while(offset<mesh.vertices.size){val size=min(staging.capacity(),mesh.vertices.size-offset)
            staging.clear();staging.put(mesh.vertices,offset,size);staging.flip()
            glBufferSubData(GL_ARRAY_BUFFER,offset*4,size*4,staging);offset+=size
        }
        var lowest=Float.MAX_VALUE;for(i in 1 until mesh.vertices.size step 9)lowest=min(lowest,mesh.vertices[i])
        val ground=lowest-.17f
        val coords=ArrayList<Float>()
        fun point(x:Float,z:Float){coords.addAll(listOf(x,ground,z,0f,1f,0f,-1f,0f,0f))}
        for(r in listOf(2f,3f,4f,4.4f))for(i in 0 until 120){val t=i*PI.toFloat()/60;val u=(i+1)*PI.toFloat()/60;point(cos(t)*r,sin(t)*r);point(cos(u)*r,sin(u)*r)}
        val spacing=mesh.metresPerUnit?.let{gridMetres(it)/it}?:1f
        val gridCount=(4f/spacing).toInt();for(i in -gridCount..gridCount){val at=i*spacing;point(at,-4f);point(at,4f);point(-4f,at);point(4f,at)}
        val fb=ByteBuffer.allocateDirect(coords.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();fb.put(coords.toFloatArray()).position(0);glBindBuffer(GL_ARRAY_BUFFER,floor);glBufferData(GL_ARRAY_BUFFER,coords.size*4,fb,GL_STATIC_DRAW);floorCount=coords.size/9
    }
    private fun prepareContactShadows():Boolean {
        val w=min(width,720);val h=(height.toLong()*w/width).toInt().coerceAtLeast(1)
        if(shadeBuffer!=0&&shadeWidth==w&&shadeHeight==h)return contactShadowsReady
        if(shadeBuffer!=0){glDeleteFramebuffers(1,intArrayOf(shadeBuffer),0);glDeleteTextures(1,intArrayOf(shadeTexture),0);glDeleteRenderbuffers(1,intArrayOf(shadeDepth),0)}
        val id=IntArray(1);glGenFramebuffers(1,id,0);shadeBuffer=id[0];glGenTextures(1,id,0);shadeTexture=id[0];glGenRenderbuffers(1,id,0);shadeDepth=id[0]
        glActiveTexture(GL_TEXTURE4);glBindTexture(GL_TEXTURE_2D,shadeTexture)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,null)
        glBindRenderbuffer(GL_RENDERBUFFER,shadeDepth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT16,w,h)
        glBindFramebuffer(GL_FRAMEBUFFER,shadeBuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,shadeTexture,0)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,shadeDepth)
        contactShadowsReady=glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE
        glBindFramebuffer(GL_FRAMEBUFFER,0);glBindTexture(GL_TEXTURE_2D,0);glActiveTexture(GL_TEXTURE0)
        shadeWidth=w;shadeHeight=h;return contactShadowsReady
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){width=w.coerceAtLeast(1);height=h.coerceAtLeast(1);viewportWidth=width;viewportHeight=height;glViewport(0,0,width,height)}
    private fun draw(vbo:Int,count:Int,mode:Int){glBindBuffer(GL_ARRAY_BUFFER,vbo);glEnableVertexAttribArray(posLoc);glEnableVertexAttribArray(normalLoc);glEnableVertexAttribArray(groupLoc);glEnableVertexAttribArray(uvLoc);glVertexAttribPointer(uvLoc,2,GL_FLOAT,false,36,28);glVertexAttribPointer(posLoc,3,GL_FLOAT,false,36,0);glVertexAttribPointer(normalLoc,3,GL_FLOAT,false,36,12);glVertexAttribPointer(groupLoc,1,GL_FLOAT,false,36,24);glDrawArrays(mode,0,count)}
    override fun onDrawFrame(gl:GL10?){
        if(program==0)return
        pendingMesh?.let{pendingMesh=null;mesh=it;upload(it)}
        val now=System.nanoTime();val dt=if(lastFrame==0L)0f else ((now-lastFrame)/1e9f).coerceAtMost(.06f);lastFrame=now
        if(auto)pose=pose.orbit(dt*9f,0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);drawBackground();val mesh=mesh?:return
        val c=pose;val aspect=width.toFloat()/height;val fit=checkNotNull(framing);val distance=fit.distance(aspect,c.yaw,c.pitch)/c.zoom;fittedDistance=distance
        val yaw=Math.toRadians(c.yaw.toDouble()).toFloat();val pitch=Math.toRadians(c.pitch.toDouble()).toFloat()
        Matrix.perspectiveM(projection,0,38f,aspect,.06f,180f)
        // Translation in the camera plane makes two-finger movement independent of orbit angle.
        val right=V(cos(yaw),0f,-sin(yaw));val up=V(-sin(yaw)*sin(pitch),cos(pitch),-cos(yaw)*sin(pitch));val target=fit.center-(right*c.panX+up*c.panY)
        Matrix.setLookAtM(view,0,target.x+sin(yaw)*cos(pitch)*distance,target.y+sin(pitch)*distance,target.z+cos(yaw)*cos(pitch)*distance,target.x,target.y,target.z,0f,1f,0f)
        Matrix.multiplyMM(mvp,0,projection,0,view,0);glUseProgram(program);glUniformMatrix4fv(matrixLoc,1,false,mvp,0);glUniform1f(selectLoc,selected.toFloat());glUniform1f(isolateLoc,if(isolate)1f else 0f)
        glUniformMatrix4fv(glGetUniformLocation(program,"uView"),1,false,view,0)
        glUniform1f(glGetUniformLocation(program,"uTextured"),if(textured)1f else 0f)
        val fossil=if(textured)null else mesh.fossilMaterial?:FossilMaterials.forModel("reconstruction")
        fossilUniforms?.bind(fossil);renderedMaterialId=fossil?.id
        listOf("uColor","uOcclusion","uNormalMap").forEachIndexed{i,name->glActiveTexture(GL_TEXTURE1+i);glBindTexture(GL_TEXTURE_2D,boneTextures[i]);glUniform1i(glGetUniformLocation(program,name),i+1)}
        val contactShadows=!textured&&prepareContactShadows()
        glUniform1f(glGetUniformLocation(program,"uContactShadows"),0f)
        glUniform1i(glGetUniformLocation(program,"uDepthMap"),4)
        if(contactShadows){
            glActiveTexture(GL_TEXTURE4);glBindTexture(GL_TEXTURE_2D,0)
            glBindFramebuffer(GL_FRAMEBUFFER,shadeBuffer);glViewport(0,0,shadeWidth,shadeHeight)
            glClearColor(1f,1f,1f,1f);glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glUniform1f(glGetUniformLocation(program,"uDepthPass"),1f);draw(buffer,mesh.vertexCount,GL_TRIANGLES)
            glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(0,0,width,height);glClearColor(baseRed,baseGreen,baseBlue,1f)
            glActiveTexture(GL_TEXTURE4);glBindTexture(GL_TEXTURE_2D,shadeTexture)
        }
        glUniform1f(glGetUniformLocation(program,"uDepthPass"),0f)
        glUniform1f(glGetUniformLocation(program,"uContactShadows"),if(contactShadows)1f else 0f)
        glUniform2f(glGetUniformLocation(program,"uViewport"),width.toFloat(),height.toFloat())
        draw(floor,floorCount,GL_LINES);draw(buffer,mesh.vertexCount,GL_TRIANGLES)
        glDisableVertexAttribArray(posLoc);glDisableVertexAttribArray(normalLoc);glDisableVertexAttribArray(groupLoc);glDisableVertexAttribArray(uvLoc)
        // The first textured frame can compile GPU work asynchronously. Publish readiness only after it completes.
        if(firstMeshFrame){glFinish();firstMeshFrame=false}
        renderedVertexCount=mesh.vertexCount
        frameCount++
    }
    fun disposeGpuWithContext(){
        if(program==0){gpuResourcesReleased=true;return}
        val egl=EGLContext.getEGL() as EGL10
        val display=eglDisplay?:return;val context=eglContext?:return;val config=eglConfig?:return
        if(egl.eglGetCurrentContext()==context){disposeGpu();return}
        val surface=egl.eglCreatePbufferSurface(display,config,intArrayOf(EGL10.EGL_WIDTH,1,EGL10.EGL_HEIGHT,1,EGL10.EGL_NONE))
        if(surface==null||surface==EGL10.EGL_NO_SURFACE)return
        try{
            if(egl.eglMakeCurrent(display,surface,surface,context)){
                disposeGpu();glFinish()
                egl.eglMakeCurrent(display,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_SURFACE,EGL10.EGL_NO_CONTEXT)
            }
        }finally{egl.eglDestroySurface(display,surface)}
    }
    private fun disposeGpu(){
        glUseProgram(0);glBindBuffer(GL_ARRAY_BUFFER,0);glBindFramebuffer(GL_FRAMEBUFFER,0);glBindRenderbuffer(GL_RENDERBUFFER,0)
        for(unit in 0..4){glActiveTexture(GL_TEXTURE0+unit);glBindTexture(GL_TEXTURE_2D,0)}
        glDeleteBuffers(2,intArrayOf(buffer,floor),0)
        glDeleteTextures(5,intArrayOf(texture,shadeTexture,*boneTextures),0)
        glDeleteFramebuffers(1,intArrayOf(shadeBuffer),0);glDeleteRenderbuffers(1,intArrayOf(shadeDepth),0)
        glDeleteProgram(program);glDeleteProgram(backgroundProgram)
        program=0;backgroundProgram=0;buffer=0;floor=0;texture=0;shadeBuffer=0;shadeTexture=0;shadeDepth=0;boneTextures.fill(0)
        fossilUniforms=null;uploadedArtwork=null;backgroundReady=false;contactShadowsReady=false;surfaceTexturesReady=false;renderedVertexCount=0;gpuResourcesReleased=true
    }
}
