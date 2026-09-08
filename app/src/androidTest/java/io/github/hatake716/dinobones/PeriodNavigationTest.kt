package io.github.hatake716.dinobones

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.data.EarthColors
import io.github.hatake716.dinobones.model.SkeletonView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PeriodNavigationTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private fun fresh(){if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()}
    private fun reveal(text:String):UiObject2 {
        repeat(10){
            // Wait for popup/layout transitions before a fallback swipe, which
            // would otherwise dismiss a just-opening part menu.
            device.wait(Until.hasObject(By.text(text)),1500)
            fresh();device.findObject(By.text(text))?.let{if(it.visibleBounds.height()>25)return it}
            device.swipe(520,1940,520,850,100);device.waitForIdle();Thread.sleep(500)
        }
        error("Missing text: $text")
    }
    private fun click(text:String){reveal(text).click();device.waitForIdle()}
    private fun description(text:String){device.wait(Until.findObject(By.desc(text)),10000)?.click()?:error("Missing $text");device.waitForIdle()}
    private fun mapBounds():Rect {
        repeat(8){
            fresh();val map=device.findObject(By.desc("時代の世界地図"))
            if(map!=null&&map.visibleBounds.height()>600)return map.visibleBounds
            device.swipe(520,720,520,1950,100);device.waitForIdle();Thread.sleep(500)
        }
        error("Map not visible")
    }
    private fun eraSwipe(older:Boolean,expected:String){
        val b=mapBounds();val left=b.left+b.width()/5;val right=b.right-b.width()/5
        device.swipe(if(older)left else right,b.centerY(),if(older)right else left,b.centerY(),100)
        device.waitForIdle();Thread.sleep(500)
        // Compose pager pages reuse virtual nodes; clear UiAutomator's old off-screen cache.
        fresh()
        var switched=false
        repeat(30){fresh();if(device.hasObject(By.descStartsWith("${expected}の世界地図")))switched=true else Thread.sleep(100)}
        if(!switched){shot("map-failure");device.dumpWindowHierarchy(File(inst.targetContext.getExternalFilesDir(null),"map-failure.xml"))}
        assertTrue("Map switched to $expected; swipe bounds=$b",switched)
    }
    private fun assertCollection(era:String,count:Int){
        reveal("コレクション")
        assertTrue(device.wait(Until.hasObject(By.desc("表示中の時代：$era")),5000))
        assertTrue(device.hasObject(By.text("$count / 100")))
    }
    private fun find(v:View):SkeletonView? {
        if(v is SkeletonView)return v
        if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it}
        return null
    }
    private fun viewer(s:ActivityScenario<MainActivity>):SkeletonView {
        var result:SkeletonView?=null
        repeat(100){s.onActivity{result=find(it.window.decorView)};if(result?.museumRenderer?.backgroundReady==true)return result!!;Thread.sleep(100)}
        error("No 3D viewer")
    }
    private fun shot(name:String){
        Thread.sleep(500)
        val dir=File(inst.targetContext.getExternalFilesDir(null),"evidence");dir.mkdirs()
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
    }
    @Test fun cretaceousCornersAndDownSwipeKeepReadingAndViewpoint(){
        Configurator.getInstance().setWaitForIdleTimeout(500)
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            device.wait(Until.findObject(By.text("図鑑一覧")),15000)?.click() ?: error("Missing collection shortcut")
            device.waitForIdle()
            val map=device.wait(Until.findObject(By.descStartsWith("白亜紀の世界地図")),10000)?:error("Missing map")
            Thread.sleep(700)
            val b=map.visibleBounds
            val screenshot=inst.uiAutomation.takeScreenshot()?:error("No screenshot")
            val background=EarthColors.Background.toInt()
            for(x in listOf(b.left+b.width()/20,b.right-b.width()/20)){
                for(y in listOf(b.top+b.height()/20,b.bottom-b.height()/20)){
                    assertEquals("Projection corner must show earth background",background,screenshot.getPixel(x,y))
                }
            }
            assertNotEquals("Map still renders at center",background,screenshot.getPixel(b.centerX(),b.centerY()))
            screenshot.recycle();shot("cretaceous-transparent-corners")
            click("ティラノサウルス");val v=viewer(s)
            click("部位：全身");click("頭骨")
            val vb=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
            device.swipe(vb.centerX()-120,vb.centerY(),vb.centerX()+180,vb.centerY(),40)
            Thread.sleep(200);val pose=v.museumRenderer.pose
            fun openDetails(){
                click("上にスワイプして詳細図鑑")
                assertTrue(device.wait(Until.hasObject(By.text("下にスワイプして骨格に戻る")),5000))
                device.waitForIdle();fresh()
            }
            fun bodySwipe(down:Boolean){
                val start=if(down)device.displayHeight*45/100 else device.displayHeight*78/100
                val end=if(down)device.displayHeight*78/100 else device.displayHeight*45/100
                device.swipe(device.displayWidth*9/10,start,device.displayWidth*9/10,end,100)
                device.waitForIdle();Thread.sleep(500);fresh()
            }
            openDetails();shot("expanded-field-notes")
            bodySwipe(true)
            assertEquals("Down swipe returns with pose",pose,viewer(s).museumRenderer.pose)
            assertEquals(1,viewer(s).museumRenderer.selected)
            openDetails();bodySwipe(false);bodySwipe(true)
            assertTrue("Reading back to the top must not close details",device.hasObject(By.text("下にスワイプして骨格に戻る")))
            bodySwipe(false)
            val handle=device.findObject(By.desc("詳細図鑑を閉じるハンドル")).visibleBounds
            device.swipe(handle.centerX(),handle.centerY(),handle.centerX(),handle.centerY()+300,80)
            device.waitForIdle()
            assertEquals("Handle closes even when scrolled",pose,viewer(s).museumRenderer.pose)
            openDetails();click("下にスワイプして骨格に戻る")
            assertEquals("Accessible button returns with pose",pose,viewer(s).museumRenderer.pose)
            shot("skeleton-after-down-swipe")
        }
    }
    @Test fun mapsFilterCollectionAndBottomCardSwipePreservesView(){
        Configurator.getInstance().setWaitForIdleTimeout(500)
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            device.wait(Until.findObject(By.text("図鑑一覧")),15000)?.click() ?: error("Missing collection shortcut")
            device.waitForIdle()
            assertTrue(device.wait(Until.hasObject(By.descStartsWith("白亜紀の世界地図")),10000));shot("period-cretaceous")
            eraSwipe(true,"ジュラ紀");shot("period-jurassic");assertCollection("ジュラ紀",25)
            reveal("アロサウルス")
            assertTrue(device.wait(Until.hasObject(By.desc("アロサウルスの復元イメージ")),5000));shot("collection-artwork")
            click("アロサウルス");val v=viewer(s)
            val initial=v.museumRenderer.pose;val bounds=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
            device.swipe(bounds.centerX()-130,bounds.centerY(),bounds.centerX()+150,bounds.centerY(),25);Thread.sleep(200)
            assertTrue("Rightward drag must turn in the reversed horizontal direction",v.museumRenderer.pose.yaw<initial.yaw-5)
            click("部位：全身");click("頭骨");assertEquals(1,v.museumRenderer.selected)
            val pose=v.museumRenderer.pose;shot("earth-exhibit")
            val card=device.findObject(By.desc("部位と観察のカード")).visibleBounds
            device.swipe(card.centerX(),card.bottom-70,card.centerX(),(card.top-240).coerceAtLeast(300),35)
            device.waitForIdle()
            val animal=Catalog.load(inst.targetContext).first{it.id=="allosaurus"}
            assertTrue("Up swipe opens details",device.wait(Until.hasObject(By.text(animal.subtitle)),5000));shot("details-from-swipe")
            device.pressBack();val restored=viewer(s)
            assertEquals("Viewpoint survives details",pose,restored.museumRenderer.pose);assertEquals(1,restored.museumRenderer.selected)
            description("一覧に戻る");assertCollection("ジュラ紀",25)
            s.recreate();assertCollection("ジュラ紀",25)
            eraSwipe(true,"三畳紀");shot("period-triassic");assertCollection("三畳紀",5)
            reveal("ヘレラサウルス");assertFalse(device.hasObject(By.text("ティラノサウルス")))
            eraSwipe(false,"ジュラ紀");eraSwipe(false,"白亜紀");assertCollection("白亜紀",72)
            click("全時代");assertCollection("全時代",100)
            eraSwipe(true,"ジュラ紀");assertCollection("ジュラ紀",25)
        }
    }
}
