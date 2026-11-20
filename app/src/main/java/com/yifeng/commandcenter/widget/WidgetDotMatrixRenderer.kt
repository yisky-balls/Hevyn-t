package com.yifeng.commandcenter.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.yifeng.commandcenter.R
import java.io.File

object WidgetDotMatrixRenderer {
    @Volatile
    private var squareDotTypeface: Typeface? = null

    fun getTypeface(context: Context): Typeface {
        squareDotTypeface?.let { return it }
        synchronized(this) {
            squareDotTypeface?.let { return it }
            val tf = createTypeface(context)
            squareDotTypeface = tf
            return tf
        }
    }

    @SuppressLint("ResourceType")
    private fun createTypeface(context: Context): Typeface {
        // 1. Try to load bundled Doto font with ROND=0 (square matrix) axis
        try {
            val cacheFile = File(context.cacheDir, "doto_square.ttf")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.resources.openRawResource(R.font.doto_variable).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val customTf = Typeface.Builder(cacheFile)
                .setFontVariationSettings("'ROND' 0, 'wght' 700")
                .build()
            if (customTf != null) return customTf
        } catch (_: Throwable) {}

        // 2. Try ResourcesCompat
        try {
            val resTf = ResourcesCompat.getFont(context, R.font.doto_variable)
            if (resTf != null) return resTf
        } catch (_: Throwable) {}

        // 3. Try Nothing OS native system font
        try {
            val sysTf = Typeface.create("NDot57All", Typeface.BOLD)
            if (sysTf != null && sysTf != Typeface.DEFAULT) return sysTf
        } catch (_: Throwable) {}

        return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    fun renderText(
        context: Context,
        text: String,
        textSizeSp: Float = 26f,
        textColor: Int = 0xFFFFFFFF.toInt(),
        letterSpacing: Float = 0f,
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val density = metrics.density.coerceAtLeast(1f)
        val pxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, metrics)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getTypeface(context)
            textSize = pxSize
            color = textColor
            this.letterSpacing = letterSpacing
        }

        val fontMetrics = paint.fontMetrics
        val textWidth = paint.measureText(text)
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val bitmapWidth = (textWidth + 6 * density).toInt().coerceAtLeast(1)
        val bitmapHeight = (textHeight + 2 * density).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val y = -fontMetrics.ascent + (bitmapHeight - textHeight) / 2f
        canvas.drawText(text, 2 * density, y, paint)

        return bitmap
    }
}
