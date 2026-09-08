package io.github.hatake716.dinobones

import android.opengl.EGL14.*
import android.opengl.GLES20.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.hatake716.dinobones.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class FossilShaderTest {
    @Test fun allFinishesProduceDistinctStablePixelsWithAndWithoutSurfaceDerivatives(){
        // A small independent EGL surface tests the actual shader, including the ES 2.0 fallback.
        val display=eglGetDisplay(EGL_DEFAULT_DISPLAY);val version=IntArray(2)
        assertTrue(eglInitialize(display,version,0,version,1))
        val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
        assertTrue(eglChooseConfig(display,intArrayOf(EGL_RENDERABLE_TYPE,EGL_OPENGL_ES2_BIT,EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_NONE),0,configs,0,1,count,0))
        val context=eglCreateContext(display,configs[0],EGL_NO_CONTEXT,intArrayOf(EGL_CONTEXT_CLIENT_VERSION,2,EGL_NONE),0)
        val surface=eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL_WIDTH,48,EGL_HEIGHT,48,EGL_NONE),0)
        try{
            assertTrue(eglMakeCurrent(display,surface,surface,context))
            val vertices=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0)}
            val pixels=ByteBuffer.allocateDirect(48*48*4)
            fun compile(type:Int,source:String):Int{
                val shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader)
                val ok=IntArray(1);glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0);assertEquals(glGetShaderInfoLog(shader),1,ok[0]);return shader
            }
            for(derivatives in listOf(false,true)){
                if(derivatives)assertTrue(glGetString(GL_EXTENSIONS)!!.contains("GL_OES_standard_derivatives"))
                val vs=compile(GL_VERTEX_SHADER,"attribute vec2 p;varying vec3 point;void main(){point=vec3(p*2.0,.3);gl_Position=vec4(p,0,1);}")
                val header=if(derivatives)"#extension GL_OES_standard_derivatives : enable\n#define FOSSIL_DERIVATIVES\n" else ""
                val fs=compile(GL_FRAGMENT_SHADER,header+"precision highp float;varying vec3 point;"+FossilSurface.GLSL+"void main(){FossilSample f=fossilSurface(point);vec3 n=fossilNormal(vec3(0,0,1),point,f.relief);gl_FragColor=vec4(f.color*(.6+.4*max(dot(n,normalize(vec3(.3,.5,1))),0.0))*f.occlusion,1);}")
                val program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
                val ok=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,ok,0);assertEquals(glGetProgramInfoLog(program),1,ok[0]);glUseProgram(program)
                val p=glGetAttribLocation(program,"p");glEnableVertexAttribArray(p);glVertexAttribPointer(p,2,GL_FLOAT,false,0,vertices);glViewport(0,0,48,48)
                val uniforms=FossilUniforms(program)
                fun render(id:String):ByteArray{
                    uniforms.bind(FossilMaterials.forModel(id));glDrawArrays(GL_TRIANGLE_STRIP,0,4)
                    pixels.clear();glReadPixels(0,0,48,48,GL_RGBA,GL_UNSIGNED_BYTE,pixels);assertEquals(0,glGetError())
                    val bytes=ByteArray(pixels.capacity());pixels.rewind();pixels.get(bytes);return bytes
                }
                val outputs=SkeletonFactory.ids.filter{it !in FossilMaterials.sourceSurfaces}.associateWith{render(it)}
                assertEquals(97,outputs.values.map{java.security.MessageDigest.getInstance("SHA-256").digest(it).toList()}.toSet().size)
                for((id,bytes) in outputs){assertArrayEquals("Stable $id derivatives=$derivatives",bytes,render(id));assertTrue("Visible surface $id",bytes.filterIndexed{i,_->i%4!=3}.map{it.toInt() and 255}.average()>15)}
                glDisableVertexAttribArray(p);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs)
            }
        }finally{
            eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);eglDestroySurface(display,surface);eglDestroyContext(display,context);eglTerminate(display)
        }
    }
}
