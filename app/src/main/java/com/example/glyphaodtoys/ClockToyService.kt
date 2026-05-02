package com.example.glyphclock

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.*
import android.graphics.Bitmap
import android.media.AudioManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.nothing.ketchum.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ClockToyService : Service() {

    private var isShowingIcon = false
    private var isGlyphConnected = false
    private val resetHandler = Handler(Looper.getMainLooper())
    private var glyphMatrixManager: GlyphMatrixManager? = null

    private var lastTimeStr = ""
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm")

    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION && context != null) {
                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                val batteryPct = if (sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)) getBatteryLevel() else -1

                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE -> showIconTemporarily(GlyphClockRenderer.renderVibrateIcon(batteryPct))
                    AudioManager.RINGER_MODE_SILENT -> showIconTemporarily(GlyphClockRenderer.renderSilentIcon(batteryPct))
                    AudioManager.RINGER_MODE_NORMAL -> showIconTemporarily(GlyphClockRenderer.renderNormalIcon(batteryPct))
                }
            }
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Prefs.ACTION_REFRESH) renderCurrentTime()
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
        registerReceiver(refreshReceiver, IntentFilter(Prefs.ACTION_REFRESH), Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(ringerModeReceiver)
        unregisterReceiver(refreshReceiver)
        super.onDestroy()
    }

    private fun renderCurrentTime() {
        if (isShowingIcon || !isGlyphConnected) return

        val currentTimeStr = LocalTime.now().format(TIME_FORMATTER)

        if (lastTimeStr.isEmpty() || lastTimeStr == currentTimeStr) {
            lastTimeStr = currentTimeStr
            drawStaticTime(currentTimeStr)
        } else {
            animateTimeTransition(lastTimeStr, currentTimeStr)
            lastTimeStr = currentTimeStr
        }
    }

    private fun animateTimeTransition(oldTime: String, newTime: String) {
        val steps = 7
        val frameDuration = 45L
        val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val batteryPct = getBatteryLevel()
        val showBattery = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)

        val startX = (25 - ((5 * 2) + 1)) / 2
        val hourY = if (style == "gauge") 3 else 4
        val minuteY = if (style == "gauge") 15 else 14

        Thread {
            for (step in 0..steps) {
                val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)

                for (i in 0..3) {
                    val x = if (i % 2 == 0) startX else startX + 5 + 1
                    val y = if (i < 2) hourY else minuteY

                    if (oldTime[i] != newTime[i]) {
                        GlyphClockRenderer.renderDigitWithOffset(bitmap, oldTime[i], x, y, step)
                        GlyphClockRenderer.renderDigitWithOffset(bitmap, newTime[i], x, y, step - steps)
                    } else {
                        GlyphClockRenderer.renderDigitWithOffset(bitmap, newTime[i], x, y, 0)
                    }
                }

                if (showBattery) {
                    if (style == "gauge") GlyphClockRenderer.drawBatteryGauge(bitmap, batteryPct)
                    else GlyphClockRenderer.drawBatteryCircle(bitmap, batteryPct)
                }

                updateGlyphMatrix(bitmap)
                Thread.sleep(frameDuration)
            }
        }.start()
    }

    private fun drawStaticTime(timeStr: String) {
        val sharedPrefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val batteryPct = if (sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)) getBatteryLevel() else -1

        val bitmap = GlyphClockRenderer.render25x25(timeStr.substring(0,2), timeStr.substring(2,4), batteryPct, style)
        updateGlyphMatrix(bitmap)
    }

    private fun updateGlyphMatrix(bitmap: Bitmap) {
        val matrixObject = GlyphMatrixObject.Builder().setImageSource(bitmap).build()
        val frame = GlyphMatrixFrame.Builder().addTop(matrixObject).build(applicationContext)
        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun showIconTemporarily(bitmap: Bitmap) {
        if (!isGlyphConnected) return
        isShowingIcon = true
        updateGlyphMatrix(bitmap)
        resetHandler.removeCallbacksAndMessages(null)
        resetHandler.postDelayed({ isShowingIcon = false; renderCurrentTime() }, 3000)
    }

    private val glyphManagerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManager?.register(Glyph.DEVICE_23112)
            isGlyphConnected = true
            renderCurrentTime()
        }
        override fun onServiceDisconnected(name: ComponentName?) { isGlyphConnected = false }
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val data = msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (data == GlyphToy.EVENT_AOD || data == GlyphToy.EVENT_CHANGE) renderCurrentTime()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManager?.init(glyphManagerCallback)
        return Messenger(serviceHandler).binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }
}