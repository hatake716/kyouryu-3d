package io.github.hatake716.dinobones

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.app.Instrumentation
import android.graphics.Point
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.github.hatake716.dinobones.data.Catalog
import io.github.hatake716.dinobones.data.EarthColors
import io.github.hatake716.dinobones.model.*
import org.junit.Assert.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExhibitTest {
    @get:Rule val compose=createEmptyComposeRule()
    private fun allEras(){
        compose.onNodeWithTag("collection-grid").performScrollToNode(hasText("全時代"))
        val chip=compose.onNode(hasText("全時代") and hasClickAction())
        if(chip.fetchSemanticsNode().config[SemanticsProperties.Selected]!=true)chip.performClick()
        chip.assertIsSelected()
        compose.onNodeWithTag("collection-grid").performScrollToNode(hasSetTextAction())
    }
    private fun openAnimal(name:String){
        compose.onNodeWithTag("collection-grid").performScrollToNode(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement(name)
        val card=hasText(name) and hasClickAction() and !hasSetTextAction()
        compose.onNodeWithTag("collection-grid").performScrollToNode(card)
        compose.onNode(card).performClick();compose.waitForIdle()
    }
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private fun fresh(){if(android.os.Build.VERSION.SDK_INT>=33)inst.uiAutomation.clearCache()}
    private fun click(text:String){
        var target:UiObject2?=null
        repeat(80){
            compose.waitForIdle();fresh();target=device.findObjects(By.text(text)).firstOrNull{it.className!="android.widget.EditText"}
            if(target!=null){target!!.click();compose.waitForIdle();device.waitForIdle();return}
            Thread.sleep(100)
        }
        shot("failure");device.dumpWindowHierarchy(File(inst.targetContext.getExternalFilesDir(null),"failure.xml"));error("Missing $text")
    }
    private fun clickDescription(text:String){compose.waitForIdle();fresh();assertTrue("Missing $text",device.wait(Until.hasObject(By.desc(text)),10000));device.findObject(By.desc(text)).click();compose.waitForIdle();device.waitForIdle()}
    private fun findView(view:View):SkeletonView?{if(view is SkeletonView)return view;if(view is ViewGroup)for(i in 0 until view.childCount)findView(view.getChildAt(i))?.let{return it};return null}
    private fun viewer(s:ActivityScenario<MainActivity>):SkeletonView{var v:SkeletonView?=null;repeat(100){compose.waitForIdle();s.onActivity{v=findView(it.window.decorView)};if(v?.museumRenderer?.renderedVertexCount?:0>1000)return v!!;Thread.sleep(100)};error("No rendered mesh")}
    private fun shot(name:String){val path=File(inst.targetContext.getExternalFilesDir(null),"evidence");path.mkdirs();assertTrue(device.takeScreenshot(File(path,"$name.png")))}
    @Test fun repeatedViewerModeChangesReleaseGraphicsAndKeepCamera(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            compose.onNodeWithText("図鑑一覧").performClick();allEras();openAnimal("スピノサウルス")
            fun ready():SkeletonView{
                val v=viewer(s)
                compose.waitUntil(20000){compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().isEmpty()}
                return v
            }
            val records=StringBuilder();var start=0L
            repeat(10){i->
                val old=ready();val pose=old.museumRenderer.pose
                clickDescription("全画面で観察");val full=ready()
                assertTrue("Old GL resources deleted",old.museumRenderer.gpuResourcesReleased)
                assertEquals(pose,full.museumRenderer.pose);assertEquals("spinosaurus",full.museumRenderer.renderedMaterialId)
                clickDescription("通常表示に戻す");val back=ready()
                assertTrue("Fullscreen GL resources deleted",full.museumRenderer.gpuResourcesReleased)
                assertEquals(pose,back.museumRenderer.pose)
                if(i==1)start=android.os.Debug.getNativeHeapAllocatedSize()
                records.append("cycle=${i+1} native=${android.os.Debug.getNativeHeapAllocatedSize()}\n")
            }
            File(inst.targetContext.getExternalFilesDir(null),"viewer-recreation-memory.txt").writeText(records.toString())
            assertTrue("Viewer resources accumulate: $records",android.os.Debug.getNativeHeapAllocatedSize()-start<32L*1024*1024)
            s.recreate();ready();shot("fossil-after-ten-mode-changes")
        }
    }
    @Test fun fossilFinishesRenderWithSourceTexturesAndSurviveRecreation(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            compose.onNodeWithText("図鑑一覧").performClick();allEras()
            fun settledShot(name:String){
                compose.waitUntil(20000){compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().isEmpty()}
                val v=viewer(s);val frame=v.museumRenderer.frameCount;v.requestRender()
                compose.waitUntil(10000){v.museumRenderer.frameCount>frame};compose.waitForIdle();Thread.sleep(300);shot(name)
            }
            val catalog=Catalog.load(inst.targetContext)
            for(id in listOf("spinosaurus","velociraptor","brachiosaurus","stegosaurus","ankylosaurus","archaeopteryx","mosasaurus","patagotitan","triceratops","tyrannosaurus","carnotaurus")){
                openAnimal(catalog.first{it.id==id}.name);var v=viewer(s)
                compose.waitUntil(10000){v.museumRenderer.renderedMaterialId==FossilMaterials.forModel(id)?.id}
                assertEquals("Source maps retained: $id",id in FossilMaterials.sourceSurfaces,v.museumRenderer.surfaceTexturesReady)
                settledShot("fossil-full-$id")
                clickDescription("全画面で観察");clickDescription("拡大");clickDescription("拡大")
                v=viewer(s);settledShot("fossil-close-$id")
                if(id=="spinosaurus"){
                    val r=v.museumRenderer;val b=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
                    val before=r.pose;device.swipe(b.centerX()-100,b.centerY(),b.centerX()+120,b.centerY()+20,20)
                    assertTrue(r.pose.yaw<before.yaw);val pose=r.pose
                    s.recreate();v=viewer(s);assertEquals(pose,v.museumRenderer.pose)
                    assertEquals(id,v.museumRenderer.renderedMaterialId);settledShot("fossil-recreated-$id")
                    clickDescription("視点をリセット")
                }
                clickDescription("通常表示に戻す");clickDescription("一覧に戻る")
            }
        }
    }
    @Test fun landscapeViewerUsesAvailableHeightAndKeepsCameraAcrossModes(){
        Configurator.getInstance().setWaitForIdleTimeout(100)
        ActivityScenario.launch(MainActivity::class.java).use{s->
            compose.onNodeWithText("図鑑一覧").performClick();allEras()
            val catalog=Catalog.load(inst.targetContext)
            for(id in listOf("triceratops","tyrannosaurus","patagotitan","brachiosaurus","pteranodon","mosasaurus","archaeopteryx")){
                openAnimal(catalog.first{it.id==id}.name);viewer(s);Thread.sleep(500);shot("fit-portrait-$id")
                s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
                compose.waitUntil(10000){device.displayWidth>device.displayHeight};Thread.sleep(500)
                var v=viewer(s);val r=v.museumRenderer
                val bounds=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
                assertTrue("Landscape height: $id ${bounds.height()}",bounds.height()>device.displayHeight*.65f)
                assertEquals(v.height,r.viewportHeight);assertEquals(v.width,r.viewportWidth)
                assertEquals("Entire native viewport is visible",v.height,bounds.height())
                assertEquals(1f,r.pose.zoom,0f);assertTrue(r.fittedDistance>0)
                Thread.sleep(450);shot("fit-landscape-$id")
                if(id=="triceratops"){
                    val original=r.pose;compose.waitForIdle()
                    device.swipe(bounds.centerX()-120,bounds.centerY(),bounds.centerX()+150,bounds.centerY()+40,20);Thread.sleep(200)
                    assertTrue(r.pose.yaw<original.yaw-10)
                    clickDescription("拡大");val zoomed=r.pose;assertTrue(zoomed.zoom>1f)
                    clickDescription("全画面で観察");v=viewer(s);assertEquals(zoomed,v.museumRenderer.pose)
                    shot("fit-fullscreen-triceratops");clickDescription("通常表示に戻す");v=viewer(s);assertEquals(zoomed,v.museumRenderer.pose)
                    s.recreate();v=viewer(s);assertEquals(zoomed,v.museumRenderer.pose)
                    clickDescription("視点をリセット");assertEquals(CameraPose(),v.museumRenderer.pose)
                    compose.onNodeWithContentDescription("部位と観察のカード").performTouchInput{swipeUp()}
                    compose.onNodeWithContentDescription("詳細図鑑を閉じるハンドル").assertExists().performTouchInput{swipeDown()}
                    viewer(s);shot("fit-after-details-swipe")
                }
                s.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
                compose.waitUntil(10000){device.displayHeight>device.displayWidth};viewer(s)
                clickDescription("一覧に戻る")
            }
        }
    }
    @Test fun allHundredRenderAndGesturesPersistAcrossRecreation(){
        // This suite is run with ANDROID_SERIAL=emulator-5554. No real-device gesture injection.
        inst.targetContext.getSharedPreferences("collection",Context.MODE_PRIVATE).edit().clear().commit()
        Configurator.getInstance().setWaitForIdleTimeout(500)
        val entries=Catalog.load(inst.targetContext)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("図鑑一覧").performClick()
            device.wait(Until.hasObject(By.text("恐竜の骨格３D")),10000);Thread.sleep(2500);shot("00-collection")
            // Check the guide before scrolling, so the button has a settled position.
            clickDescription("このアプリについて");Thread.sleep(300);fresh()
            assertTrue(device.wait(Until.hasObject(By.textContains("100種類の古生物")),5000));shot("licenses-about");click("閉じる")
            device.swipe(500,1950,500,800,100);device.waitForIdle();Thread.sleep(600);click("全時代");Thread.sleep(350);fresh()
            assertTrue("All eras selected before starting",device.hasObject(By.desc("表示中の時代：全時代")))
            for(d in entries){
                fresh()
                assertTrue("All eras retained before ${d.id}",device.hasObject(By.desc("表示中の時代：全時代")))
                var field=device.findObject(By.clazz("android.widget.EditText"))
                if(field==null){device.swipe(500,1900,500,850,30);field=device.wait(Until.findObject(By.clazz("android.widget.EditText")),5000)}
                assertNotNull(field);field.click();field.text=d.name;Thread.sleep(400);scenario.onActivity{WindowInsetsControllerCompat(it.window,it.window.decorView).hide(WindowInsetsCompat.Type.ime())};device.waitForIdle();click(d.name)
                val v=viewer(scenario);assertTrue("Correct taxon title: ${d.id}",device.wait(Until.hasObject(By.text(d.latin)),5000));assertTrue(d.id,v.museumRenderer.renderedVertexCount>10000)
                repeat(60){if(!v.museumRenderer.backgroundReady)Thread.sleep(100)}
                assertTrue("Background: ${d.id}",v.museumRenderer.backgroundReady);Thread.sleep(350);shot(d.id)
                if(d.id=="triceratops"){
                    assertEquals(449994,v.museumRenderer.renderedVertexCount)
                    assertTrue("Specimen surface maps uploaded",v.museumRenderer.surfaceTexturesReady)
                    assertTrue("Emulator supports mapped surface normals",v.museumRenderer.normalMappingSupported)
                    click("モデルと実寸資料");assertTrue(device.wait(Until.hasObject(By.textContains("5.89 m")),5000));shot("triceratops-source");click("閉じる")
                    clickDescription("拡大");clickDescription("拡大");shot("triceratops-surface-closeup")
                    val before=v.museumRenderer.pose;scenario.recreate();val restored=viewer(scenario)
                    assertEquals(before,restored.museumRenderer.pose);assertTrue(restored.museumRenderer.surfaceTexturesReady)
                    clickDescription("視点をリセット")
                }
                if(d.id=="tyrannosaurus"){
                    val original=v.museumRenderer.pose
                    val bounds=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
                    device.swipe(bounds.centerX()-150,bounds.centerY(),bounds.centerX()+180,bounds.centerY()+70,25);Thread.sleep(250)
                    assertTrue("Rightward drag reverses horizontal orbit",v.museumRenderer.pose.yaw<original.yaw-5)
                    val beforeZoom=v.museumRenderer.pose.zoom
                    val obj=device.findObject(UiSelector().descriptionStartsWith("3D骨格。"))
                    obj.performTwoPointerGesture(Point(bounds.centerX()-70,bounds.centerY()),Point(bounds.centerX()+70,bounds.centerY()),Point(bounds.centerX()-210,bounds.centerY()),Point(bounds.centerX()+210,bounds.centerY()),30)
                    Thread.sleep(250);assertTrue("Pinch zoom: before=$beforeZoom after=${v.museumRenderer.pose.zoom}, bounds=$bounds",v.museumRenderer.pose.zoom>beforeZoom*1.2f)
                    val beforePan=v.museumRenderer.pose
                    obj.performTwoPointerGesture(Point(bounds.centerX()-110,bounds.centerY()-50),Point(bounds.centerX()+110,bounds.centerY()-50),Point(bounds.centerX()-50,bounds.centerY()+45),Point(bounds.centerX()+170,bounds.centerY()+45),25)
                    Thread.sleep(250);assertTrue(v.museumRenderer.pose.panX!=beforePan.panX)
                    shot("tyrannosaurus-gesture")
                    val pose=v.museumRenderer.pose;scenario.recreate();val restored=viewer(scenario);assertEquals(pose,restored.museumRenderer.pose)
                    clickDescription("視点をリセット");assertEquals(CameraPose(),restored.museumRenderer.pose)
                    click("部位：全身");click("頭骨");Thread.sleep(200);assertEquals(1,restored.museumRenderer.selected);shot("tyrannosaurus-highlight")
                    clickDescription("お気に入りに追加");assertTrue(inst.targetContext.getSharedPreferences("collection",Context.MODE_PRIVATE).getStringSet("favorites",emptySet())!!.contains(d.id))
                    click("詳細図鑑");assertTrue(device.hasObject(By.text(d.subtitle)));shot("tyrannosaurus-details")
                    click("3D骨格");viewer(scenario)
                    clickDescription("全画面で観察");shot("tyrannosaurus-fullscreen");clickDescription("通常表示に戻す")
                }
                clickDescription("一覧に戻る");Thread.sleep(150)
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use{
            compose.onNodeWithText("図鑑一覧").performClick()
            assertTrue(inst.targetContext.getSharedPreferences("collection",Context.MODE_PRIVATE).getStringSet("favorites",emptySet())!!.contains("tyrannosaurus"))
        }
    }
    @Test fun detailedSkullsAndScanSurfaceRender(){
        Configurator.getInstance().setWaitForIdleTimeout(500)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("図鑑一覧").performClick()
            allEras()
            val entries=Catalog.load(inst.targetContext)
            for(id in listOf("tyrannosaurus","carnotaurus","patagotitan","camarasaurus","spinosaurus","velociraptor","stegosaurus","brachiosaurus","ankylosaurus","pteranodon","mosasaurus")){
                val d=entries.first{it.id==id};fresh()
                openAnimal(d.name)
                val v=viewer(scenario);assertTrue(device.wait(Until.hasObject(By.text(d.latin)),5000));Thread.sleep(500)
                assertTrue(v.museumRenderer.renderedVertexCount>100000);shot("quality-$id")
                if(id=="carnotaurus"){assertEquals(782562,v.museumRenderer.renderedVertexCount);assertTrue(v.museumRenderer.surfaceTexturesReady)}
                if(id=="patagotitan"){
                    assertEquals(1050537,v.museumRenderer.renderedVertexCount);assertTrue(v.museumRenderer.contactShadowsReady)
                    scenario.recreate();val restored=viewer(scenario)
                    assertTrue("Contact shadows recreated",restored.museumRenderer.contactShadowsReady)
                    val frames=restored.museumRenderer.frameCount
                    scenario.onActivity{restored.preserveEGLContextOnPause=false;restored.onPause();restored.onResume();restored.requestRender()}
                    repeat(100){if(restored.museumRenderer.frameCount<=frames)Thread.sleep(100)}
                    assertTrue("Frame after EGL context loss",restored.museumRenderer.frameCount>frames)
                    assertTrue(restored.museumRenderer.contactShadowsReady)
                    val ready=java.util.concurrent.CountDownLatch(1);var glError=-1
                    restored.queueEvent{glError=android.opengl.GLES20.glGetError();ready.countDown()}
                    assertTrue(ready.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,glError)
                }
                if(id=="tyrannosaurus"){
                    assertEquals(750720,v.museumRenderer.renderedVertexCount);assertTrue(v.museumRenderer.surfaceTexturesReady)
                    click("モデルと実寸資料");assertTrue(device.wait(Until.hasObject(By.textContains("Rigsters")),5000));shot("quality-tyrannosaurus-source");click("閉じる")
                }
                clickDescription("拡大");clickDescription("拡大");Thread.sleep(300);shot("quality-$id-close")
                val latch=java.util.concurrent.CountDownLatch(1);var error=-1
                viewer(scenario).queueEvent{error=android.opengl.GLES20.glGetError();latch.countDown()}
                assertTrue(latch.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals("GLES: $id",0,error)
                clickDescription("一覧に戻る");Thread.sleep(250)
            }
        }
    }
    @Test fun specimenFirstFrameIsolationAndMeasuredSource(){
        Configurator.getInstance().setWaitForIdleTimeout(500)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("図鑑一覧").performClick()
            allEras()
            for((name,id) in listOf("トリケラトプス" to "triceratops","ステゴサウルス" to "stegosaurus")){
                openAnimal(name)
                val v=viewer(scenario);Thread.sleep(600)
                val latch=java.util.concurrent.CountDownLatch(1);var error=-1
                v.queueEvent{error=android.opengl.GLES20.glGetError();latch.countDown()}
                assertTrue(latch.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals("$id GL error",0,error)
                shot("$id-ready")
                click("モデルと実寸資料")
                assertTrue(device.wait(Until.hasObject(By.textContains(if(id=="triceratops")"5.89 m" else "868 mm")),5000))
                shot("$id-measurements");click("閉じる")
                if(id=="triceratops"){
                    click("部位：全身");click("頭骨")
                    clickDescription("選択部位のみを表示");Thread.sleep(400);assertTrue(v.museumRenderer.isolate);shot("triceratops-skull-isolated")
                    clickDescription("選択部位のみを表示");click("部位：頭骨");click("全身")
                    scenario.onActivity{v.camera(CameraPose(145f,20f,1.25f))};Thread.sleep(500);shot("triceratops-reverse")
                    scenario.recreate();val restored=viewer(scenario);assertTrue(restored.museumRenderer.surfaceTexturesReady)
                }
                clickDescription("一覧に戻る");Thread.sleep(250)
            }
        }
    }
    @Test fun exportOriginalStoreArtwork(){
        val dir=File(inst.targetContext.getExternalFilesDir(null),"store");dir.mkdirs()
        val drawable=inst.targetContext.getDrawable(R.drawable.ic_launcher)!!
        val icon=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,512,512);drawable.draw(Canvas(icon))
        File(dir,"icon-512.png").outputStream().use{icon.compress(Bitmap.CompressFormat.PNG,100,it)}
        val feature=Bitmap.createBitmap(1024,500,Bitmap.Config.ARGB_8888);val canvas=Canvas(feature)
        canvas.drawColor(EarthColors.Background);val p=Paint(Paint.ANTI_ALIAS_FLAG)
        p.color=EarthColors.Accent;p.textSize=18f;canvas.drawText("PALEO COLLECTION",54f,82f,p)
        p.color=0xFFEDE1C8.toInt();p.textSize=49f;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);canvas.drawText("恐竜の骨格３D",54f,182f,p)
        p.textSize=25f;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);canvas.drawText("太古の姿を、あらゆる角度から。",54f,250f,p)
        p.color=EarthColors.Accent;p.textSize=20f;canvas.drawText("時代の地図から出会う、100種類の骨格",54f,331f,p)
        p.color=EarthColors.Muted;p.textSize=16f;canvas.drawText("公開標本と実測資料で、骨格を観察",54f,428f,p)
        drawable.setBounds(700,125,950,375);drawable.draw(canvas)
        File(dir,"feature-1024x500.png").outputStream().use{feature.compress(Bitmap.CompressFormat.PNG,100,it)}
        assertEquals(1024,feature.width);assertEquals(500,feature.height)
    }
    private fun abs(value:Float)=kotlin.math.abs(value)
}
