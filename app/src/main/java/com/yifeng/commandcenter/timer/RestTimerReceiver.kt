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
            RestTimerManager.ACTION_COMPLETE -> RestTimerManager.complete(
                context,
                intent.getLongExtra(RestTimerManager.EXTRA_EXPECTED_END_MILLIS, 0L)
                    .takeIf { it > 0L },
            )
            RestTimerManager.ACTION_COMPLETE_SET -> RestTimerManager.completeSet(
                context = context,
                exerciseId = intent.getStringExtra(RestTimerManager.EXTRA_EXERCISE_ID),
                setId = intent.getStringExtra(RestTimerManager.EXTRA_SET_ID),
            )
        }
    }
}