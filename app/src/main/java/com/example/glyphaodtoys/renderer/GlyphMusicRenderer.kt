package com.example.glyphclock.renderer

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.atan2
import kotlin.math.sqrt

object GlyphMusicRenderer {

    private const val MATRIX_SIZE = 25

    // Génération de l'anneau extérieur
    val circlePixels: List<Pair<Int, Int>> = generateCirclePixels()

    // Génération des pixels pour les ondes dynamiques
    private val wavePixels: List<Triple<Int, Int, Double>> = generateWavePixels()

    private fun generateCirclePixels(): List<Pair<Int, Int>> {
        val pixels = mutableListOf<Pair<Int, Int>>()
        val cx = 12
        val cy = 12
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                val distance = Math.round(sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble())).toInt()
                if (distance == 12) pixels.add(Pair(x, y))
            }
        }
        return pixels
    }

    private fun generateWavePixels(): List<Triple<Int, Int, Double>> {
        val pixels = mutableListOf<Triple<Int, Int, Double>>()
        val cx = 12
        val cy = 12
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                val distance = Math.round(sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble())).toInt()
                // Les deux ondes aux distances 6 et 8 (séparées par la distance 7, soit 1 pixel d'écart)
                if (distance == 7 || distance == 9) {
                    var angle = atan2((y - cy).toDouble(), (x - cx).toDouble())
                    // Normalisation de l'angle entre 0 et 2π
                    if (angle < 0) angle += 2 * Math.PI
                    pixels.add(Triple(x, y, angle))
                }
            }
        }
        return pixels
    }

    // --- DESIGN 1 : WAVE BARS ---
    fun renderVisualizer(data: ByteArray): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        for (x in 0 until MATRIX_SIZE) {
            val sampleIdx = (x * (data.size / MATRIX_SIZE)).coerceIn(0, data.size - 1)
            val rawValue = Math.abs(data[sampleIdx].toInt())
            val barHeight = (rawValue * 18 / 128).coerceIn(1, 18)

            val yCenter = 12
            val yStart = yCenter - (barHeight / 2)
            val yEnd = yCenter + (barHeight / 2)

            for (py in yStart..yEnd) {
                if (py in 0 until MATRIX_SIZE) {
                    bitmap.setPixel(x, py, Color.WHITE)
                }
            }
        }
        return bitmap
    }

    // --- DESIGN 2 : SPINNING CD ---
    fun renderSpinningCD(step: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)

        // 1. Contour externe du CD
        for (pos in circlePixels) {
            bitmap.setPixel(pos.first, pos.second, Color.WHITE)
        }

        // 2. Trou central optimisé (Ton design exact avec le centre à 12,12 évidé)
        val centerCircle = intArrayOf(
            11,10, 12,10, 13,10,
            10,11, 11,11, 12,11, 13,11, 14,11,
            10,12, 11,12,        13,12, 14,12,
            10,13, 11,13, 12,13, 13,13, 14,13,
            11,14, 12,14, 13,14
        )
        for (i in centerCircle.indices step 2) {
            bitmap.setPixel(centerCircle[i], centerCircle[i+1], Color.WHITE)
        }

        // 3. Les doubles ondes symétriques qui tournent
        // Vitesse de rotation : on avance de 0.1 radian par frame (~1 tour complet toutes les 2 secondes à 30fps)
        val timeAngle = (step * 0.1) % (2 * Math.PI)
        val arcSpan = 0.6 // Ouverture de l'arc (environ 68 degrés, modifiable si tu veux des ondes plus longues)

        for (pixel in wavePixels) {
            val angle = pixel.third

            // Différence d'angle avec l'onde 1 (axe direct)
            val diff1 = Math.abs(angle - timeAngle)
            // Différence d'angle avec l'onde 2 (axe opposé à 180°)
            val diff2 = Math.abs(angle - (timeAngle + Math.PI) % (2 * Math.PI))

            // Gestion de la circularité (pour que l'arc ne disparaisse pas en passant le cap des 360°)
            val minDiff1 = Math.min(diff1, 2 * Math.PI - diff1)
            val minDiff2 = Math.min(diff2, 2 * Math.PI - diff2)

            // Si le pixel se trouve dans la zone d'une des deux ondes, on l'allume
            if (minDiff1 < arcSpan || minDiff2 < arcSpan) {
                bitmap.setPixel(pixel.first, pixel.second, Color.WHITE)
            }
        }

        return bitmap
    }
}