package com.example.glyphclock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Toy AOD pour Glyph Matrix\nHeures en haut, minutes en bas"
            textSize = 16f
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Ouvrir le gestionnaire des Glyph Toys"
            setOnClickListener {
                try {
                    val intent = Intent().apply {
                        component = ComponentName(
                            "com.nothing.thirdparty",
                            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                        )
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(button)

        setContentView(container)
    }
}
