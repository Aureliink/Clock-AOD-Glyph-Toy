package com.example.glyphclock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Ajoute cet import
import android.widget.Switch
import android.content.Context

// Place-le ici
object Prefs {
    const val NAME = "glyph_clock_prefs"
    const val KEY_BATTERY = "show_battery"
    const val ACTION_REFRESH = "com.example.glyphclock.REFRESH_GLYPH" // Ajout ici
}

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(64, 80, 64, 80) // Plus de padding pour respirer
        }

        ViewCompat.setOnApplyWindowInsetsListener(container) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // On applique les marges système en plus de tes paddings (64, 80...)
            v.setPadding(insets.left + 64, insets.top + 64, insets.right + 64, insets.bottom + 64)
            WindowInsetsCompat.CONSUMED
        }

        // --- BLOC HAUT : Titres ---
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Toy AOD for Glyph Matrix"
            textSize = 16f
            alpha = 0.7f // Un peu plus discret
            gravity = Gravity.CENTER
        }

        // Espaceur qui pousse tout vers le bas
        val topSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        // --- BLOC MILIEU : Settings ---
        val settingsLabel = TextView(this).apply {
            text = "SETTINGS"
            textSize = 12f
            setPadding(0, 0, 0, 16)
        }

        val batterySwitch = Switch(this).apply {
            text = "Enable battery ring"
            isChecked = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_BATTERY, isChecked).apply()
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        // Espaceur qui pousse le bouton vers le bas
        val bottomSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        // --- BLOC BAS : Actions ---
        val button = Button(this).apply {
            text = "Open glyph toys settings"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.nothing.thirdparty",
                            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                        )
                    }
                    startActivity(intent)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // Assemblage
        container.addView(title)
        container.addView(subtitle)
        container.addView(topSpacer)     // Pousse vers le milieu
        container.addView(settingsLabel)
        container.addView(batterySwitch)
        container.addView(bottomSpacer)  // Pousse vers le bas
        container.addView(button)

        window.setDecorFitsSystemWindows(false)
        setContentView(container)
    }
}
