package io.github.hatake716.dinobones

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.data.PaleoMaps
import io.github.hatake716.dinobones.data.GeologicPeriod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceLinkTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun wikipediaAndYoutubeButtonsDispatchDocumentedHttpsLinks() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val expected=Catalog.load(inst.targetContext).first{it.id=="tyrannosaurus"}
        val urls=java.util.Collections.synchronizedList(mutableListOf<String>())
        val monitor=object:Instrumentation.ActivityMonitor(){
            override fun onStartActivity(intent:Intent):Instrumentation.ActivityResult? {
                if(intent.action==Intent.ACTION_VIEW){urls.add(intent.dataString.orEmpty());return Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null)}
                return null
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            inst.addMonitor(monitor)
            try {
                val map=PaleoMaps.load(inst.targetContext).getValue(GeologicPeriod.CRETACEOUS)
                compose.onNodeWithTag("collection-grid").performScrollToNode(hasText("地図の出典"))
                compose.onNodeWithText("地図の出典").performClick()
                for((label,url) in listOf("地図の原本・権利情報" to map.source,"地図のライセンス条件" to map.licenseUrl,"PALEOMAPの研究資料" to "https://doi.org/10.5281/zenodo.10659112","時代区分の出典" to "https://stratigraphy.org/chart/")){
                    compose.onNodeWithText(label).performScrollTo().performClick();compose.waitForIdle();assertEquals(url,urls.last())
                }
                assertEquals(4,urls.size);urls.clear()
                compose.onNodeWithText("閉じる").performClick()
                compose.onNodeWithTag("collection-grid").performScrollToNode(hasText("ティラノサウルス"))
                compose.onAllNodes(hasText("ティラノサウルス",substring=true) and hasClickAction())[0].performClick()
                compose.onNodeWithText("詳細図鑑").performClick()
                for((label,url) in listOf("Wikipediaで詳しく読む" to expected.wikipedia,"YouTubeの関連動画を探す" to expected.youtube) + expected.videos.map{it.title to it.url}) {
                    compose.onNodeWithTag("details-list").performScrollToNode(hasText(label))
                    compose.onNodeWithText(label).performClick()
                    compose.waitForIdle()
                    assertEquals(url,urls.last())
                }
                assertEquals(2+expected.videos.size,urls.size)
                compose.onNodeWithText("3D骨格").performClick()
                compose.onNodeWithText("背景と出典").performClick()
                for((label,url) in listOf("画像の原本・権利情報" to expected.artwork!!.source,"画像のライセンス条件" to expected.artwork!!.licenseUrl)){
                    compose.onNodeWithText(label).performScrollTo().performClick();compose.waitForIdle();assertEquals(url,urls.last())
                }
            } finally { inst.removeMonitor(monitor) }
        }
    }
}
