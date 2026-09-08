package io.github.hatake716.dinobones.tools

import io.github.hatake716.dinobones.model.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

/** Explicit Gradle export task, not a unit test and never run by ordinary builds. */
object ExportTheater {
    @JvmStatic fun main(args:Array<String>){
        val out=File(args.single()).apply{mkdirs()}
        for(id in SkeletonFactory.ids.filterNot{it in DetailedModels.entries}){
            val m=SkeletonFactory.createTheater(id)
            GZIPOutputStream(File(out,"$id.ksmz").outputStream()).use{stream->
                stream.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putInt(0x314d534b).putInt(m.vertexCount).putFloat(m.metresPerUnit?:0f).putFloat(m.radius).array())
                val bytes=ByteArray(65536);val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();var offset=0
                while(offset<m.vertices.size){val n=minOf(buffer.capacity(),m.vertices.size-offset);buffer.clear();buffer.put(m.vertices,offset,n);stream.write(bytes,0,n*4);offset+=n}
            }
            println("$id ${m.vertexCount/3} triangles")
        }
    }
}
