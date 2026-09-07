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
import io.github.hatake716.dinobones.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExhibitTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(inst)
    private fun click(text:String){
        var target:UiObject2?=null
        repeat(80){
            target=device.findObjects(By.textContains(text)).firstOrNull{it.className!="android.widget.EditText"}
            if(target!=null){target!!.click();device.waitForIdle();return}
            Thread.sleep(100)
        }
        shot("failure");device.dumpWindowHierarchy(File(inst.targetContext.getExternalFilesDir(null),"failure.xml"));error("Missing $text")
    }
    private fun clickDescription(text:String){assertTrue("Missing $text",device.wait(Until.hasObject(By.desc(text)),10000));device.findObject(By.desc(text)).click();device.waitForIdle()}
    private fun findView(view:View):SkeletonView?{if(view is SkeletonView)return view;if(view is ViewGroup)for(i in 0 until view.childCount)findView(view.getChildAt(i))?.let{return it};return null}
    private fun viewer(s:ActivityScenario<MainActivity>):SkeletonView{var v:SkeletonView?=null;repeat(100){s.onActivity{v=findView(it.window.decorView)};if(v?.museumRenderer?.renderedVertexCount?:0>1000)return v!!;Thread.sleep(100)};error("No rendered mesh")}
    private fun shot(name:String){val path=File(inst.targetContext.getExternalFilesDir(null),"evidence");path.mkdirs();assertTrue(device.takeScreenshot(File(path,"$name.png")))}
    @Test fun allThirteenRenderAndGesturesPersistAcrossRecreation(){
        // This suite is run with ANDROID_SERIAL=emulator-5554. No real-device gesture injection.
        inst.targetContext.getSharedPreferences("collection",Context.MODE_PRIVATE).edit().clear().commit()
        val entries=Catalog.load(inst.targetContext)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            device.wait(Until.hasObject(By.text("恐竜の骨格３D")),10000);Thread.sleep(2500);shot("00-collection")
            for(d in entries){
                var field=device.findObject(By.clazz("android.widget.EditText"))
                if(field==null){device.swipe(500,1900,500,850,30);field=device.wait(Until.findObject(By.clazz("android.widget.EditText")),5000)}
                assertNotNull(field);field.click();field.text=d.name;Thread.sleep(400);scenario.onActivity{WindowInsetsControllerCompat(it.window,it.window.decorView).hide(WindowInsetsCompat.Type.ime())};device.waitForIdle();click(d.name)
                val v=viewer(scenario);assertTrue(d.id,v.museumRenderer.renderedVertexCount>10000);Thread.sleep(350);shot(d.id)
                if(d.id=="tyrannosaurus"){
                    val original=v.museumRenderer.pose
                    val bounds=device.findObject(By.descStartsWith("3D骨格。")).visibleBounds
                    device.swipe(bounds.centerX()-150,bounds.centerY(),bounds.centerX()+180,bounds.centerY()+70,25);Thread.sleep(250)
                    assertTrue(abs(v.museumRenderer.pose.yaw-original.yaw)>5)
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
                clickDescription("一覧に戻る")
            }
            clickDescription("このアプリについて");assertTrue(device.wait(Until.hasObject(By.textContains("13種類の古生物")),5000));shot("licenses-about");click("閉じる")
        }
        ActivityScenario.launch(MainActivity::class.java).use{
            assertTrue(inst.targetContext.getSharedPreferences("collection",Context.MODE_PRIVATE).getStringSet("favorites",emptySet())!!.contains("tyrannosaurus"))
        }
    }
    @Test fun exportOriginalStoreArtwork(){
        val dir=File(inst.targetContext.getExternalFilesDir(null),"store");dir.mkdirs()
        val drawable=inst.targetContext.getDrawable(R.drawable.ic_launcher)!!
        val icon=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,512,512);drawable.draw(Canvas(icon))
        File(dir,"icon-512.png").outputStream().use{icon.compress(Bitmap.CompressFormat.PNG,100,it)}
        val feature=Bitmap.createBitmap(1024,500,Bitmap.Config.ARGB_8888);val canvas=Canvas(feature)
        canvas.drawColor(0xFF101815.toInt());val p=Paint(Paint.ANTI_ALIAS_FLAG)
        p.color=0xFFB6D8A5.toInt();p.textSize=18f;canvas.drawText("PALEO COLLECTION",54f,82f,p)
        p.color=0xFFEDE1C8.toInt();p.textSize=49f;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);canvas.drawText("恐竜の骨格３D",54f,182f,p)
        p.textSize=25f;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);canvas.drawText("太古の姿を、あらゆる角度から。",54f,250f,p)
        p.color=0xFFB6D8A5.toInt();p.textSize=20f;canvas.drawText("13種類の骨格  /  出典付きの日本語図鑑",54f,331f,p)
        p.color=0xFFA6B3AA.toInt();p.textSize=16f;canvas.drawText("独自制作の学習用・模式復元モデル",54f,428f,p)
        drawable.setBounds(700,125,950,375);drawable.draw(canvas)
        File(dir,"feature-1024x500.png").outputStream().use{feature.compress(Bitmap.CompressFormat.PNG,100,it)}
        assertEquals(1024,feature.width);assertEquals(500,feature.height)
    }
    private fun abs(value:Float)=kotlin.math.abs(value)
}
