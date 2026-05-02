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

    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION && context != null) {
                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

                // On récupère la batterie au moment du changement
                val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                // 2. À modifier dans ClockToyService.kt (dans ton ringerModeReceiver)
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE -> showIconTemporarily(GlyphClockRenderer.renderVibrateIcon(batteryPct))
                    AudioManager.RINGER_MODE_SILENT -> showIconTemporarily(GlyphClockRenderer.renderSilentIcon(batteryPct))
                    AudioManager.RINGER_MODE_NORMAL -> showIconTemporarily(GlyphClockRenderer.renderNormalIcon(batteryPct))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ringerModeReceiver)
    }

    private fun showIconTemporarily(bitmap: Bitmap) {
        if (!isGlyphConnected) return
        isShowingIcon = true

        val clockObject = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .setPosition(0, 0)
            .setScale(100)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(clockObject)
            .build(applicationContext)

        glyphMatrixManager?.setMatrixFrame(frame.render())

        // On efface les anciens timers si l'utilisateur change de mode très vite
        resetHandler.removeCallbacksAndMessages(null)
        // On remet l'horloge au bout de 3 secondes (3000 ms)
        resetHandler.postDelayed({
            isShowingIcon = false
            renderCurrentTime()
        }, 3000)
    }

    private var glyphMatrixManager: GlyphMatrixManager? = null

    private val glyphManagerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManager?.register(Glyph.DEVICE_23112)
            isGlyphConnected = true // <-- AJOUT
            renderCurrentTime()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isGlyphConnected = false // <-- AJOUT
        }
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val event = extractToyEvent(msg.data)
                    when (event) {
                        GlyphToy.EVENT_AOD -> renderCurrentTime()
                        GlyphToy.EVENT_CHANGE -> renderCurrentTime()
                        GlyphToy.EVENT_ACTION_DOWN,
                        GlyphToy.EVENT_ACTION_UP,
                        null -> {}
                    }
                }
                else -> super.handleMessage(msg)
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

    private fun renderCurrentTime() {
        if (isShowingIcon) return // <-- AJOUTER CETTE LIGNE
        if (!isGlyphConnected) return

        val now = LocalTime.now()
        val hours = now.hour.toString().padStart(2, '0')
        val minutes = now.minute.toString().padStart(2, '0')

        // Récupération du niveau de batterie
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Transmission du pourcentage au renderer
        val bitmap = GlyphClockRenderer.render25x25(hours, minutes, batteryPct)

        val clockObject = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .setPosition(0, 0)
            .setScale(100)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(clockObject)
            .build(applicationContext)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    private fun extractToyEvent(bundle: Bundle?): String? {
        if (bundle == null) return null
        return bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA) ?: bundle.getString("data")
    }
}