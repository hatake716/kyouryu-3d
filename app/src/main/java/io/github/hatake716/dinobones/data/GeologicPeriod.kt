package io.github.hatake716.dinobones.data

import android.content.Context
import org.json.JSONArray

enum class GeologicPeriod(val label:String,val range:String,val mapAge:String) {
    TRIASSIC("三畳紀","約2億5190万〜2億140万年前","2億2500万年前"),
    JURASSIC("ジュラ紀","約2億140万〜1億4310万年前","1億5500万年前"),
    CRETACEOUS("白亜紀","約1億4310万〜6600万年前","1億年前");

    // Catalog labels retain taxonomic/stage interpretation when numerical estimates differ.
    // Genera spanning a boundary appear in both periods, not in an arbitrary single bucket.
    fun contains(dinosaur:Dinosaur)=dinosaur.period.contains(label)
}

object PaleoMaps {
    fun load(context:Context):Map<GeologicPeriod,Artwork> {
        val rows=JSONArray(context.assets.open("maps.json").bufferedReader().use{it.readText()})
        return (0 until rows.length()).associate { index ->
            val r=rows.getJSONObject(index)
            GeologicPeriod.valueOf(r.getString("id").uppercase(java.util.Locale.ROOT)) to
                Artwork(r.getString("asset"),r.getString("fileTitle"),r.getString("artist"),r.getString("source"),r.getString("license"),r.getString("licenseUrl"),r.getString("credit"))
        }
    }
}
