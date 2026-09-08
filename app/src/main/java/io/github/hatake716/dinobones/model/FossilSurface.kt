package io.github.hatake716.dinobones.model

import android.opengl.GLES20.*

/** One shader is shared by both views, always sampled in the model's own coordinates. */
object FossilSurface {
    const val GLSL="""
        uniform float uFossilOn;
        uniform vec3 uFossilDark,uFossilLight,uFossilSoil,uFossilStain,uFossilSeed;
        uniform vec4 uFossilParams;
        float fossilHash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
        float fossilNoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
            return mix(mix(mix(fossilHash(i),fossilHash(i+vec3(1,0,0)),f.x),mix(fossilHash(i+vec3(0,1,0)),fossilHash(i+vec3(1,1,0)),f.x),f.y),
                mix(mix(fossilHash(i+vec3(0,0,1)),fossilHash(i+vec3(1,0,1)),f.x),mix(fossilHash(i+vec3(0,1,1)),fossilHash(i+vec3(1,1,1)),f.x),f.y),f.z);
        }
        struct FossilSample {vec3 color;float relief;float occlusion;float roughness;};
        FossilSample fossilSurface(vec3 position){
            vec3 p=position*uFossilParams.x+uFossilSeed;
            float fine=1.0,medium=1.0;
            #ifdef FOSSIL_DERIVATIVES
                // Fade subpixel pores and fissures instead of sparkling while walking or orbiting.
                float footprint=max(length(dFdx(p)),length(dFdy(p)));
                fine=1.0-smoothstep(.003,.022,footprint);
                medium=1.0-smoothstep(.014,.07,footprint);
            #endif
            float broad=fossilNoise(p*1.45);
            float patches=fossilNoise(p*4.8+vec3(broad*1.9,0.0,broad));
            float grain=mix(.5,fossilNoise(p*137.0),fine);
            float pores=fossilNoise(p*43.0);
            float pit=(1.0-smoothstep(.16,.29,pores))*fine;
            float fissure=abs(fossilNoise(p*6.7+patches*1.8)-.51);
            float crack=(1.0-smoothstep(.007,.029,fissure))*smoothstep(.40,.64,patches)*medium*uFossilParams.y;
            float silt=fossilNoise(p*12.5+vec3(0.0,broad*2.0,0.0));
            float earth=smoothstep(.47,.70,silt+pit*.20+crack*.25)*smoothstep(.30,.65,broad)*uFossilParams.z;
            vec3 c=mix(uFossilDark,uFossilLight,clamp(.18+broad*.64+patches*.15,0.0,1.0));
            c=mix(c,uFossilStain,smoothstep(.52,.77,patches)*.48);
            c*=.94+grain*.10;
            c=mix(c,uFossilDark*.48,crack*.7+pit*.22);
            c=mix(c,uFossilSoil,earth*.83);
            FossilSample result;
            result.color=c;
            result.relief=(grain-.5)*.0015-pit*.004-crack*.005+earth*.004;
            result.occlusion=1.0-pit*.22-crack*.16;
            result.roughness=clamp(uFossilParams.w+earth*.12+(grain-.5)*.07,.65,1.0);
            return result;
        }
        vec3 fossilNormal(vec3 n,vec3 position,float relief){
            #ifdef FOSSIL_DERIVATIVES
                vec3 sx=dFdx(position),sy=dFdy(position),rx=cross(sy,n),ry=cross(n,sx);
                float det=dot(sx,rx);
                if(abs(det)>.00000001)n=normalize(abs(det)*n-sign(det)*(dFdx(relief)*rx+dFdy(relief)*ry));
            #endif
            return n;
        }
    """
}
class FossilUniforms(program:Int){
    private val enabled=glGetUniformLocation(program,"uFossilOn")
    private val dark=glGetUniformLocation(program,"uFossilDark")
    private val light=glGetUniformLocation(program,"uFossilLight")
    private val soil=glGetUniformLocation(program,"uFossilSoil")
    private val stain=glGetUniformLocation(program,"uFossilStain")
    private val seed=glGetUniformLocation(program,"uFossilSeed")
    private val params=glGetUniformLocation(program,"uFossilParams")
    fun bind(material:FossilMaterial?){
        glUniform1f(enabled,if(material==null)0f else 1f)
        if(material==null)return
        fun put(location:Int,v:V)=glUniform3f(location,v.x,v.y,v.z)
        put(dark,material.dark);put(light,material.light);put(soil,material.soil);put(stain,material.stain);put(seed,material.seed)
        glUniform4f(params,material.patternScale,material.fractures,material.sediment,material.roughness)
    }
}
