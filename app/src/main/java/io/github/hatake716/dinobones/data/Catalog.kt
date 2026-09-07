package io.github.hatake716.dinobones.data

import android.content.Context
import org.json.JSONArray
import java.net.URLEncoder
import java.text.Normalizer

data class Source(val title: String, val url: String)
data class FactSection(val title: String, val text: String)
data class Dinosaur(
    val id: String, val name: String, val latin: String, val subtitle: String,
    val category: String, val period: String, val age: String, val startMa: Double,
    val endMa: Double, val region: String, val size: String, val diet: String,
    val summary: String, val sections: List<FactSection>, val sources: List<Source>,
    val modelNote: String, val wikiTitle: String, val videos: List<Source>,
    val artwork: Artwork? = null, val rank:Int=0, val pageviews:Long=0,
) {
    val wikipedia get() = "https://ja.wikipedia.org/wiki/" + URLEncoder.encode(wikiTitle, "UTF-8").replace("+", "%20")
    val youtube get() = "https://www.youtube.com/results?search_query=" + URLEncoder.encode("$name $latin 骨格 博物館 解説", "UTF-8")
    fun matches(query: String): Boolean = normalize("$name $latin $region $period $category $wikiTitle").contains(normalize(query.trim()))
    companion object {
        fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().map {
            if (it in '\u3041'..'\u3096') (it.code + 0x60).toChar() else it
        }.joinToString("")
    }
}

object Catalog {
    const val REVIEW_DATE = "2026-09-07"
    fun load(context: Context) = parse(context.assets.open("dinosaurs.json").bufferedReader().use { it.readText() })
    fun parse(text: String): List<Dinosaur> {
        val rows = JSONArray(text)
        return List(rows.length()) { index ->
            val r = rows.getJSONObject(index)
            val sections = r.getJSONArray("sections")
            val sources = r.getJSONArray("sources")
            val videos = r.getJSONArray("videos")
            val art=r.optJSONObject("artwork")
            Dinosaur(r.getString("id"), r.getString("name"), r.getString("latin"), r.getString("subtitle"),
                r.getString("category"), r.getString("period"), r.getString("age"), r.getDouble("startMa"), r.getDouble("endMa"),
                r.getString("region"), r.getString("size"), r.getString("diet"), r.getString("summary"),
                List(sections.length()) { val s = sections.getJSONObject(it); FactSection(s.getString("title"), s.getString("text")) },
                List(sources.length()) { val s = sources.getJSONObject(it); Source(s.getString("title"), s.getString("url")) },
                r.getString("modelNote"), r.getString("wikiTitle"),
                List(videos.length()) { val s=videos.getJSONObject(it); Source(s.getString("title"),s.getString("url")) },
                art?.let{Artwork(it.getString("asset"),it.getString("fileTitle"),it.getString("artist"),it.getString("source"),it.getString("license"),it.getString("licenseUrl"),it.optString("credit"),it.optString("note"))},
                r.optInt("rank"),r.optLong("pageviews"))
        }
    }
}
