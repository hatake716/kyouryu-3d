package io.github.hatake716.dinobones.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import io.github.hatake716.dinobones.applyLandscapeSystemBars

/** Dialogs own a separate window, so they also follow the current orientation. */
@Composable internal fun DialogSystemBars(){
    val view=LocalView.current
    val window=(view.parent as? DialogWindowProvider)?.window
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(window,landscape){
        window?.let{applyLandscapeSystemBars(it,landscape)}
        onDispose{}
    }
}
