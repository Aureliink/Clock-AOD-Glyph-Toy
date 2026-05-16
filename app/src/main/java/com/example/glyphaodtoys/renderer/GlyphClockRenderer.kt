package com.example.glyphclock.renderer

import android.graphics.Bitmap
import android.graphics.Color

object GlyphClockRenderer {

    private const val MATRIX_SIZE = 25
    private const val DIGIT_WIDTH = 5
    private const val DIGIT_HEIGHT = 7
    private const val DIGIT_SPACING = 1

    private val digits = mapOf(
        '0' to arrayOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to arrayOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to arrayOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to arrayOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
        '4' to arrayOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to arrayOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
        '6' to arrayOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
        '7' to arrayOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to arrayOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to arrayOf("01110", "10001", "10001", "01111", "00001", "00001", "01110")
    )

    /**
     * Rendu de base de l'horloge seule (sans batterie, gérée par le décorateur)
     */
    fun render25x25(hours: String, minutes: String, style: String = "ring"): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)

        val (hourY, minuteY) = if (style == "gauge") Pair(3, 15) else Pair(4, 14)

        val top = hours.padStart(2, '0').takeLast(2)
        val bottom = minutes.padStart(2, '0').takeLast(2)

        drawCenteredTwoDigits(bitmap, top[0], top[1], y = hourY)
        drawCenteredTwoDigits(bitmap, bottom[0], bottom[1], y = minuteY)

        return bitmap
    }

    /**
     * Utilisé pour l'effet de défilement vertical (scroll)
     */
    /**
     * Utilisé pour l'effet de défilement vertical (scroll)
     */
    fun renderDigitWithOffset(bitmap: Bitmap, digit: Char, x: Int, y: Int, offsetY: Int) {
        val glyph = digits[digit] ?: return
        for (row in 0 until DIGIT_HEIGHT) {
            val targetY = y + row + offsetY

            // On vérifie juste qu'on reste dans les limites physiques du Bitmap (0 à 24)
            if (targetY in 0 until MATRIX_SIZE) {
                val line = glyph[row]
                for (col in 0 until DIGIT_WIDTH) {
                    val targetX = x + col
                    if (targetX in 0 until MATRIX_SIZE && line[col] == '1') {
                        bitmap.setPixel(targetX, targetY, Color.WHITE)
                    }
                }
            }
        }
    }

    private fun clear(bitmap: Bitmap) {
        for (y in 0 until MATRIX_SIZE) for (x in 0 until MATRIX_SIZE) bitmap.setPixel(x, y, Color.BLACK)
    }

    private fun drawCenteredTwoDigits(bitmap: Bitmap, left: Char, right: Char, y: Int) {
        val totalWidth = (DIGIT_WIDTH * 2) + DIGIT_SPACING
        val startX = (MATRIX_SIZE - totalWidth) / 2
        drawDigit(bitmap, left, startX, y)
        drawDigit(bitmap, right, startX + DIGIT_WIDTH + DIGIT_SPACING, y)
    }

    private fun drawDigit(bitmap: Bitmap, digit: Char, startX: Int, startY: Int) {
        val glyph = digits[digit] ?: return
        for (row in 0 until DIGIT_HEIGHT) {
            val line = glyph[row]
            for (col in 0 until DIGIT_WIDTH) {
                if (line[col] == '1') bitmap.setPixel(startX + col, startY + row, Color.WHITE)
            }
        }
    }
}