package com.example.glyphclock.renderer

import android.graphics.Bitmap
import android.graphics.Color

object GlyphCallRenderer {
    private const val MATRIX_SIZE = 25

    fun renderPhoneIcon(waveStep: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)

        // --- LE CORPS DU TÉLÉPHONE (Fixe) ---
        val phoneShape = intArrayOf(
            6,3, 7,3, 8,3,
            5,4, 6,4, 7,4, 8,4,
            4,5, 5,5, 6,5, 7,5, 8,5,
            4,6, 5,6, 6,6, 7,6, 8,6, 9,6,
            4,7, 5,7, 6,7, 7,7, 8,7, 9,7,
            4,8, 5,8, 6,8, 7,8, 8,8, 9,8,
            4,9, 5,9, 6,9, 7,9, 8,9,
            4,10, 5,10, 6,10, 7,10,
            5,11, 6,11, 7,11,
            5,12, 6,12, 7,12, 8,12,
            6,13, 7,13, 8,13, 9,13,
            6,14, 7,14, 8,14, 9,14,
            7,15, 8,15, 9,15, 10,15,
            8,16, 9,16, 10,16, 11,16, 14,16, 15,16, 16,16, 17,16,
            8,17, 9,17, 10,17, 11,17, 12,17, 13,17, 14,17, 15,17, 16,17, 17,17, 18,17,
            9,18, 10,18, 11,18, 12,18, 13,18, 14,18, 15,18, 16,18, 17,18, 18,18, 19,18,
            10,19, 11,19, 12,19, 13,19, 14,19, 15,19, 16,19, 17,19, 18,19,
            11,20, 12,20, 13,20, 14,20, 15,20, 16,20, 17,20, 18,20,
            13,21, 14,21, 15,21, 16,21, 17,21
        )

        for (i in phoneShape.indices step 2) {
            val x = phoneShape[i]
            val y = phoneShape[i+1]
            if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        // --- LOGIQUE D'ÉMISSION (Chaque étape a son onde exclusive) ---

        // Cycle de 4 étapes : 0 (vide), 1 (proche), 2 (milieu), 3 (loin)
        val currentStep = waveStep % 4

        val wave0 = intArrayOf(13,7, 14,7, 15,8, 15,9, 16,9, 16,10, 16,11, 16,12)
        val wave1 = intArrayOf(13,4, 14,4, 15,5, 16,5, 16,6, 17,7, 18,8, 18,9, 18,10, 18,11, 18,12)
        val wave2 = intArrayOf(15,2, 16,2, 17,3, 18,4, 19,5, 20,6, 20,7, 20,8, 20,9, 21,9, 21,10, 21,11, 21,12)

        when (currentStep) {
            1 -> {
                // Étape 1 : L'onde part du combiné
                for (i in wave0.indices step 2) bitmap.setPixel(wave0[i], wave0[i+1], Color.WHITE)
            }
            2 -> {
                // Étape 2 : Elle se propage au milieu (la première s'éteint)
                for (i in wave1.indices step 2) bitmap.setPixel(wave1[i], wave1[i+1], Color.WHITE)
            }
            3 -> {
                // Étape 3 : Elle s'éloigne au maximum
                for (i in wave2.indices step 2) bitmap.setPixel(wave2[i], wave2[i+1], Color.WHITE)
            }
            // Étape 0 : Rien ne s'affiche, l'onde s'est dissipée dans le vide avant le prochain tir
        }

        return bitmap
    }
}