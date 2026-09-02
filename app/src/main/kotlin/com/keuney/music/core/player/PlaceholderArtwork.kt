package com.keuney.music.core.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.ByteArrayOutputStream

internal fun createPlaceholderArtwork(): ByteArray {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(48, 40, 70))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 222, 248) }
        val symbol = Path().apply {
            moveTo(100f, 72f)
            lineTo(184f, 128f)
            lineTo(100f, 184f)
            close()
        }
        canvas.drawPath(symbol, paint)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
