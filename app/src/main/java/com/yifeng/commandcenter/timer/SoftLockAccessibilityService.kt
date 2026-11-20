package com.yifeng.commandcenter.timer

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Minimal AccessibilityService used exclusively to soft-lock the screen
 * (GLOBAL_ACTION_LOCK_SCREEN, preserving biometric unlock) when the rest timer finishes.
 *
 * It requests no window content access and listens to no accessibility events.
 */
class SoftLockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: this service only performs GLOBAL_ACTION_LOCK_SCREEN on demand.
    }

    override fun onInterrupt() {
        // No ongoing feedback to interrupt.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instanceRef: WeakReference<SoftLockAccessibilityService>? = null

        val isSupportedOnDevice: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        fun isRunning(): Boolean = instanceRef?.get() != null

        fun isEnabledInSettings(context: Context): Boolean {
            if (!isSupportedOnDevice) return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            if (enabledServices.isBlank()) return false
            val target = ComponentName(context, SoftLockAccessibilityService::class.java)
            return enabledServices.split(':').any { entry ->
                ComponentName.unflattenFromString(entry) == target
            }
        }

        /**
         * Turns off and locks the screen if the service is connected and the screen is currently on.
         * Preserves biometric unlock (fingerprint / face) unlike DevicePolicyManager.lockNow().
         */
        fun lockScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
            if (!powerManager.isInteractive) return false
            val service = instanceRef?.get() ?: return false
            return runCatching {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }.getOrDefault(false)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }
}
