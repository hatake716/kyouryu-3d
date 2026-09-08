package io.github.hatake716.dinobones

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.github.hatake716.dinobones.model.SkeletonView
import io.github.hatake716.dinobones.data.EarthColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BackgroundTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private fun find(v:View):SkeletonView? {
        if(v is SkeletonView)return v
        if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it}
        return null
    }
    private fun viewer(s:ActivityScenario<MainActivity>):SkeletonView {
        var view:SkeletonView?=null
        repeat(100){compose.waitForIdle();s.onActivity{view=find(it.window.decorView)};if(view?.museumRenderer?.backgroundReady==true&&(view?.museumRenderer?.renderedVertexCount?:0)>1000)return view!!;Thread.sleep(100)}
        error("Background did not render")
    }
    private fun click(text:String){compose.waitForIdle();device.wait(Until.findObject(By.text(text)),10000)?.click()?:error("Missing $text");compose.waitForIdle();device.waitForIdle()}
    private fun shot(name:String){
        val dir=File(inst.targetContext.getExternalFilesDir(null),"evidence");dir.mkdirs()
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
    }
    private fun strength(fraction:Float){
        compose.waitForIdle()
        val slider=device.wait(Until.findObject(By.desc("背景の濃さ")),5000)?:error("No slider")
        val b=slider.visibleBounds
        val end=if(fraction==0f)(b.left-80).coerceAtLeast(1) else b.left+2+((b.width()-4)*fraction).toInt()
        device.swipe(b.centerX(),b.centerY(),end,b.centerY(),20)
        device.waitForIdle();Thread.sleep(200)
    }
    @Test fun backgroundControlsKeepCameraAndSurviveRecreation(){
        Configurator.getInstance().setWaitForIdleTimeout(500)
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            compose.onNodeWithText("図鑑一覧").performClick()
            // Scroll to the actual card rather than relying on fixed portrait coordinates.
            val card=hasText("ティラノサウルス") and hasClickAction() and !hasSetTextAction()
            compose.onNodeWithTag("collection-grid").performScrollToNode(card)
            compose.onNode(card).performClick();compose.waitForIdle()
            val view=viewer(s);val pose=view.museumRenderer.pose
            click("背景と出典");strength(0f);click("閉じる");Thread.sleep(300)
            assertEquals(0f,view.museumRenderer.artworkStrength)
            assertEquals(pose,view.museumRenderer.pose)
            // This viewport pixel is away from the bones, floor and text.
            val point=IntArray(2);s.onActivity{view.getLocationOnScreen(point)}
            val earth=EarthColors.Background
            var c=0;var matched=false
            // The dialog and the separate GL surface are composed asynchronously. Wait for
            // the requested color to be presented, while retaining the exact color assertion.
            repeat(40){
                if(!matched){val screen=inst.uiAutomation.takeScreenshot();c=screen.getPixel(point[0]+4,point[1]+view.height/3);screen.recycle()
                    matched=abs(Color.red(c)-Color.red(earth))<5 && abs(Color.green(c)-Color.green(earth))<5 && abs(Color.blue(c)-Color.blue(earth))<5
                    if(!matched)Thread.sleep(100)
                }
            }
            assertTrue("Background off pixel $c after presentation wait",matched)
            shot("background-off")
            click("背景と出典");strength(.55f);click("画像全体");shot("background-attribution")
            click("閉じる");Thread.sleep(300)
            val actual=view.museumRenderer.artworkStrength
            assertFalse(view.museumRenderer.backgroundFill);assertTrue(actual in .45f.. .65f)
            shot("background-fit")
            s.recreate();val restored=viewer(s)
            assertFalse(restored.museumRenderer.backgroundFill);assertEquals(actual,restored.museumRenderer.artworkStrength);assertEquals(pose,restored.museumRenderer.pose)
            click("背景と出典");click("画面に広げる");strength(.32f);click("閉じる");Thread.sleep(300);shot("background-cover")
        }
    }
}
