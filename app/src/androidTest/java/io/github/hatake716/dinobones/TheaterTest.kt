package io.github.hatake716.dinobones

import android.graphics.Point
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.theater.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class TheaterTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"theater-evidence").also{it.mkdirs()}
    private fun fresh(){if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()}
    private fun waitFor(label:String,timeout:Long=20000,condition:()->Boolean){
        val end=SystemClock.uptimeMillis()+timeout
        while(SystemClock.uptimeMillis()<end){if(condition())return;Thread.sleep(80)}
        shot("failure");error("Timed out: $label")
    }
    private fun click(text:String){fresh();device.wait(Until.findObject(By.text(text)),15000)?.click()?:error("Missing $text");device.waitForIdle()}
    private fun desc(text:String){fresh();device.wait(Until.findObject(By.desc(text)),15000)?.click()?:error("Missing $text");device.waitForIdle()}
    private fun find(v:View):TheaterView?{if(v is TheaterView)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it};return null}
    private fun hall(s:ActivityScenario<MainActivity>):TheaterView {
        var v:TheaterView?=null
        waitFor("museum rendered"){s.onActivity{v=find(it.window.decorView)};v?.renderer?.frameCount?.let{it>3}==true&&v?.renderer?.renderedIds?.isNotEmpty()==true}
        return v!!
    }
    private fun shot(name:String){Thread.sleep(500);assertTrue(device.takeScreenshot(File(dir,"$name.png")))}
    private fun finishSearch(){
        val key=device.wait(Until.findObject(By.pkg("com.google.android.inputmethod.latin").desc("Search")),2500)
        if(key!=null)key.click() else device.pressEnter()
        device.waitForIdle();Thread.sleep(450);fresh()
    }
    private fun travel(name:String){
        click("館内マップ")
        val search=device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)?:error("No map search")
        search.click();search.text=name;finishSearch()
        desc("${name}の展示へ移動")
        // Closing the dialog also dismisses its keyboard.
        waitFor("map dismissed"){fresh();!device.hasObject(By.desc("館内マップを閉じる"))}
    }
    private fun center(v:TheaterView):Point {val xy=IntArray(2);inst.runOnMainSync{v.getLocationOnScreen(xy)};return Point(xy[0]+v.width/2,xy[1]+v.height/2)}
    @Test fun allHundredIllustrationStandsOpenTheirOwnImagesAndKeepMuseumPosition(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        val catalog=Catalog.load(inst.targetContext).associateBy{it.id}
        ActivityScenario.launch(MainActivity::class.java).use{s->
            for(spot in TheaterWorld.spots){
                var v=hall(s)
                val b=IllustrationBoard(spot,false)
                val p=WalkPose(b.x,b.z+2f,0f,atan2(TheaterWorld.EYE_HEIGHT-b.y-b.height/2,2f)*180f/PI.toFloat())
                s.onActivity{v.travel(spot);v.renderer.pose=p}
                waitFor("boards ready"){v.renderer.illustrationsReady&&v.renderer.renderedIds.isNotEmpty()}
                Thread.sleep(250)
                val point=center(v);device.click(point.x,point.y)
                val d=catalog.getValue(spot.id)
                assertTrue("Correct illustration: ${d.id}",device.wait(Until.hasObject(By.desc("${d.name}の生体復元画")),10000))
                val frames=v.renderer.frameCount;Thread.sleep(250);assertEquals("Picture pauses museum",frames,v.renderer.frameCount)
                if(spot.index%20==0){
                    shot("illustration-${d.id}");click("作者・出典")
                    assertTrue(device.wait(Until.hasObject(By.text("作者：${d.artwork!!.artist}")),5000))
                    shot("illustration-source-${d.id}");click("イラストに戻る")
                }
                if(spot.index==0){
                    val image=device.findObject(UiSelector().description("${d.name}の生体復元画"))
                    val bounds=device.findObject(By.desc("${d.name}の生体復元画")).visibleBounds
                    image.performTwoPointerGesture(Point(bounds.centerX()-90,bounds.centerY()),Point(bounds.centerX()+90,bounds.centerY()),Point(bounds.centerX()-270,bounds.centerY()),Point(bounds.centerX()+270,bounds.centerY()),30)
                    Thread.sleep(300);shot("illustration-zoom")
                    s.recreate();assertTrue(device.wait(Until.hasObject(By.desc("${d.name}の生体復元画")),10000))
                    desc("イラストの表示をリセット")
                    s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
                    waitFor("illustration landscape"){device.displayWidth>device.displayHeight}
                    assertTrue(device.wait(Until.hasObject(By.desc("${d.name}の生体復元画")),10000));shot("illustration-landscape")
                    s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
                    waitFor("portrait restored"){device.displayHeight>device.displayWidth}
                }
                desc("復元イラストを閉じる");v=hall(s);assertEquals("Return to same stand ${d.id}",p,v.renderer.pose);assertEquals(0,v.renderer.glError)
                android.util.Log.i("IllustrationTour","${spot.index+1}/100 ${d.id} matched and returned")
            }
        }
    }
    @Test fun landscapeFillsDisplayAndHidesBarsAcrossDialogsAndPortraitReturn(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            fun barsVisible():Boolean{
                var visible=true
                s.onActivity{activity->
                    val insets=androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                    visible=insets==null||insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars())||insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                }
                return visible
            }
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            waitFor("landscape"){device.displayWidth>device.displayHeight};waitFor("system bars hidden"){!barsVisible()}
            // A fresh Android install displays its own immersive-mode tutorial over the app.
            device.wait(Until.findObject(By.text("Got it").pkg("android")),4000)?.click()
            device.waitForIdle()
            var v=hall(s);val xy=IntArray(2);s.onActivity{v.getLocationOnScreen(xy)}
            assertArrayEquals("Museum starts at display origin",intArrayOf(0,0),xy)
            assertEquals("Full width",device.displayWidth,v.width);assertEquals("Full height",device.displayHeight,v.height)
            fun fullDisplay(label:String){
                Thread.sleep(500);s.onActivity{v.getLocationOnScreen(xy)}
                s.onActivity{a->
                    File(dir,"layout-$label.txt").writeText(buildString{
                        appendLine(a.resources.configuration);appendLine(a.window.attributes)
                        var node:View?=v
                        while(node!=null){val n=node!!;val p=IntArray(2);n.getLocationOnScreen(p);appendLine("${n.javaClass.name}: ${p.toList()} ${n.width} x ${n.height}");node=n.parent as? View}
                    })
                }
                shot("immersive-$label")
                assertArrayEquals("Museum origin after $label",intArrayOf(0,0),xy)
                assertEquals("Full width after $label",device.displayWidth,v.width)
                assertEquals("Full height after $label",device.displayHeight,v.height)
            }
            shot("immersive-landscape")
            desc("館内の歩き方");shot("immersive-help");click("散策を続ける");waitFor("bars after help"){!barsVisible()}
            click("館内マップ");shot("immersive-map");desc("館内マップを閉じる");waitFor("bars after map"){!barsVisible()}
            click("図鑑一覧");desc("シアターへ");v=hall(s);waitFor("bars after collection"){!barsVisible()}
            fullDisplay("collection-return")
            val pose=v.renderer.pose;s.recreate();v=hall(s);assertEquals(pose,v.renderer.pose);waitFor("bars after recreation"){!barsVisible()}
            fullDisplay("recreated")
            s.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);s.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);hall(s);waitFor("bars after resume"){!barsVisible()}
            fullDisplay("resumed")
            device.swipe(device.displayWidth/2,1,device.displayWidth/2,180,15);shot("immersive-edge-swipe")
            Thread.sleep(5000);shot("immersive-bars-auto-hidden");waitFor("temporary bars hidden"){!barsVisible()}
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            waitFor("portrait"){device.displayHeight>device.displayWidth}
            waitFor("portrait bars restored"){
                var shown=false;s.onActivity{a->val i=androidx.core.view.ViewCompat.getRootWindowInsets(a.window.decorView);shown=i?.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars())==true&&i.isVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars())};shown
            }
            hall(s);shot("immersive-portrait-restored")
        }
    }
    @Test fun allHundredExhibitsUseTheirOwnFossilOrSourceSurface(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            val v=hall(s);val records=StringBuilder()
            for(spot in TheaterWorld.spots){
                s.onActivity{v.travel(spot)}
                waitFor("surface ${spot.id}",30000){spot.id in v.renderer.renderedIds}
                val frame=v.renderer.frameCount;waitFor("presented finish ${spot.id}"){v.renderer.frameCount>frame+2}
                val original=spot.id in io.github.hatake716.dinobones.model.FossilMaterials.sourceSurfaces
                assertEquals(spot.id,!original,spot.id in v.renderer.renderedFossilIds)
                assertEquals("GL ${spot.id}",0,v.renderer.glError);assertNull(v.renderer.failed)
                records.append("${spot.id}\t${if(original)"source" else "fossil"}\t${v.renderer.lastFrameMillis} ms CPU submission\n")
                if(spot.id in setOf("spinosaurus","brachiosaurus","archaeopteryx","patagotitan","triceratops"))shot("fossil-hall-${spot.id}")
            }
            File(dir,"fossil-tour.tsv").writeText(records.toString())
        }
    }
    @Test fun defaultMuseumSupportsTwoFingerWalkingSelectionAndReturn(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            var v=hall(s);assertTrue(device.hasObject(By.text("恐竜の骨格３D")));shot("entrance")
            travel("トリケラトプス");v=hall(s);waitFor("triceratops visible"){"triceratops" in v.renderer.renderedIds};Thread.sleep(600);shot("triceratops")
            val before=v.renderer.pose;val p=center(v)
            device.swipe(p.x+160,p.y,p.x+420,p.y+120,25);Thread.sleep(200)
            assertTrue("Rightward drag turns to the right",v.renderer.pose.yaw>before.yaw+10)
            assertTrue("Downward drag looks down",v.renderer.pose.pitch>before.pitch+3)
            travel("ティラノサウルス");v=hall(s)
            val xy=IntArray(2);inst.runOnMainSync{v.controls.getLocationOnScreen(xy)}
            val jx=xy[0]+v.controls.stickX;val jy=xy[1]+v.controls.stickY;val radius=v.controls.radius
            val start=v.renderer.pose;val down=SystemClock.uptimeMillis()
            fun event(action:Int,coords:List<Pair<Float,Float>>){
                val properties=Array(coords.size){i->MotionEvent.PointerProperties().apply{id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
                val points=Array(coords.size){i->MotionEvent.PointerCoords().apply{x=coords[i].first;y=coords[i].second;pressure=1f;size=1f}}
                val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,coords.size,properties,points,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
                assertTrue(inst.uiAutomation.injectInputEvent(e,true));e.recycle()
            }
            val lookX=p.x+180f;val lookY=p.y.toFloat()
            event(MotionEvent.ACTION_DOWN,listOf(jx to jy))
            event(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),listOf(jx to jy,lookX to lookY))
            repeat(25){i->event(MotionEvent.ACTION_MOVE,listOf(jx to jy-radius*.9f,(lookX+i*4) to lookY));Thread.sleep(35)}
            event(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),listOf(jx to jy-radius*.9f,(lookX+100) to lookY))
            event(MotionEvent.ACTION_UP,listOf(jx to jy-radius*.9f));Thread.sleep(200)
            val walked=v.renderer.pose
            assertTrue("Movement and look both respond to separate fingers",hypot(walked.x-start.x,walked.z-start.z)>.7f&&walked.yaw>start.yaw+5f)
            assertEquals(0f,v.renderer.forward,0f);assertEquals(0f,v.renderer.strafe,0f)
            travel("トリケラトプス");v=hall(s);waitFor("target mesh"){"triceratops" in v.renderer.renderedIds};Thread.sleep(350)
            val saved=v.renderer.pose;val center=center(v);device.click(center.x,center.y)
            assertTrue(device.wait(Until.hasObject(By.text("下にスワイプして骨格に戻る")),10000))
            assertTrue(device.hasObject(By.text(Catalog.load(inst.targetContext).first{it.id=="triceratops"}.latin)));shot("selected-details")
            desc("展示に戻る");v=hall(s);assertEquals(saved,v.renderer.pose)
            s.recreate();v=hall(s);assertEquals("Recreation retains location and view",saved,v.renderer.pose)
            val frames=v.renderer.frameCount
            s.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);Thread.sleep(450)
            val stopped=v.renderer.frameCount;Thread.sleep(450);assertEquals("Background stops rendering",stopped,v.renderer.frameCount)
            s.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);v=hall(s);assertEquals(saved,v.renderer.pose);waitFor("context resumes"){v.renderer.frameCount>frames+5}
            desc("館内の歩き方");click("散策を続ける")
        }
    }
    @Test fun all100DisplaysLoadAndOpenTheirOwnRecordsAcrossEveryGallery(){
        Configurator.getInstance().setWaitForIdleTimeout(80)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            hall(s);val catalog=Catalog.load(inst.targetContext).associateBy{it.id};val records=StringBuilder()
            for((i,spot) in TheaterWorld.spots.withIndex()){
                val d=catalog.getValue(spot.id);travel(d.name);val v=hall(s)
                waitFor("${d.id} in gallery",30000){d.id in v.renderer.renderedIds}
                assertEquals(spot.section,TheaterWorld.section(v.renderer.pose));assertNull(v.renderer.failed);assertEquals("OpenGL ${d.id}",0,v.renderer.glError)
                val frames=v.renderer.frameCount;waitFor("presented ${d.id}"){v.renderer.frameCount>frames+3}
                assertTrue("${d.id} accessible detail button appears after loading",device.wait(Until.hasObject(By.text("詳細図鑑を開く")),10000))
                val saved=v.renderer.pose;assertEquals(d.id,TheaterWorld.pick(saved,0f,0f,v.width.toFloat()/v.height,v.renderer.exhibitHeights)?.id)
                val p=center(v);device.click(p.x,p.y)
                val opened=device.wait(Until.hasObject(By.text(d.latin)),10000)
                if(!opened){shot("failed-${d.id}");device.dumpWindowHierarchy(File(dir,"failed-${d.id}.xml"))}
                assertTrue("${d.id} data opened",opened)
                assertTrue(device.hasObject(By.desc("展示に戻る")))
                desc("展示に戻る");val back=hall(s);assertEquals(saved,back.renderer.pose);assertSame("One museum renderer survives navigation",v,back)
                waitFor("returned display"){d.id in back.renderer.renderedIds}
                if(i<8||i%10==0||i==99)shot("display-${i+1}-${d.id}")
                records.append("${i+1}\t${d.id}\tsection=${spot.section+1}\tposition=${saved.x},${saved.z}\tloaded=${back.renderer.loadedCount}\n")
                File(dir,"all-exhibits.tsv").writeText(records.toString())
                if(i%10==0||i==99){val memory=device.executeShellCommand("dumpsys meminfo ${inst.targetContext.packageName}");File(dir,"memory-${i+1}.txt").writeText(memory)}
            }
            val v=hall(s);Thread.sleep(1000);val start=SystemClock.elapsedRealtime();val frame=v.renderer.frameCount;Thread.sleep(5000)
            File(dir,"performance.txt").writeText("Emulator SwiftShader only; frames=${v.renderer.frameCount-frame}; seconds=${(SystemClock.elapsedRealtime()-start)/1000f}; last CPU submission ms=${v.renderer.lastFrameMillis}; cached displays=${v.renderer.loadedCount}\n")
        }
    }
    @Test fun repeatedRecreationReleasesMuseumResources(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            hall(s);Thread.sleep(1500)
            val start=android.os.Debug.getNativeHeapAllocatedSize();val records=StringBuilder("initial=$start\n")
            repeat(6){
                s.recreate();hall(s);Thread.sleep(700)
                records.append("recreation=${it+1} native=${android.os.Debug.getNativeHeapAllocatedSize()}\n")
                File(dir,"recreation-memory.txt").writeText(records.toString())
            }
            // Six leaked mural textures alone would exceed this allowance.
            assertTrue("Recreating museum must not retain scene textures: $records",android.os.Debug.getNativeHeapAllocatedSize()-start<24L*1024*1024)
        }
    }
    @Test fun originalMenuShortcutAndSelectedPlaceSurviveRoundTrip(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            hall(s);travel("ステゴサウルス");val v=hall(s);val saved=v.renderer.pose
            click("図鑑一覧");assertTrue(device.wait(Until.hasObject(By.descStartsWith("白亜紀の世界地図")),15000));shot("collection-shortcut")
            desc("シアターへ");assertEquals(saved,hall(s).renderer.pose)
            click("館内マップ");shot("museum-map");desc("館内マップを閉じる")
            desc("館内の歩き方");assertTrue(device.wait(Until.hasObject(By.text("散策を続ける")),3000));shot("museum-guide");click("散策を続ける")
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            waitFor("landscape viewport"){device.displayWidth>device.displayHeight};val landscape=hall(s);val landscapeFrames=landscape.renderer.frameCount
            waitFor("landscape rendered"){landscape.renderer.frameCount>landscapeFrames+20};assertEquals(saved,landscape.renderer.pose);assertEquals(0,landscape.renderer.glError);shot("museum-landscape")
            click("館内マップ")
            val search=device.wait(Until.findObject(By.clazz("android.widget.EditText")),5000)?:error("Landscape search missing")
            search.click();search.text="Triceratops";finishSearch()
            shot("museum-landscape-map");device.dumpWindowHierarchy(File(dir,"landscape-map.xml"));assertTrue(device.wait(Until.hasObject(By.desc("トリケラトプスの展示へ移動")),5000))
            desc("トリケラトプスの展示へ移動");val jumped=hall(s);assertEquals(TheaterWorld.byId.getValue("triceratops").arrival(jumped.renderer.exhibitHeights.getValue("triceratops")),jumped.renderer.pose)
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};waitFor("portrait viewport"){device.displayHeight>device.displayWidth};hall(s)
            click("入口へ");waitFor("entrance reset"){hall(s).renderer.pose==WalkPose()}
        }
    }
    @Test fun physicalScaleLabelsAndSmallSpecimensHaveAccessibleRecords(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            hall(s);assertTrue(device.hasObject(By.text("恐竜の骨格３D")))
            for(id in listOf("patagotitan","archaeopteryx","brachiosaurus","quetzalcoatlus","triceratops")){
                val d=Catalog.load(inst.targetContext).first{it.id==id};travel(d.name);val v=hall(s)
                waitFor("scaled $id rendered"){id in v.renderer.renderedIds}
                assertTrue(device.wait(Until.hasObject(By.text(ExhibitScales.entries.getValue(id).label)),10000))
                shot("metre-$id");click("詳細図鑑を開く")
                assertTrue(device.wait(Until.hasObject(By.text(d.latin)),10000));desc("展示に戻る");hall(s)
            }
            desc("館内の歩き方");val button=device.wait(Until.findObject(By.text("寸法の根拠と出典を確認")),5000)
            if(button==null){device.swipe(550,1700,550,900,20)}
            click("寸法の根拠と出典を確認");assertTrue(device.wait(Until.hasObject(By.text("館内の縮尺")),5000));shot("scale-notes")
            click("閉じる");val v=hall(s);val frames=v.renderer.frameCount;waitFor("resume after scale notes"){v.renderer.frameCount>frames+3}
        }
    }

    @Test fun forestMuralSurvivesGalleryChangesAndLandscapeRecreation(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            hall(s);val catalog=Catalog.load(inst.targetContext).associateBy{it.id}
            for(section in 0..4){
                val spot=TheaterWorld.spots.first{it.section==section}
                travel(catalog.getValue(spot.id).name);val v=hall(s)
                waitFor("forest wall uploaded in gallery $section"){v.renderer.muralReady}
                val f=v.renderer.frameCount;waitFor("forest presented"){v.renderer.frameCount>f+3}
                assertEquals("Forest and specimen GL error",0,v.renderer.glError)
                shot("forest-gallery-${section+1}")
            }
            click("入口へ");val saved=hall(s).renderer.pose
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            waitFor("landscape"){device.displayWidth>device.displayHeight}
            var v=hall(s);waitFor("landscape forest"){v.renderer.muralReady}
            assertEquals(saved,v.renderer.pose);assertTrue(device.hasObject(By.text("恐竜の骨格３D")))
            shot("forest-landscape")
            s.recreate();v=hall(s);assertEquals(saved,v.renderer.pose);assertTrue(v.renderer.muralReady)
            assertEquals(0,v.renderer.glError);shot("forest-landscape-recreated")
            desc("館内の歩き方");click("散策を続ける");hall(s)
            click("館内マップ");desc("館内マップを閉じる");hall(s)
            s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            waitFor("portrait"){device.displayHeight>device.displayWidth};v=hall(s);assertTrue(v.renderer.muralReady)
            assertEquals(saved,v.renderer.pose);shot("forest-portrait")
        }
    }

}
