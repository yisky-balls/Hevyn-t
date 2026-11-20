package com.yifeng.commandcenter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yifeng.commandcenter.ui.FitnessApp
import com.yifeng.commandcenter.ui.HevyntAppViewModel
import com.yifeng.commandcenter.ui.theme.HevyntTheme

class MainActivity : ComponentActivity() {
    private val appViewModel by viewModels<HevyntAppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestRefreshRate()
        setContent {
            HevyntTheme {
                FitnessApp(appViewModel)
            }
        }
        handleWidgetIntent(intent)
    }

    private fun preferHighestRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentDisplay = display ?: return
            val bestMode = currentDisplay.supportedModes
                .filter {
                    it.physicalWidth == currentDisplay.mode.physicalWidth &&
                        it.physicalHeight == currentDisplay.mode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: return
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = bestMode.modeId
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        appViewModel.onAppResumed()
    }

    private fun handleWidgetIntent(intent: Intent) {
        val action = intent.action
        intent.action = null
        when (action) {
            ACTION_OPEN_WORKOUT -> appViewModel.openPrograms()
            ACTION_CONTINUE_WORKOUT -> appViewModel.resumeWorkout()
        }
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.yifeng.commandcenter.action.OPEN_WORKOUT"
        const val ACTION_CONTINUE_WORKOUT = "com.yifeng.commandcenter.action.CONTINUE_WORKOUT"
    }
}