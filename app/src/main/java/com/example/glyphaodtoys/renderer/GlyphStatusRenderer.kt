package com.example.glyphclock.renderer

import android.graphics.Bitmap
import android.graphics.Color

object GlyphStatusRenderer {
    private const val MATRIX_SIZE = 25

    fun renderNormalIcon(): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until MATRIX_SIZE) for (x in 0 until MATRIX_SIZE) bitmap.setPixel(x, y, Color.BLACK)

        for (y in 10..14) for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) bitmap.setPixel(x, y, Color.WHITE)
        }

        val wave1 = intArrayOf(15,10, 16,11, 16,12, 16,13, 15,14)
        for(i in wave1.indices step 2) bitmap.setPixel(wave1[i], wave1[i+1], Color.WHITE)

        val wave2 = intArrayOf(18,8, 19,9, 20,10, 20,11, 20,12, 20,13, 20,14, 19,15, 18,16)
        for(i in wave2.indices step 2) {
            val x = wave2[i]
            val y = wave2[i+1]
            if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) bitmap.setPixel(x, y, Color.WHITE)
        }
        return bitmap
    }



    fun renderVibrateIcon(): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until MATRIX_SIZE) for (x in 0 until MATRIX_SIZE) bitmap.setPixel(x, y, Color.BLACK)

        for (y in 5..19) { bitmap.setPixel(9, y, Color.WHITE); bitmap.setPixel(15, y, Color.WHITE) }
        for (x in 10..14) { bitmap.setPixel(x, 5, Color.WHITE); bitmap.setPixel(x, 19, Color.WHITE) }

        // Nouvelles vibrations (barres verticales épurées demandées)
        for (y in 8..16) bitmap.setPixel(7, y, Color.WHITE)
        for (y in 10..14) bitmap.setPixel(5, y, Color.WHITE)
        for (y in 8..16) bitmap.setPixel(17, y, Color.WHITE)
        for (y in 10..14) bitmap.setPixel(19, y, Color.WHITE)

        return bitmap
    }

    fun renderSilentIcon(): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until MATRIX_SIZE) for (x in 0 until MATRIX_SIZE) bitmap.setPixel(x, y, Color.BLACK)

        for (y in 10..14) for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) bitmap.setPixel(x, y, Color.WHITE)
        }
        for (i in 4..20) {
            if (i < MATRIX_SIZE && (24-i) < MATRIX_SIZE) {
                bitmap.setPixel(i, 24 - i, Color.WHITE)
                if (i+1 < MATRIX_SIZE) bitmap.setPixel(i + 1, 24 - i, Color.WHITE)
            }
        }
        return bitmap
    }
}