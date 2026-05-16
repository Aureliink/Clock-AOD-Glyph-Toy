package com.example.glyphclock.renderer

import android.graphics.Bitmap
import android.graphics.Color

object GlyphBatteryRenderer {

    private const val MATRIX_SIZE = 25

    private val circlePixels = listOf(
        Pair(12, 0), Pair(13, 0), Pair(14, 0), Pair(15, 0), Pair(16, 1), Pair(17, 1), Pair(18, 2),
        Pair(19, 2), Pair(20, 3), Pair(21, 4), Pair(22, 5), Pair(22, 6), Pair(23, 7), Pair(23, 8),
        Pair(24, 9), Pair(24, 10), Pair(24, 11), Pair(24, 12), Pair(24, 13), Pair(24, 14), Pair(24, 15),
        Pair(23, 16), Pair(23, 17), Pair(22, 18), Pair(22, 19), Pair(21, 20), Pair(20, 21), Pair(19, 22),
        Pair(18, 22), Pair(17, 23), Pair(16, 23), Pair(15, 24), Pair(14, 24), Pair(13, 24), Pair(12, 24),
        Pair(11, 24), Pair(10, 24), Pair(9, 24), Pair(8, 23), Pair(7, 23), Pair(6, 22), Pair(5, 22),
        Pair(4, 21), Pair(3, 20), Pair(2, 19), Pair(2, 18), Pair(1, 17), Pair(1, 16), Pair(0, 15),
        Pair(0, 14), Pair(0, 13), Pair(0, 12), Pair(0, 11), Pair(0, 10), Pair(0, 9), Pair(1, 8),
        Pair(1, 7), Pair(2, 6), Pair(2, 5), Pair(3, 4), Pair(4, 3), Pair(5, 2), Pair(6, 2),
        Pair(7, 1), Pair(8, 1), Pair(9, 0), Pair(10, 0), Pair(11, 0)
    )

    val circlePixelsCount: Int = circlePixels.size

    fun drawBattery(bitmap: Bitmap, batteryPct: Int, style: String) {
        if (batteryPct == -1) return
        if (style == "gauge") {
            drawBatteryGauge(bitmap, batteryPct)
        } else {
            drawBatteryCircle(bitmap, batteryPct)
        }
    }

    private fun drawBatteryGauge(bitmap: Bitmap, batteryPct: Int) {
        val y = 12
        val startX = 0
        val endX = 24
        val width = endX - startX + 1
        val pct = batteryPct.coerceIn(0, 100)
        val filledPixels = Math.round((width * pct) / 100f)

        for (i in 0 until width) {
            val x = startX + i
            if (x in 0 until MATRIX_SIZE) {
                // On dessine systématiquement : soit Blanc, soit Gris.
                // Le clipping dans le Service s'occupera de la priorité des chiffres.
                val color = if (i < filledPixels) Color.WHITE else Color.rgb(30, 30, 30)
                bitmap.setPixel(x, y, color)
            }
        }
    }

    private fun drawBatteryCircle(bitmap: Bitmap, batteryPct: Int) {
        val pct = batteryPct.coerceIn(0, 100)
        val totalPixels = circlePixels.size
        val filledCount = (totalPixels * pct) / 100

        for (index in 0 until totalPixels) {
            val pos = circlePixels[index]
            // Cette fonction est déjà parfaite, elle garantit le dessin complet du cercle.
            val color = if (index >= (totalPixels - filledCount)) {
                Color.WHITE
            } else {
                Color.rgb(30, 30, 30)
            }
            bitmap.setPixel(pos.first, pos.second, color)
        }
    }
}