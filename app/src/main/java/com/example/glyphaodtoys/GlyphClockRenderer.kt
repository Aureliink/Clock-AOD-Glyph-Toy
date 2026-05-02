package com.example.glyphclock

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.atan2
import kotlin.math.sqrt

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

    private val circlePixels: List<Pair<Int, Int>> = generateCirclePixels()

    private fun generateCirclePixels(): List<Pair<Int, Int>> {
        val pixels = mutableListOf<Pair<Int, Int>>()
        val cx = 12
        val cy = 12

        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                val dx = x - cx
                val dy = y - cy
                val distance = Math.round(sqrt((dx * dx + dy * dy).toDouble())).toInt()

                if (distance == 12) {
                    pixels.add(Pair(x, y))
                }
            }
        }

        pixels.sortBy { pair ->
            val dx = pair.first - cx
            val dy = pair.second - cy
            var angle = atan2(dx.toDouble(), -dy.toDouble())
            if (angle < 0) angle += 2 * Math.PI
            angle
        }
        return pixels
    }

    fun render25x25(hours: String, minutes: String, batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)

        val top = hours.padStart(2, '0').takeLast(2)
        val bottom = minutes.padStart(2, '0').takeLast(2)

        drawCenteredTwoDigits(bitmap, top[0], top[1], y = 4)
        drawCenteredTwoDigits(bitmap, bottom[0], bottom[1], y = 14)

        // On ne dessine le cercle que si batteryPct est différent de -1
        if (batteryPct != -1) {
            drawBatteryCircle(bitmap, batteryPct)
        }

        return bitmap
    }

    private fun clear(bitmap: Bitmap) {
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                bitmap.setPixel(x, y, Color.BLACK)
            }
        }
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
                if (line[col] == '1') {
                    bitmap.setPixel(startX + col, startY + row, Color.WHITE)
                }
            }
        }
    }

    private fun drawBatteryCircle(bitmap: Bitmap, batteryPct: Int) {
        // Sécurité pour batteryPct
        val pct = batteryPct.coerceIn(0, 100)
        val emptyCount = circlePixels.size - ((circlePixels.size * pct) / 100)

        for ((index, pos) in circlePixels.withIndex()) {
            val color = if (index < emptyCount) Color.rgb(30, 30, 30) else Color.WHITE
            bitmap.setPixel(pos.first, pos.second, color)
        }
    }

    // --- ICONES DE MODE SONORE ---

    fun renderNormalIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        if (batteryPct != -1) drawBatteryCircle(bitmap, batteryPct)

        for (y in 10..14) for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) bitmap.setPixel(x, y, Color.WHITE)
        }
        // Ondes
        intArrayOf(15,10, 16,11, 16,12, 16,13, 15,14).let { for(i in it.indices step 2) bitmap.setPixel(it[i], it[i+1], Color.WHITE) }
        return bitmap
    }

    fun renderVibrateIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        if (batteryPct != -1) drawBatteryCircle(bitmap, batteryPct)

        for (y in 5..19) { bitmap.setPixel(9, y, Color.WHITE); bitmap.setPixel(15, y, Color.WHITE) }
        for (x in 10..14) { bitmap.setPixel(x, 5, Color.WHITE); bitmap.setPixel(x, 19, Color.WHITE) }
        // Vibrations (simplifié)
        val v = intArrayOf(6,9, 5,10, 7,11, 5,12, 7,13, 5,14, 6,15, 18,9, 19,10, 17,11, 19,12, 17,13, 19,14, 18,15)
        for(i in v.indices step 2) bitmap.setPixel(v[i], v[i+1], Color.WHITE)
        return bitmap
    }

    fun renderSilentIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        if (batteryPct != -1) drawBatteryCircle(bitmap, batteryPct)

        for (y in 10..14) for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) bitmap.setPixel(x, y, Color.WHITE)
        }
        // Barre oblique
        for (i in 4..20) {
            if (i < MATRIX_SIZE && (24-i) < MATRIX_SIZE) {
                bitmap.setPixel(i, 24 - i, Color.WHITE)
                if (i+1 < MATRIX_SIZE) bitmap.setPixel(i + 1, 24 - i, Color.WHITE)
            }
        }
        return bitmap
    }
}