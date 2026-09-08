package io.github.hatake716.dinobones

import android.os.Bundle
import android.content.res.Configuration
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.hatake716.dinobones.ui.DinoApp

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        updateSystemBars();setContent { DinoApp() }
    }
    // Android restores window flags after onCreate when an Activity is recreated.
    override fun onPostResume(){
        super.onPostResume()
        window.decorView.post{updateSystemBars();androidx.core.view.ViewCompat.requestApplyInsets(window.decorView)}
    }
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus)updateSystemBars()}
    private fun updateSystemBars(){applyLandscapeSystemBars(window,resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE)}
}

@Suppress("DEPRECATION")
internal fun applyLandscapeSystemBars(window:Window,landscape:Boolean){
    WindowCompat.setDecorFitsSystemWindows(window,false)
    // Android 15 can retain its content-inset listener when reusing a window on
    // recreation. Its listener honors these layout flags even with enforced edge-to-edge.
    if(android.os.Build.VERSION.SDK_INT==35)window.decorView.systemUiVisibility=window.decorView.systemUiVisibility or
        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    if(android.os.Build.VERSION.SDK_INT>=28)window.attributes=window.attributes.apply{
        layoutInDisplayCutoutMode=when{
            landscape&&android.os.Build.VERSION.SDK_INT>=30->WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            landscape->WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            else->WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
    }
    WindowCompat.getInsetsController(window,window.decorView).apply{
        systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if(landscape)hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
    }
}
