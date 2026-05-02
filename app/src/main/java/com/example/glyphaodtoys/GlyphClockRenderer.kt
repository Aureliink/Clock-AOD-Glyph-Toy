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

        // 1. On scanne toute la matrice et on garde UNIQUEMENT les pixels
        // dont la distance au centre est de 12.
        // Cela crée un anneau continu de 1 pixel d'épaisseur, remplissant les trous en haut et en bas.
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

        // 2. On trie ces pixels pour qu'ils tournent dans le sens des aiguilles d'une montre
        pixels.sortBy { pair ->
            val dx = pair.first - cx
            val dy = pair.second - cy

            // L'astuce atan2 : en passant (dx, -dy), on force 12h00 à être l'angle 0.
            var angle = atan2(dx.toDouble(), -dy.toDouble())
            if (angle < 0) {
                angle += 2 * Math.PI
            }
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

        drawBatteryCircle(bitmap, batteryPct)

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
                    val x = startX + col
                    val y = startY + row
                    bitmap.setPixel(x, y, Color.WHITE)
                }
            }
        }
    }

    private fun drawBatteryCircle(bitmap: Bitmap, batteryPct: Int) {
        // On calcule le nombre de pixels qui doivent être éteints
        val emptyCount = circlePixels.size - ((circlePixels.size * batteryPct) / 100)

        for ((index, pos) in circlePixels.withIndex()) {
            // Les premiers pixels sont gris (le vide avance), les suivants sont blancs
            val color = if (index < emptyCount) Color.rgb(30, 30, 30) else Color.WHITE
            bitmap.setPixel(pos.first, pos.second, color)
        }
    }

    // 1. À ajouter dans GlyphClockRenderer.kt
    fun renderNormalIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        drawBatteryCircle(bitmap, batteryPct)

        // Base du haut-parleur
        for (y in 10..14) {
            for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        }

        // Cône du haut-parleur
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        // Petite onde
        bitmap.setPixel(15, 10, Color.WHITE)
        bitmap.setPixel(16, 11, Color.WHITE)
        bitmap.setPixel(16, 12, Color.WHITE)
        bitmap.setPixel(16, 13, Color.WHITE)
        bitmap.setPixel(15, 14, Color.WHITE)

        // Grande onde
        bitmap.setPixel(18, 7, Color.WHITE)
        bitmap.setPixel(19, 8, Color.WHITE)
        for (y in 9..15) bitmap.setPixel(20, y, Color.WHITE)
        bitmap.setPixel(19, 16, Color.WHITE)
        bitmap.setPixel(18, 17, Color.WHITE)

        return bitmap
    }

    fun renderVibrateIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        drawBatteryCircle(bitmap, batteryPct)

        // Téléphone plus large
        for (y in 5..19) {
            bitmap.setPixel(9, y, Color.WHITE)
            bitmap.setPixel(15, y, Color.WHITE)
        }
        for (x in 10..14) {
            bitmap.setPixel(x, 5, Color.WHITE)
            bitmap.setPixel(x, 19, Color.WHITE)
        }

        // Éclairs/Vibrations à gauche
        bitmap.setPixel(6, 9, Color.WHITE); bitmap.setPixel(5, 10, Color.WHITE); bitmap.setPixel(7, 11, Color.WHITE)
        bitmap.setPixel(5, 12, Color.WHITE); bitmap.setPixel(7, 13, Color.WHITE); bitmap.setPixel(5, 14, Color.WHITE)
        bitmap.setPixel(6, 15, Color.WHITE)

        // Éclairs/Vibrations à droite
        bitmap.setPixel(18, 9, Color.WHITE); bitmap.setPixel(19, 10, Color.WHITE); bitmap.setPixel(17, 11, Color.WHITE)
        bitmap.setPixel(19, 12, Color.WHITE); bitmap.setPixel(17, 13, Color.WHITE); bitmap.setPixel(19, 14, Color.WHITE)
        bitmap.setPixel(18, 15, Color.WHITE)

        return bitmap
    }

    fun renderSilentIcon(batteryPct: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        clear(bitmap)
        drawBatteryCircle(bitmap, batteryPct)

        // Base du haut-parleur (aligné avec renderNormalIcon)
        for (y in 10..14) {
            for (x in 5..8) bitmap.setPixel(x, y, Color.WHITE)
        }

        // Cône du haut-parleur
        for (i in 0..4) {
            val x = 9 + i
            for (y in (9 - i)..(15 + i)) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }


        // Grande barre oblique (Haut-parleur barré)
        for (i in 4..20) {
            bitmap.setPixel(i, 24 - i, Color.WHITE)
            bitmap.setPixel(i + 1, 24 - i, Color.WHITE)
        }

        return bitmap
    }
}