package io.github.hatake716.dinobones.theater

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.*

/** Native pointer ownership lets the thumbstick and free look work at the same time. */
class TheaterView(context:Context):FrameLayout(context) {
    var onPick:((String)->Unit)?=null
    var onArtwork:((String)->Unit)?=null
    var onError:((String?)->Unit)?=null
    val renderer=TheaterRenderer(context,{action->post(action)},{id->onError?.invoke(id)})
    private val surface=object:GLSurfaceView(context){
        override fun onDetachedFromWindow(){
            destroyGraphics()
            super.onDetachedFromWindow()
        }
    }.apply {
        preserveEGLContextOnPause=true
        setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,0,24,0)
        setRenderer(renderer);renderMode=GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
    val controls=Controls(context)
    init {
        addView(surface,LayoutParams(-1,-1));addView(controls,LayoutParams(-1,-1))
        contentDescription="一人称の博物館。左下のスティックで移動、画面をドラッグして見回す。骨格をタップして詳細図鑑。隣の復元イラストの立て札をタップして画像を閲覧。"
        isFocusable=true
    }
    private var paused=false
    private var released=false
    fun resume(){if(!released&&paused){paused=false;surface.onResume()}}
    fun pause(){if(!paused&&!released){controls.cancel();surface.onPause();paused=true;renderer.renderedIds=emptySet()}}
    fun travel(spot:ExhibitSpot?){controls.cancel();renderer.renderedIds=emptySet();synchronized(renderer){renderer.pose=spot?.arrival(renderer.exhibitHeights[spot.id]?:3f)?:WalkPose()}}
    fun retry(){surface.queueEvent{renderer.retry()}}
    private var graphicsDestroyed=false
    private fun destroyGraphics(){
        if(graphicsDestroyed)return
        graphicsDestroyed=true;renderer.release()
        val done=java.util.concurrent.CountDownLatch(1)
        surface.queueEvent{try{renderer.disposeGpuWithContext()}finally{done.countDown()}}
        // GLSurfaceView checks its exit flag before queued work. Finish deletion
        // before onDetachedFromWindow requests thread exit, including while paused.
        done.await(1500,java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    fun release(){
        if(released)return
        controls.cancel();destroyGraphics();surface.preserveEGLContextOnPause=false
        surface.onPause();paused=true;released=true;onPick=null;onArtwork=null;onError=null
    }
    override fun dispatchTouchEvent(event:MotionEvent):Boolean =
        visibility==View.VISIBLE&&!released&&super.dispatchTouchEvent(event)

    inner class Controls(context:Context):View(context) {
        private val dp=resources.displayMetrics.density
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val stickX get()=84f*dp
        val stickY get()=height-104f*dp
        val radius get()=54f*dp
        private var joy=-1;private var look=-1
        private var lastX=0f;private var lastY=0f;private var downX=0f;private var downY=0f;private var moved=false;private var downAt=0L
        init {setWillNotDraw(false);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
        fun cancel(){joy=-1;look=-1;renderer.forward=0f;renderer.strafe=0f;invalidate()}
        override fun onTouchEvent(e:MotionEvent):Boolean {
            val action=e.actionMasked;val index=e.actionIndex
            when(action){
                MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{
                    val x=e.getX(index);val y=e.getY(index);val id=e.getPointerId(index)
                    if(joy<0&&hypot(x-stickX,y-stickY)<radius*1.5f){joy=id;stick(x,y)}
                    else if(look<0){look=id;lastX=x;lastY=y;downX=x;downY=y;downAt=e.eventTime;moved=false}
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE->{
                    e.findPointerIndex(joy).takeIf{it>=0}?.let{stick(e.getX(it),e.getY(it))}
                    e.findPointerIndex(look).takeIf{it>=0}?.let{
                        val x=e.getX(it);val y=e.getY(it)
                        if(hypot(x-downX,y-downY)>8f*dp)moved=true
                        if(moved)synchronized(renderer){renderer.pose=renderer.pose.look((x-lastX)/dp*.23f,(y-lastY)/dp*.20f)}
                        lastX=x;lastY=y
                    }
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP->{
                    val id=e.getPointerId(index)
                    if(id==joy){joy=-1;renderer.forward=0f;renderer.strafe=0f;invalidate()}
                    if(id==look){
                        if(!moved&&e.eventTime-downAt<500){
                            val hit=TheaterWorld.pickTarget(renderer.pose,e.getX(index)/width*2-1,1-e.getY(index)/height*2,width.toFloat()/height,renderer.exhibitHeights)
                            if(hit!=null){if(hit.artwork&&renderer.illustrationsReady)onArtwork?.invoke(hit.spot.id) else if(!hit.artwork&&hit.spot.id in renderer.renderedIds)onPick?.invoke(hit.spot.id)}
                            performClick()
                        };look=-1
                    }
                    if(action==MotionEvent.ACTION_UP)cancel()
                }
                MotionEvent.ACTION_CANCEL->cancel()
            }
            return true
        }
        override fun performClick():Boolean {super.performClick();return true}
        private fun stick(x:Float,y:Float){
            val dx=(x-stickX)/radius;val dy=(stickY-y)/radius;val length=max(1f,hypot(dx,dy))
            renderer.strafe=dx/length;renderer.forward=dy/length;invalidate()
        }
        override fun onDraw(canvas:Canvas){
            paint.style=Paint.Style.FILL;paint.color=Color.argb(155,33,25,19);canvas.drawCircle(stickX,stickY,radius,paint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f*dp;paint.color=Color.argb(165,216,179,119);canvas.drawCircle(stickX,stickY,radius,paint)
            val tx=stickX+renderer.strafe*radius*.65f;val ty=stickY-renderer.forward*radius*.65f
            paint.style=Paint.Style.FILL;paint.color=Color.argb(220,216,179,119);canvas.drawCircle(tx,ty,22f*dp,paint)
            paint.textAlign=Paint.Align.CENTER;paint.textSize=11f*dp;paint.color=Color.argb(230,235,217,187);canvas.drawText("移動",stickX,stickY+radius+22f*dp,paint)
            paint.color=Color.argb(205,245,229,200);canvas.drawCircle(width*.5f,height*.5f,2f*dp,paint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1f*dp;canvas.drawCircle(width*.5f,height*.5f,8f*dp,paint)
        }
    }
}
