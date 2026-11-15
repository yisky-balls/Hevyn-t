package com.yifeng.commandcenter.glyph

import android.os.Build
import android.util.Log

/**
 * Checks whether the current device supports the Nothing Glyph Developer Kit
 * for Phone (4a) (model 25111).
 *
 * Uses [com.nothing.ketchum.Common.is25111] as well as fallback checks against
 * `Build.MODEL`, `Build.DEVICE`, and `Build.PRODUCT` to handle regional SKU
 * differences (e.g. A069, A069IN, Phone (4a), 25111).
 *
 * Wrapped in try/catch so non-Nothing devices never crash.
 */
object GlyphDeviceSupport {

    private const val TAG = "GlyphDeviceSupport"

    /**
     * `true` when running on a Nothing Phone (4a) with the Glyph SDK available.
     * Safe to call on any Android device.
     */
    fun isPhone4a(): Boolean = try {
        val sdkMatch = com.nothing.ketchum.Common.is25111()
        val model = Build.MODEL.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val product = Build.PRODUCT.orEmpty()

        sdkMatch ||
            model.contains("A069", ignoreCase = true) ||
            model.contains("Phone (4a)", ignoreCase = true) ||
            device.contains("25111") ||
            product.contains("25111")
    } catch (t: Throwable) {
        Log.d(TAG, "Glyph SDK not available: ${t.message}")
        false
    }

    /**
     * `true` when running on any supported Nothing device with the Glyph SDK.
     */
    fun isSupported(): Boolean = isPhone4a()
}
