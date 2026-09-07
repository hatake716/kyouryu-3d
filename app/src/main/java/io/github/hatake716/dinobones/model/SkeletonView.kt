package io.github.hatake716.dinobones.model

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLUtils
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

data class CameraPose(val yaw: Float = 18f, val pitch: Float = 9f, val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    fun orbit(dx: Float,dy: Float) = copy(yaw=(yaw+dx)%360f,pitch=(pitch+dy).coerceIn(-89f,89f))
    fun scale(factor: Float) = copy(zoom=(zoom*factor).coerceIn(.55f,5f))
    fun pan(dx: Float,dy: Float) = copy(panX=(panX+dx).coerceIn(-6f,6f),panY=(panY+dy).coerceIn(-6f,6f))
}
class SkeletonView(context: Context): GLSurfaceView(context) {
    val museumRenderer = MuseumRenderer()
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
    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        val count=e.pointerCount;val x=(0 until count).sumOf {e.getX(it).toDouble()}.toFloat()/count;val y=(0 until count).sumOf {e.getY(it).toDouble()}.toFloat()/count
        val span=if(count>=2)hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0)) else 0f
        if(e.actionMasked==MotionEvent.ACTION_MOVE&&count==pointers){
            val dx=x-lastX;val dy=y-lastY
            museumRenderer.pose=if(count==1)museumRenderer.pose.orbit(dx*.32f,dy*.27f) else museumRenderer.pose.scale(if(lastSpan>1f)span/lastSpan else 1f).pan(dx/width*8f/museumRenderer.pose.zoom,-dy/height*8f/museumRenderer.pose.zoom)
            requestRender()
        }
        if(e.actionMasked==MotionEvent.ACTION_UP){performClick();parent?.requestDisallowInterceptTouchEvent(false)}
        if(e.actionMasked==MotionEvent.ACTION_CANCEL)parent?.requestDisallowInterceptTouchEvent(false)
        pointers=if(e.actionMasked==MotionEvent.ACTION_POINTER_UP)0 else count;lastX=x;lastY=y;lastSpan=span;return true
    }
}
class MuseumRenderer:GLSurfaceView.Renderer {
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
    private var mesh:Skeleton?=null
    private var program=0;private var buffer=0;private var floor=0;private var floorCount=0;private var width=1;private var height=1;private var lastFrame=0L
    private val projection=FloatArray(16);private val view=FloatArray(16);private val mvp=FloatArray(16)
    private var posLoc=0;private var normalLoc=0;private var groupLoc=0;private var matrixLoc=0;private var selectLoc=0;private var isolateLoc=0
    private fun shader(type: Int,src: String): Int {val s=glCreateShader(type);glShaderSource(s,src);glCompileShader(s);val ok=IntArray(1);glGetShaderiv(s,GL_COMPILE_STATUS,ok,0);check(ok[0]!=0){glGetShaderInfoLog(s)};return s}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        val vs=shader(GL_VERTEX_SHADER,"""
            uniform mat4 uMatrix;
            attribute vec3 aPosition; attribute vec3 aNormal; attribute float aGroup;
            varying vec3 vNormal; varying float vGroup;
            void main(){gl_Position=uMatrix*vec4(aPosition,1.0);vNormal=aNormal;vGroup=aGroup;}
        """.trimIndent())
        val fs=shader(GL_FRAGMENT_SHADER,"""
            precision mediump float;
            varying vec3 vNormal; varying float vGroup;
            uniform float uSelected; uniform float uIsolate;
            void main(){
                if(uIsolate>0.5 && uSelected>0.5 && abs(vGroup-uSelected)>0.2 && vGroup>0.0) discard;
                vec3 n=normalize(vNormal);
                float light=0.62+0.36*max(dot(n,normalize(vec3(0.1,1.0,0.8))),0.0)+0.16*abs(dot(n,normalize(vec3(-0.8,0.3,-0.5))));
                vec3 color=vec3(0.83,0.77,0.63);
                if(uSelected>0.5){if(abs(vGroup-uSelected)<0.2)color=vec3(0.66,0.89,0.55);else color=vec3(0.37,0.40,0.35);}
                if(vGroup<0.0)color=vec3(0.14,0.20,0.17);
                gl_FragColor=vec4(color*light,1.0);
            }
        """.trimIndent())
        program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
        val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(program)};glDeleteShader(vs);glDeleteShader(fs)
        posLoc=glGetAttribLocation(program,"aPosition");normalLoc=glGetAttribLocation(program,"aNormal");groupLoc=glGetAttribLocation(program,"aGroup")
        matrixLoc=glGetUniformLocation(program,"uMatrix");selectLoc=glGetUniformLocation(program,"uSelected");isolateLoc=glGetUniformLocation(program,"uIsolate")
        val buffers=IntArray(2);glGenBuffers(2,buffers,0);buffer=buffers[0];floor=buffers[1]
        glEnable(GL_DEPTH_TEST);glClearColor(16/255f,24/255f,21/255f,1f)
        val bvs=shader(GL_VERTEX_SHADER,"attribute vec2 aPoint; varying vec2 vUv; void main(){gl_Position=vec4(aPoint,0.0,1.0);vUv=vec2((aPoint.x+1.0)*0.5,(1.0-aPoint.y)*0.5);}")
        val bfs=shader(GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 vUv; uniform sampler2D uImage; uniform vec4 uBounds; uniform vec4 uCrop; uniform float uStrength; void main(){vec2 uv=uCrop.xy+(uBounds.xy+vUv*uBounds.zw)*uCrop.zw;vec4 art=texture2D(uImage,uv);if(uv.x<0.0||uv.x>1.0||uv.y<0.0||uv.y>1.0)art.a=0.0;gl_FragColor=vec4(mix(vec3(16.0,24.0,21.0)/255.0,art.rgb,uStrength*art.a),1.0);}")
        backgroundProgram=glCreateProgram();glAttachShader(backgroundProgram,bvs);glAttachShader(backgroundProgram,bfs);glLinkProgram(backgroundProgram)
        glGetProgramiv(backgroundProgram,GL_LINK_STATUS,linked,0);check(linked[0]!=0){glGetProgramInfoLog(backgroundProgram)};glDeleteShader(bvs);glDeleteShader(bfs)
        val textures=IntArray(1);glGenTextures(1,textures,0);texture=textures[0];uploadedArtwork=null;backgroundReady=false
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
        glUniform1i(glGetUniformLocation(backgroundProgram,"uImage"),0);glUniform1f(glGetUniformLocation(backgroundProgram,"uStrength"),artworkStrength)
        glUniform4f(glGetUniformLocation(backgroundProgram,"uBounds"),bounds[0],bounds[1],bounds[2],bounds[3])
        glUniform4f(glGetUniformLocation(backgroundProgram,"uCrop"),(1f-cropW)/2f,(1f-cropH)/2f,cropW,cropH)
        glDrawArrays(GL_TRIANGLE_STRIP,0,4);glDisableVertexAttribArray(loc);glEnable(GL_DEPTH_TEST)
    }
    private fun upload(mesh:Skeleton){
        glBindBuffer(GL_ARRAY_BUFFER,buffer);val data=ByteBuffer.allocateDirect(mesh.vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(mesh.vertices).position(0);glBufferData(GL_ARRAY_BUFFER,mesh.vertices.size*4,data,GL_STATIC_DRAW)
        val ground=mesh.vertices.filterIndexed{i,_->i%7==1}.min()-.17f
        val coords=ArrayList<Float>()
        fun point(x:Float,z:Float){coords.addAll(listOf(x,ground,z,0f,1f,0f,-1f))}
        for(r in listOf(2f,3f,4f,4.4f))for(i in 0 until 120){val t=i*PI.toFloat()/60;val u=(i+1)*PI.toFloat()/60;point(cos(t)*r,sin(t)*r);point(cos(u)*r,sin(u)*r)}
        for(i in -4..4){point(i.toFloat(),-4f);point(i.toFloat(),4f);point(-4f,i.toFloat());point(4f,i.toFloat())}
        val fb=ByteBuffer.allocateDirect(coords.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();fb.put(coords.toFloatArray()).position(0);glBindBuffer(GL_ARRAY_BUFFER,floor);glBufferData(GL_ARRAY_BUFFER,coords.size*4,fb,GL_STATIC_DRAW);floorCount=coords.size/7
        renderedVertexCount=mesh.vertexCount
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){width=w.coerceAtLeast(1);height=h.coerceAtLeast(1);glViewport(0,0,width,height)}
    private fun draw(vbo:Int,count:Int,mode:Int){glBindBuffer(GL_ARRAY_BUFFER,vbo);glEnableVertexAttribArray(posLoc);glEnableVertexAttribArray(normalLoc);glEnableVertexAttribArray(groupLoc);glVertexAttribPointer(posLoc,3,GL_FLOAT,false,28,0);glVertexAttribPointer(normalLoc,3,GL_FLOAT,false,28,12);glVertexAttribPointer(groupLoc,1,GL_FLOAT,false,28,24);glDrawArrays(mode,0,count)}
    override fun onDrawFrame(gl:GL10?){
        pendingMesh?.let{pendingMesh=null;mesh=it;upload(it)}
        val now=System.nanoTime();val dt=if(lastFrame==0L)0f else ((now-lastFrame)/1e9f).coerceAtMost(.06f);lastFrame=now
        if(auto)pose=pose.orbit(dt*9f,0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);drawBackground();val mesh=mesh?:return
        val c=pose;val aspect=width.toFloat()/height;val distance=mesh.radius/(sin(Math.toRadians(19.0)).toFloat())*max(1f,1f/aspect)*1.06f/c.zoom
        val yaw=Math.toRadians(c.yaw.toDouble()).toFloat();val pitch=Math.toRadians(c.pitch.toDouble()).toFloat()
        Matrix.perspectiveM(projection,0,38f,aspect,.06f,180f)
        // Translation in the camera plane makes two-finger movement independent of orbit angle.
        val right=V(cos(yaw),0f,-sin(yaw));val up=V(-sin(yaw)*sin(pitch),cos(pitch),-cos(yaw)*sin(pitch));val target=(right*c.panX+up*c.panY)*-1f
        Matrix.setLookAtM(view,0,target.x+sin(yaw)*cos(pitch)*distance,target.y+sin(pitch)*distance,target.z+cos(yaw)*cos(pitch)*distance,target.x,target.y,target.z,0f,1f,0f)
        Matrix.multiplyMM(mvp,0,projection,0,view,0);glUseProgram(program);glUniformMatrix4fv(matrixLoc,1,false,mvp,0);glUniform1f(selectLoc,selected.toFloat());glUniform1f(isolateLoc,if(isolate)1f else 0f)
        draw(floor,floorCount,GL_LINES);draw(buffer,mesh.vertexCount,GL_TRIANGLES);frameCount++
    }
}
