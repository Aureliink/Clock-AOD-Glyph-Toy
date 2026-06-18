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
    const val KEY_CALL_ICON = "show_call_icon"
    const val KEY_VISUALIZER = "show_visualizer"
    const val KEY_VISUALIZER_STYLE = "visualizer_style"
    const val KEY_TIME_FORMAT_12H = "time_format_12h"
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)

        // Needed for the phone calling status
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 101)
        }

        // Needed for the music visualizer
        //val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        //startActivity(intent)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)

            fitsSystemWindows = true
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            isVerticalScrollBarEnabled = false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(56, 0, 56, 460)
        }

        // --- HEADER ---
        container.addView(TextView(this).apply {
            text = "Clock AOD Glyph toy"
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            setPadding(0, 160, 0, 8)
        })

        container.addView(TextView(this).apply {
            text = "MATRIX TOY AOD"
            textSize = 13f
            setTextColor(Color.GRAY)
            letterSpacing = 0.3f
        })

        // --- CARD : CLOCK SETTINGS ---
        val clockCard = createSettingsCard()
        val clockContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val formatSwitch = MaterialSwitch(this).apply {
            text = "Format 12h (AM/PM)"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setupColors()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            isChecked = sharedPrefs.getBoolean(Prefs.KEY_TIME_FORMAT_12H, false)

            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_TIME_FORMAT_12H, isChecked).apply()
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        // Icône d'horloge native Android
        val clockHeader = createHeaderWithIcon(android.R.drawable.ic_lock_idle_alarm, formatSwitch, 0f)

        val clockDescription = TextView(this).apply {
            text = "If enabled, switches to a 12h format clock"
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(96, 8, 0, 24)
        }

        clockContent.addView(clockHeader)
        clockContent.addView(clockDescription)
        clockCard.addView(clockContent)
        container.addView(clockCard)

        // --- CARD 1 : BATTERY ---
        val batteryCard = createSettingsCard()
        val batteryContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val batterySwitch = MaterialSwitch(this).apply {
            text = "Battery Status"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setupColors()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            // 1. On applique d'abord la valeur
            isChecked = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)

            // 2. On n'écoute le changement QU'APRÈS pour éviter le faux départ
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_BATTERY, isChecked).apply()
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        // Utilisation d'une icône de batterie plus moderne (éclat/bolt)
        val batteryHeader = createHeaderWithIcon(android.R.drawable.ic_lock_idle_charging, batterySwitch, 0f)

        val batteryDescription = TextView(this).apply {
            text = "Displays a gauge or a circle depending on the battery level"
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(96, 8, 0, 0) // Aligné avec le texte du switch (icon 64 + margin 32)
        }

        val styleLabel = TextView(this).apply {
            text = "VISUAL STYLE"
            setTextColor(Color.GRAY)
            textSize = 11f
            letterSpacing = 0.1f
            setPadding(96, 32, 0, 24)
        }

        val styleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(96, 0, 0, 0)
            val currentStyle = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring")
            val radioRing = createStyleRadio("Ring", currentStyle == "ring")
            val radioGauge = createStyleRadio("Gauge", currentStyle == "gauge")
            addView(radioRing)
            addView(radioGauge)

            // On n'attribue l'écouteur qu'après avoir ajouté les boutons pré-cochés
            setOnCheckedChangeListener { _, checkedId ->
                val selectedStyle = if (checkedId == radioRing.id) "ring" else "gauge"
                sharedPrefs.edit { putString(Prefs.KEY_BATTERY_STYLE, selectedStyle) }
                sendBroadcast(Intent(Prefs.ACTION_REFRESH))
            }
        }

        batteryContent.addView(batteryHeader)
        batteryContent.addView(batteryDescription)
        batteryContent.addView(styleLabel)
        batteryContent.addView(styleGroup)
        batteryCard.addView(batteryContent)
        container.addView(batteryCard)

        // --- CARD 2 : PHONE CALLS ---
        val callCard = createSettingsCard()
        val callContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val callSwitch = MaterialSwitch(this).apply {
            text = "Phone call status"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setupColors()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            isChecked = sharedPrefs.getBoolean(Prefs.KEY_CALL_ICON, true)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_CALL_ICON, isChecked).apply()
            }
        }

        // Icône téléphone avec rotation de -45 degrés pour pointer vers le haut à droite
        val callHeader = createHeaderWithIcon(android.R.drawable.sym_action_call, callSwitch, -90f)

        val callDescription = TextView(this).apply {
            text = "Displays an animated phone icon during a phone call"
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(96, 8, 0, 0)
        }

        callContent.addView(callHeader)
        callContent.addView(callDescription)
        callCard.addView(callContent)
        container.addView(callCard)

        // --- CARD 3 : MUSIC VISUALIZER ---
        val musicCard = createSettingsCard()
        val musicContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val musicSwitch = MaterialSwitch(this).apply {
            text = "Music Visualizer"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setupColors()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            isChecked = sharedPrefs.getBoolean(Prefs.KEY_VISUALIZER, true)

            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(Prefs.KEY_VISUALIZER, isChecked).apply()
                // On notifie pour couper instantanément l'affichage si on désactive en pleine lecture
                sendBroadcast(Intent(Prefs.ACTION_REFRESH).apply {
                    putExtra("is_visualizer", true)
                    putExtra("is_playing", false)
                })
            }
        }

        // Utilisation de l'icône multimédia standard (note de musique / play)
        val musicHeader = createHeaderWithIcon(android.R.drawable.ic_media_play, musicSwitch, 0f)

        val musicDescription = TextView(this).apply {
            text = "Displays an animated visualizer when music or media is playing"
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(96, 8, 0, 0)
        }

        val musicStyleLabel = TextView(this).apply {
            text = "VISUALIZER STYLE"
            setTextColor(Color.GRAY)
            textSize = 11f
            letterSpacing = 0.1f
            setPadding(96, 32, 0, 24)
        }

        val musicStyleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(96, 0, 0, 0)
            val currentMusicStyle = sharedPrefs.getString(Prefs.KEY_VISUALIZER_STYLE, "bars")
            val radioBars = createStyleRadio("Wave Bars", currentMusicStyle == "bars")
            val radioCD = createStyleRadio("Spinning CD", currentMusicStyle == "cd")
            addView(radioBars)
            addView(radioCD)

            setOnCheckedChangeListener { _, checkedId ->
                val selectedStyle = if (checkedId == radioBars.id) "bars" else "cd"
                sharedPrefs.edit { putString(Prefs.KEY_VISUALIZER_STYLE, selectedStyle) }
            }
        }

        musicContent.addView(musicHeader)
        musicContent.addView(musicDescription)
        musicContent.addView(musicStyleLabel)
        musicContent.addView(musicStyleGroup)
        musicCard.addView(musicContent)
        container.addView(musicCard)

        // --- ACTIONS CONTAINER ---
        val actionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
            setPadding(64, 32, 64, 100)
            setBackgroundColor(Color.BLACK)
        }

        val donateButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Support the project! (PayPal)"
            setTextColor(Color.WHITE)
            cornerRadius = 100
            setPadding(0, 40, 0, 40)
            strokeColor = ColorStateList.valueOf(Color.parseColor("#444444"))
            strokeWidth = 3

            // AJOUT : Rendre le bouton opaque
            backgroundTintList = ColorStateList.valueOf(Color.BLACK)
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.paypal.me/Mxiden"))) }
        }

        val openSettings = MaterialButton(this).apply {
            text = "Manage Glyph Toys"
            val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
            val btnColor = if (isColorTooDark(accent)) Color.parseColor("#EEEEEE") else accent
            setBackgroundColor(btnColor)
            setTextColor(getContrastColor(btnColor))
            cornerRadius = 100
            setPadding(0, 48, 0, 48)
            setOnClickListener {
                try { startActivity(Intent().apply { component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity") }) }
                catch (e: Exception) { Toast.makeText(this@MainActivity, "Toy Manager not found", Toast.LENGTH_SHORT).show() }
            }
        }

        actionsContainer.addView(donateButton)
        actionsContainer.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 24) })
        actionsContainer.addView(openSettings)

        scrollView.addView(container)
        root.addView(scrollView)
        root.addView(actionsContainer)
        setContentView(root)
    }

    private fun createHeaderWithIcon(iconRes: Int, switch: MaterialSwitch, rotation: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                imageTintList = ColorStateList.valueOf(Color.WHITE)
                this.rotation = rotation
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginEnd = 32 }
            }

            addView(icon)
            addView(switch)
        }
    }

    private fun createSettingsCard(): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 48, 0, 0) }
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(72f).build()
            strokeWidth = 3
            setStrokeColor(Color.parseColor("#222222"))
            setCardBackgroundColor(Color.parseColor("#111111"))
            setContentPadding(56, 56, 56, 56)
        }
    }

    private fun MaterialSwitch.setupColors() {
        val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        val finalAccent = if (isColorTooDark(accent)) Color.WHITE else accent
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(finalAccent, Color.DKGRAY))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(finalAccent.adjustAlpha(0.3f), Color.parseColor("#222222")))
    }

    private fun createStyleRadio(label: String, checked: Boolean): MaterialRadioButton {
        return MaterialRadioButton(this).apply {
            id = View.generateViewId()
            text = label
            setTextColor(Color.WHITE)
            val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
            val finalAccent = if (isColorTooDark(accent)) Color.WHITE else accent
            buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(finalAccent, Color.GRAY))
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