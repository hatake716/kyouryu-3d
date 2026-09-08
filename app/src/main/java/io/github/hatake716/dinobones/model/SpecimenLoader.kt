package io.github.hatake716.dinobones.model

import java.io.InputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/** KSM1 is generated offline from attributed models. Decode into one vertex array. */
object SpecimenLoader {
    val ids get()=DetailedModels.entries.keys
    fun load(id:String="triceratops",open:(String)->InputStream):Skeleton {
        require(id in ids)
        return open("specimens/$id.ksmz").use{raw->DataInputStream(GZIPInputStream(raw,64*1024)).use{stream->
            val header=ByteArray(16);stream.readFully(header)
            val b=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            require(b.int==0x314d534b){"Invalid specimen mesh"}
            val count=b.int;val metres=b.float;val radius=b.float
            require(count in 3..1200000&&count%3==0)
            // Zero is an uncalibrated source, never a physical scale claim.
            require(metres>=0f&&metres.isFinite()&&radius in 3.9f..6f)
            val vertices=FloatArray(count*9)
            val chunk=ByteArray(64*1024);val floats=ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            var offset=0
            while(offset<vertices.size){
                val size=minOf(chunk.size/4,vertices.size-offset);stream.readFully(chunk,0,size*4)
                floats.position(0);floats.get(vertices,offset,size);offset+=size
            }
            require(stream.read()==-1){"Unexpected trailing mesh data"}
            require(vertices.all{it.isFinite()})
            Skeleton(vertices,emptyList(),emptyList(),radius,metres.takeIf{it>0f},if(DetailedModels.entries.getValue(id).textured)"specimens/$id" else null,FossilMaterials.forModel(id))
        }}
    }
}
