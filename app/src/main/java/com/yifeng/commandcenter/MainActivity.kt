package com.yifeng.commandcenter

import android.content.Intent
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
        setContent {
            HevyntTheme {
                FitnessApp(appViewModel)
            }
        }
        handleWidgetIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent) {
        val action = intent.action
        intent.action = null
        when (action) {
            ACTION_OPEN_WORKOUT -> appViewModel.openPrograms()
            ACTION_START_ROUTINE -> intent.getStringExtra(EXTRA_ROUTINE_ID)
                ?.let(appViewModel::startRoutine)
            ACTION_CONTINUE_WORKOUT -> appViewModel.resumeWorkout()
        }
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.yifeng.commandcenter.action.OPEN_WORKOUT"
        const val ACTION_START_ROUTINE = "com.yifeng.commandcenter.action.START_ROUTINE"
        const val ACTION_CONTINUE_WORKOUT = "com.yifeng.commandcenter.action.CONTINUE_WORKOUT"
        const val EXTRA_ROUTINE_ID = "routine_id"
    }
}