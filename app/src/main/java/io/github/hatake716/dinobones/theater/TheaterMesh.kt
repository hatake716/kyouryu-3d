package io.github.hatake716.dinobones.theater

import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

data class TheaterPart(val vertices:FloatArray,val indices:ShortArray)
data class TheaterMesh(val parts:List<TheaterPart>){
    val triangles get()=parts.sumOf{it.indices.size/3}
    val bytes get()=parts.sumOf{it.vertices.size*4+it.indices.size*2}
    companion object {
        fun read(input:InputStream):TheaterMesh=DataInputStream(GZIPInputStream(input,65536)).use{stream->
            fun int():Int=Integer.reverseBytes(stream.readInt())
            require(int()==0x31534d54){"Invalid theater mesh"};val count=int();require(count in 1..12)
            val parts=List(count){
                val vc=int();val ic=int();require(vc in 3..60000&&ic in 3..1800000&&ic%3==0)
                val vertices=FloatArray(vc*9);val bytes=ByteArray(65536);val floats=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();var offset=0
                while(offset<vertices.size){val n=minOf(floats.capacity(),vertices.size-offset);stream.readFully(bytes,0,n*4);floats.position(0);floats.get(vertices,offset,n);offset+=n}
                require(vertices.all{it.isFinite()})
                val indices=ShortArray(ic);val shorts=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();offset=0
                while(offset<ic){val n=minOf(shorts.capacity(),ic-offset);stream.readFully(bytes,0,n*2);shorts.position(0);shorts.get(indices,offset,n);offset+=n}
                require(indices.all{(it.toInt()and 65535)<vc});TheaterPart(vertices,indices)
            }
            require(stream.read()==-1);TheaterMesh(parts)
        }
    }
}
