package com.yifeng.commandcenter.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RestTimerManager.ACTION_SKIP -> RestTimerManager.skip(context)
            RestTimerManager.ACTION_SUBTRACT -> RestTimerManager.adjust(context, -15)
            RestTimerManager.ACTION_ADD -> RestTimerManager.adjust(context, 15)
            RestTimerManager.ACTION_COMPLETE -> RestTimerManager.complete(context)
            RestTimerManager.ACTION_COMPLETE_SET -> RestTimerManager.completeSet(
                context = context,
                exerciseId = intent.getStringExtra(RestTimerManager.EXTRA_EXERCISE_ID),
                setId = intent.getStringExtra(RestTimerManager.EXTRA_SET_ID),
            )
        }
    }
}