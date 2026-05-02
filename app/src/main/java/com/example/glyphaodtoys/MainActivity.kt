package com.example.glyphclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton

object Prefs {
    const val NAME = "glyph_clock_prefs"
    const val KEY_BATTERY = "show_battery"
    const val KEY_BATTERY_STYLE = "battery_style"
    const val ACTION_REFRESH = "com.example.glyphclock.REFRESH_GLYPH"
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = Color.TRANSPARENT

        val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(56, 0, 56, 0)
        }

        // --- HEADER ---
        val title = TextView(this).apply {
            text = "Glyph Clock"
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            setPadding(0, 160, 0, 8)
        }

        val subtitle = TextView(this).apply {
            text = "MATRIX TOY AOD"
            textSize = 13f
            setTextColor(Color.GRAY)
            letterSpacing = 0.3f
        }

        // --- SETTINGS CARD ---
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 100, 0, 0) }

            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(80f)
                .build()

            strokeWidth = 4
            setStrokeColor(Color.parseColor("#444444"))
            setCardBackgroundColor(Color.parseColor("#1A1A1A"))
            setContentPadding(64, 64, 64, 64)
        }

        val cardContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Battery Switch
        val batterySwitch = MaterialSwitch(this).apply {
            text = "Battery Status"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
            val finalAccent = if (isColorTooDark(accent)) Color.WHITE else accent

            thumbTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(finalAccent, Color.DKGRAY)
            )

            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(finalAccent.adjustAlpha(0.4f), Color.parseColor("#333333"))
            )

            isChecked = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_BATTERY, isChecked).apply()
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 48, 0, 48)
            }
            setBackgroundColor(Color.parseColor("#222222"))
        }

        val styleLabel = TextView(this).apply {
            text = "VISUAL STYLE"
            setTextColor(Color.GRAY)
            textSize = 11f
            letterSpacing = 0.1f
            setPadding(0, 0, 0, 32)
        }

        // --- RADIO GROUP ---
        val styleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val currentStyle = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring")

            val radioRing = createStyleRadio("Ring", currentStyle == "ring")
            val radioGauge = createStyleRadio("Gauge", currentStyle == "gauge")

            addView(radioRing)
            addView(radioGauge)

            setOnCheckedChangeListener { _, checkedId ->
                val selectedStyle = if (checkedId == radioRing.id) "ring" else "gauge"
                sharedPrefs.edit { putString(Prefs.KEY_BATTERY_STYLE, selectedStyle) }
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        cardContent.addView(batterySwitch)
        cardContent.addView(divider)
        cardContent.addView(styleLabel)
        cardContent.addView(styleGroup)
        card.addView(cardContent)

        // --- ACTIONS ---
        val actionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply { setMargins(64, 0, 64, 100) }
        }

        val openSettings = MaterialButton(this).apply {
            text = "Manage Glyph Toys"
            val systemAccent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
            val buttonBgColor = if (isColorTooDark(systemAccent)) Color.parseColor("#EEEEEE") else systemAccent

            setBackgroundColor(buttonBgColor)
            setTextColor(getContrastColor(buttonBgColor))
            cornerRadius = 100
            setPadding(0, 48, 0, 48)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            setOnClickListener {
                try {
                    val intent = Intent().apply {
                        component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Toy Manager not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- BOUTON PAYPAL ---
        val donateButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Support the project! (PayPal)"
            setTextColor(Color.WHITE)

            cornerRadius = 100
            setPadding(0, 40, 0, 40)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            strokeColor = ColorStateList.valueOf(Color.parseColor("#444444"))
            strokeWidth = 3

            setOnClickListener {
                val paypalUrl = "https://www.paypal.me/Mxiden"
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paypalUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
                }
            }
        }

        actionsContainer.addView(donateButton)
        actionsContainer.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 24) })
        actionsContainer.addView(openSettings)

        container.addView(title)
        container.addView(subtitle)
        container.addView(card)
        root.addView(container)
        root.addView(actionsContainer)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setContentView(root)
    }

    private fun createStyleRadio(label: String, checked: Boolean): MaterialRadioButton {
        return MaterialRadioButton(this).apply {
            id = View.generateViewId()
            text = label
            setTextColor(Color.WHITE)
            textSize = 15f
            val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
            val finalAccent = if (isColorTooDark(accent)) Color.WHITE else accent

            buttonTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(finalAccent, Color.GRAY)
            )
            isChecked = checked
            layoutParams = RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun getContrastColor(color: Int): Int {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (darkness < 0.5) Color.BLACK else Color.WHITE
    }

    private fun isColorTooDark(color: Int): Boolean {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return luminance < 0.2
    }

    private fun Int.adjustAlpha(factor: Float): Int {
        return Color.argb((Color.alpha(this) * factor).toInt(), Color.red(this), Color.green(this), Color.blue(this))
    }
}