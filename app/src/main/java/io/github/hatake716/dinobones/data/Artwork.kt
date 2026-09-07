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
    private val previews=object:LruCache<String,Bitmap>(8*1024*1024) {
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    }
    private val cache=object:LruCache<String,Bitmap>(24*1024*1024) {
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    }
    @Synchronized fun load(context:Context,art:Artwork):Bitmap = cache.get(art.asset) ?: context.assets.open(art.asset).use {
        requireNotNull(BitmapFactory.decodeStream(it)).also { bitmap -> cache.put(art.asset,bitmap) }
    }
    @Synchronized fun preview(context:Context,art:Artwork):Bitmap {
        previews.get(art.asset)?.let{return it}
        val options=BitmapFactory.Options().apply{inJustDecodeBounds=true;inSampleSize=1}
        context.assets.open(art.asset).use{BitmapFactory.decodeStream(it,null,options)}
        options.inJustDecodeBounds=false
        while(options.outWidth/options.inSampleSize>720 || options.outHeight/options.inSampleSize>720)options.inSampleSize*=2
        return context.assets.open(art.asset).use{requireNotNull(BitmapFactory.decodeStream(it,null,options))}.also{previews.put(art.asset,it)}
    }
}
