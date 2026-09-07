package io.github.hatake716.dinobones.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

data class Artwork(
    val asset: String, val title: String, val artist: String, val source: String,
    val license: String, val licenseUrl: String, val credit: String, val note:String="",
)

object ArtworkCache {
    private val cache=object:LruCache<String,Bitmap>(24*1024*1024) {
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    }
    @Synchronized fun load(context:Context,art:Artwork):Bitmap = cache.get(art.asset) ?: context.assets.open(art.asset).use {
        requireNotNull(BitmapFactory.decodeStream(it)).also { bitmap -> cache.put(art.asset,bitmap) }
    }
}
