package com.yifeng.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yifeng.commandcenter.ui.FitnessApp
import com.yifeng.commandcenter.ui.theme.CommandcenterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CommandcenterTheme {
                FitnessApp()
            }
        }
    }
}