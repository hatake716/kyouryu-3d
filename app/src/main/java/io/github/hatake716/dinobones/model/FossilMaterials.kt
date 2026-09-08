package io.github.hatake716.dinobones.model

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

/** Art direction for reconstructed surfaces; these are not sampled fossil measurements. */
data class FossilMaterial(
    val id:String,val finish:String,val dark:V,val light:V,val soil:V,val stain:V,
    val seed:V,val patternScale:Float,val fractures:Float,val sediment:Float,val roughness:Float,
){
    fun thumbnailColor(x:Float,y:Float,z:Float,lighting:Float):Int {
        val t=.48f+.15f*sin(x*3.1f+seed.x)+.12f*sin(y*7.3f+z*2.7f+seed.y)
        val c=dark*(1f-t)+light*t
        fun channel(v:Float)=(v*lighting*255).toInt().coerceIn(0,255)
        return (255 shl 24) or (channel(c.x) shl 16) or (channel(c.y) shl 8) or channel(c.z)
    }
}
object FossilMaterials {
    const val NOTE="化石の色むら・風化・ひび・付着土を種類ごとに変えた独自の表面表現です。色は生前の骨の色や産地固有の保存状態を測定したものではありません。"
    val sourceSurfaces=setOf("triceratops","tyrannosaurus","carnotaurus")
    private data class Palette(val name:String,val dark:Int,val light:Int,val soil:Int,val stain:Int)
    private val palettes=listOf(
        Palette("赤褐色の骨肌・赤土の染み",0x35231B,0xA57950,0xAF885A,0x86401F),
        Palette("暗褐色の骨肌・黄土の付着",0x242321,0x736653,0xBBA174,0x64503D),
        Palette("砂色の骨肌・褐色の鉱物斑",0x615348,0xC4B295,0x997348,0x795634),
        Palette("灰黒色の骨肌・明るい砂粒",0x262729,0x797774,0xB7AA8D,0x4B4038),
        Palette("栗褐色の骨肌・灰土の付着",0x3C2520,0x9C7866,0xACA08B,0x76381E),
        Palette("灰褐色の骨肌・粘土の残り",0x45413A,0xB2A68A,0x8F765B,0x655347),
        Palette("黒褐色の骨肌・淡い鉱物斑",0x29201B,0x8A7155,0xC0A47B,0x544B45),
        Palette("くすんだ黄褐色・濃い土の斑",0x4A3D29,0xB79F6C,0x73503B,0x86603E),
    )
    private val choices=mapOf(
        "spinosaurus" to 0,"velociraptor" to 1,"stegosaurus" to 4,"ankylosaurus" to 6,
        "brachiosaurus" to 2,"parasaurolophus" to 7,"giganotosaurus" to 5,"allosaurus" to 6,
        "pteranodon" to 2,"archaeopteryx" to 3,"mosasaurus" to 3,"patagotitan" to 4,
        "diplodocus" to 5,"apatosaurus" to 7,"camarasaurus" to 1,"iguanodon" to 0,
        "therizinosaurus" to 5,"deinonychus" to 4,"quetzalcoatlus" to 7,"saurophaganax" to 6,
    )
    private val cache=ConcurrentHashMap<String,FossilMaterial>()
    fun forModel(id:String):FossilMaterial? = if(id in sourceSurfaces)null else cache.getOrPut(id){
        require(id.isNotBlank())
        // Specimen appearance remains stable across catalog reordering and process restarts.
        var state=id.fold(0x811c9dc5.toInt()){h,c->(h xor c.code)*16777619}
        fun random():Float {state=state xor (state shl 13);state=state xor (state ushr 17);state=state xor (state shl 5);return (state ushr 8)/16777216f}
        val index=choices[id]?:((state ushr 1)%palettes.size)
        val p=palettes[index];val warm=(random()-.5f)*.06f;val value=.94f+random()*.12f
        fun rgb(n:Int)=V((((n shr 16)and 255)/255f*value+warm).coerceIn(.05f,.85f),(((n shr 8)and 255)/255f*value).coerceIn(.05f,.85f),((n and 255)/255f*value-warm*.4f).coerceIn(.05f,.85f))
        FossilMaterial(id,p.name,rgb(p.dark),rgb(p.light),rgb(p.soil),rgb(p.stain),
            V(random()*23f,random()*23f,random()*23f),.82f+random()*.52f,.45f+random()*.50f,.42f+random()*.42f,.78f+random()*.20f)
    }
}
