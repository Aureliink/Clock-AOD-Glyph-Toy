package com.example.glyphclock

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import java.time.LocalTime
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioManager
import android.graphics.Bitmap

class ClockToyService : Service() {

    private var isShowingIcon = false
    private var isGlyphConnected = false
    private val resetHandler = Handler(Looper.getMainLooper())
    private var glyphMatrixManager: GlyphMatrixManager? = null

    // Gère le changement de mode sonore (Vibreur, Sonnerie, Silence)
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION && context != null) {
                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

                // On récupère les préférences pour savoir quel style de batterie afficher sur l'icône
                val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                val showBattery = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)

                val batteryPct = if (showBattery) {
                    val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                    bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                } else -1

                // Affichage temporaire selon le mode actuel
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE -> showIconTemporarily(GlyphClockRenderer.renderVibrateIcon(batteryPct))
                    AudioManager.RINGER_MODE_SILENT -> showIconTemporarily(GlyphClockRenderer.renderSilentIcon(batteryPct))
                    AudioManager.RINGER_MODE_NORMAL -> showIconTemporarily(GlyphClockRenderer.renderNormalIcon(batteryPct))
                }
            }
        }
    }

    // Rafraîchit les Glyphs immédiatement quand on change un réglage dans l'appli
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Prefs.ACTION_REFRESH) {
                renderCurrentTime()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
        registerReceiver(refreshReceiver, IntentFilter(Prefs.ACTION_REFRESH), Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ringerModeReceiver)
        unregisterReceiver(refreshReceiver)
    }

    private fun renderCurrentTime() {
        if (isShowingIcon || !isGlyphConnected) return

        // 1. Lecture des préférences
        val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val showBattery = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"

        // 2. Préparation des données
        val now = LocalTime.now()
        val hours = now.hour.toString().padStart(2, '0')
        val minutes = now.minute.toString().padStart(2, '0')

        val batteryPct = if (showBattery) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else -1

        // 3. Rendu du Bitmap avec le style choisi (Ring ou Gauge)
        val bitmap = GlyphClockRenderer.render25x25(hours, minutes, batteryPct, style)

        // 4. Envoi à la matrice
        updateGlyphMatrix(bitmap)
    }

    private fun showIconTemporarily(bitmap: Bitmap) {
        if (!isGlyphConnected) return
        isShowingIcon = true

        updateGlyphMatrix(bitmap)

        resetHandler.removeCallbacksAndMessages(null)
        resetHandler.postDelayed({
            isShowingIcon = false
            renderCurrentTime()
        }, 3000)
    }

    private fun updateGlyphMatrix(bitmap: Bitmap) {
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .setPosition(0, 0)
            .setScale(100)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    // --- CALLBACKS & SDK GLYPH ---

    private val glyphManagerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManager?.register(Glyph.DEVICE_23112)
            isGlyphConnected = true
            renderCurrentTime()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isGlyphConnected = false
        }
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val event = extractToyEvent(msg.data)
                if (event == GlyphToy.EVENT_AOD || event == GlyphToy.EVENT_CHANGE) {
                    renderCurrentTime()
                }
            }
        }
    }

    private val messenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder {
        glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager?.init(glyphManagerCallback)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    private fun extractToyEvent(bundle: Bundle?): String? {
        return bundle?.getString(GlyphToy.MSG_GLYPH_TOY_DATA) ?: bundle?.getString("data")
    }
}