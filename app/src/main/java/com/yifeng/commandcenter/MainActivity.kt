package com.yifeng.commandcenter

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
    }
}