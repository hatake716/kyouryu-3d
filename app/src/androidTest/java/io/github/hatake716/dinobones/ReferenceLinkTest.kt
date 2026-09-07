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
            compose.onAllNodes(hasText("ティラノサウルス",substring=true) and hasClickAction())[0].performClick()
            compose.onNodeWithText("詳細図鑑").performClick()
            inst.addMonitor(monitor)
            try {
                for((label,url) in listOf("Wikipediaで詳しく読む" to expected.wikipedia,"YouTubeの関連動画を探す" to expected.youtube) + expected.videos.map{it.title to it.url}) {
                    compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
                    compose.onNodeWithText(label).performClick()
                    compose.waitForIdle()
                    assertEquals(url,urls.last())
                }
                assertEquals(2+expected.videos.size,urls.size)
            } finally { inst.removeMonitor(monitor) }
        }
    }
}
