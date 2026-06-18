package com.example.glyphclock

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.*
import android.telephony.TelephonyManager
import com.example.glyphclock.renderer.GlyphClockRenderer
import com.nothing.ketchum.*
import com.example.glyphclock.renderer.GlyphBatteryRenderer
import com.example.glyphclock.renderer.GlyphCallRenderer
import com.example.glyphclock.renderer.GlyphStatusRenderer
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.graphics.Color
import com.example.glyphclock.MyNotificationListener
import com.example.glyphclock.renderer.GlyphMusicRenderer

class ClockToyService : Service() {

    private var isShowingIcon = false
    private var isGlyphConnected = false
    private val resetHandler = Handler(Looper.getMainLooper())
    private var glyphMatrixManagerInstance: GlyphMatrixManager? = null
    private var isPhoneCalling = false
    private val callHandler = Handler(Looper.getMainLooper())
    private var chargingPixelIndex = 0
    private var currentBatteryPct = 50

    private var lastTimeStr = ""
    private var isCharging = false
    private val chargingHandler = Handler(Looper.getMainLooper())
    private var isAnimatingTransition = false

    // Music visualizer
    private var isMusicActive = false

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            if (isMusicActive || isPhoneCalling || isShowingIcon || isAnimatingTransition) {
                clockHandler.postDelayed(this, 1000)
            } else {
                renderCurrentTime()
                clockHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val is12h = getSharedPreferences(Prefs.NAME, MODE_PRIVATE).getBoolean(Prefs.KEY_TIME_FORMAT_12H, false)
        // "hhmm" génère le format 12h (01 à 12), "HHmm" génère le format 24h (00 à 23)
        val pattern = if (is12h) "hhmm" else "HHmm"
        return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern))
    }

    // --- RECEIVERS SYSTEME ---
    private var lastRingerMode = -1
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION && context != null) {
                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                val currentMode = audioManager.ringerMode

                if (lastRingerMode != -1 && lastRingerMode != currentMode) {
                    handleAudioStatusChange(currentMode)
                }

                lastRingerMode = currentMode
            }
        }
    }

    private var musicAnimationStep = 0
    private val musicAnimationHandler = Handler(Looper.getMainLooper())

    private val musicAnimationRunnable = object : Runnable {
        override fun run() {
            if (!isMusicActive) return // Sécurité

            // On ne met à jour la matrice QUE si rien d'autre ne s'affiche
            if (!isPhoneCalling && !isShowingIcon && !isAnimatingTransition) {
                val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                val style = sharedPrefs.getString(Prefs.KEY_VISUALIZER_STYLE, "bars") ?: "bars"

                val bitmap: Bitmap = if (style == "cd") {
                    GlyphMusicRenderer.renderSpinningCD(musicAnimationStep)
                } else {
                    val fakeAudioData = ByteArray(25)
                    val center = 12
                    for (i in fakeAudioData.indices) {
                        val distanceFromCenter = Math.abs(i - center)
                        val damping = 1.0 - (distanceFromCenter / 12.0)
                        val wave = Math.sin((distanceFromCenter * 0.6) - (musicAnimationStep * 0.4))
                        val noise = Math.random() * 0.3
                        val finalValue = (Math.abs(wave) + noise) * damping
                        fakeAudioData[i] = (finalValue.coerceIn(0.0, 1.0) * 127).toInt().toByte()
                    }
                    GlyphMusicRenderer.renderVisualizer(fakeAudioData)
                }
                updateGlyphMatrix(bitmap)
            }

            // On incrémente le temps quoi qu'il arrive
            musicAnimationStep++
            musicAnimationHandler.postDelayed(this, 30)
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Prefs.ACTION_REFRESH) {
                val isFromVisualizer = intent.getBooleanExtra("is_visualizer", false)

                if (isFromVisualizer) {
                    val isPlaying = intent.getBooleanExtra("is_playing", false)
                    val isVisualizerEnabled = context?.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)?.getBoolean(Prefs.KEY_VISUALIZER, true) == true

                    if (isPlaying && isVisualizerEnabled) {
                        if (!isMusicActive) {
                            isMusicActive = true
                            musicAnimationHandler.removeCallbacksAndMessages(null)
                            musicAnimationHandler.post(musicAnimationRunnable)
                        }
                    } else {
                        if (isMusicActive) {
                            isMusicActive = false
                            musicAnimationHandler.removeCallbacksAndMessages(null)

                            lastTimeStr = ""
                            if (isCharging) startChargingAnimation() else renderCurrentTime()
                        }
                    }
                } else {
                    if (!isMusicActive) {
                        if (isCharging) startChargingAnimation() else {
                            lastTimeStr = ""
                            renderCurrentTime()
                        }
                    }
                }
            }
        }
    }

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING, TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    if (!isPhoneCalling) {
                        if (isCharging) stopChargingAnimation()
                        startCallAnimation()
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (isPhoneCalling) {
                        stopCallAnimation()
                        if (isCharging) {
                            callHandler.postDelayed({ startChargingAnimation() }, 200)
                        }
                    }
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                currentBatteryPct = (level * 100 / scale.toFloat()).toInt()
            }

            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            if (charging != isCharging) {
                isCharging = charging
                if (isCharging) {
                    chargingHandler.removeCallbacksAndMessages(null)
                    startChargingAnimation()
                } else {
                    stopChargingAnimation()
                }
            }
        }
    }

    // --- CYCLE DE VIE DU SERVICE ---
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, IntentFilter(Prefs.ACTION_REFRESH), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, IntentFilter(Prefs.ACTION_REFRESH))
        }

        registerReceiver(callReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        lastRingerMode = audioManager.ringerMode

        glyphMatrixManagerInstance = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManagerInstance?.init(glyphMatrixManagerInstanceCallback)
    }

    override fun onDestroy() {
        stopClock()
        unregisterReceiver(ringerModeReceiver)
        unregisterReceiver(refreshReceiver)
        unregisterReceiver(callReceiver)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}
        chargingHandler.removeCallbacksAndMessages(null)
        callHandler.removeCallbacksAndMessages(null)
        resetHandler.removeCallbacksAndMessages(null)
        clockHandler.removeCallbacksAndMessages(null)
        glyphMatrixManagerInstance?.unInit()
        super.onDestroy()
    }

    private fun startClock() {
        clockHandler.removeCallbacks(clockRunnable)
        clockHandler.post(clockRunnable)
    }

    private fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun renderCurrentTime() {
        if (isPhoneCalling || isShowingIcon || isAnimatingTransition || isMusicActive) return

        val currentTimeStr = getCurrentTimeFormatted()

        if (lastTimeStr.isEmpty() || lastTimeStr == currentTimeStr) {
            lastTimeStr = currentTimeStr
            if (!isCharging) {
                drawStaticTime(currentTimeStr)
            }
        } else {
            if (isCharging) {
                chargingHandler.removeCallbacksAndMessages(null)
            }
            animateTimeTransition(lastTimeStr, currentTimeStr)
            lastTimeStr = currentTimeStr
        }
    }

    private fun drawStaticTime(timeStr: String) {
        val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val showBattery = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)

        val bitmap = GlyphClockRenderer.render25x25(timeStr.substring(0, 2), timeStr.substring(2, 4), style)

        if (showBattery) {
            GlyphBatteryRenderer.drawBattery(bitmap, getBatteryLevel(), style)
        }

        updateGlyphMatrix(bitmap)
    }

    private fun animateTimeTransition(oldTime: String, newTime: String) {
        val steps = 7
        val frameDuration = 45L
        val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val showBattery = sharedPrefs.getBoolean(Prefs.KEY_BATTERY, true)
        val batteryPct = getBatteryLevel()

        val startX = (25 - ((5 * 2) + 1)) / 2
        val hourY = if (style == "gauge") 3 else 4
        val minuteY = if (style == "gauge") 15 else 14
        val digitBoxHeight = 7

        Thread {
            for (step in 0..steps) {
                if (isMusicActive || isPhoneCalling) break
                val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)

                if (showBattery) {
                    GlyphBatteryRenderer.drawBattery(bitmap, batteryPct, style)
                }

                val digitsLayer = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
                for (i in 0..3) {
                    val x = if (i % 2 == 0) startX else startX + 5 + 1
                    val y = if (i < 2) hourY else minuteY

                    if (oldTime[i] != newTime[i]) {
                        if (step < steps) {
                            GlyphClockRenderer.renderDigitWithOffset(digitsLayer, oldTime[i], x, y, step)
                        }
                        GlyphClockRenderer.renderDigitWithOffset(digitsLayer, newTime[i], x, y, step - steps)
                    } else {
                        GlyphClockRenderer.renderDigitWithOffset(digitsLayer, newTime[i], x, y, 0)
                    }
                }

                for (py in 0..24) {
                    val inHourBox = py in hourY until (hourY + digitBoxHeight)
                    val inMinuteBox = py in minuteY until (minuteY + digitBoxHeight)

                    if (inHourBox || inMinuteBox) {
                        for (px in 0..24) {
                            val pixelColor = digitsLayer.getPixel(px, py)
                            if (pixelColor != 0 && pixelColor != Color.BLACK) {
                                bitmap.setPixel(px, py, pixelColor)
                            }
                        }
                    }
                }

                updateGlyphMatrix(bitmap)
                Thread.sleep(frameDuration)
            }

            isAnimatingTransition = false

            if (isCharging) {
                Handler(Looper.getMainLooper()).post { startChargingAnimation() }
            } else {
                Handler(Looper.getMainLooper()).post { renderCurrentTime() }
            }
        }.start()
    }

    private fun startChargingAnimation() {
        chargingHandler.removeCallbacksAndMessages(null)

        val realBatteryPct = getBatteryLevel()
        chargingPixelIndex = 0

        val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val maxSteps = if (style == "gauge") 25 else GlyphBatteryRenderer.circlePixelsCount
        val targetStep = (maxSteps * realBatteryPct) / 100

        val timeStr = getCurrentTimeFormatted()
        val baseClockBitmap = GlyphClockRenderer.render25x25(timeStr.substring(0, 2), timeStr.substring(2, 4), style)

        val runnable = object : Runnable {
            override fun run() {
                if (!isCharging || isShowingIcon || isPhoneCalling || isMusicActive) return

                val virtualPct = (chargingPixelIndex * 100) / maxSteps
                val workingBitmap = baseClockBitmap.copy(Bitmap.Config.ARGB_8888, true)

                GlyphBatteryRenderer.drawBattery(workingBitmap, virtualPct, style)
                updateGlyphMatrix(workingBitmap)

                chargingPixelIndex++
                if (chargingPixelIndex > targetStep || chargingPixelIndex > maxSteps) {
                    chargingPixelIndex = 0
                }

                val finalDelay = if (style == "gauge") 60L else 40L
                chargingHandler.postDelayed(this, finalDelay)
            }
        }
        runnable.run()
    }

    private fun stopChargingAnimation() {
        chargingHandler.removeCallbacksAndMessages(null)
        lastTimeStr = ""
        renderCurrentTime()
    }

    // --- LOGIQUE DE L'ANIMATION D'APPEL ---
    private var emissionStep = 0
    private val animationHandler = Handler(Looper.getMainLooper())

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isPhoneCalling) return

            val bitmap = GlyphCallRenderer.renderPhoneIcon(emissionStep)
            updateGlyphMatrix(bitmap)

            emissionStep++
            animationHandler.postDelayed(this, 300)
        }
    }

    private fun startCallAnimation() {
        isPhoneCalling = true
        animationHandler.removeCallbacks(animationRunnable)
        animationHandler.post(animationRunnable)
    }

    private fun stopCallAnimation() {
        isPhoneCalling = false
        animationHandler.removeCallbacks(animationRunnable)
        emissionStep = 0

        lastTimeStr = ""
        if (isCharging) {
            startChargingAnimation()
        } else {
            startClock()
            renderCurrentTime()
        }
    }

    private fun handleAudioStatusChange(ringerMode: Int) {
        if (isPhoneCalling || isMusicActive) return
        if (isCharging) stopChargingAnimation()
        isShowingIcon = true

        val bitmap = when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> GlyphStatusRenderer.renderSilentIcon()
            AudioManager.RINGER_MODE_VIBRATE -> GlyphStatusRenderer.renderVibrateIcon()
            else -> GlyphStatusRenderer.renderNormalIcon()
        }

        updateGlyphMatrix(bitmap)

        resetHandler.removeCallbacksAndMessages(null)
        resetHandler.postDelayed({
            isShowingIcon = false
            if (isCharging) {
                startChargingAnimation()
            } else {
                lastTimeStr = ""
                renderCurrentTime()
            }
        }, 3000)
    }

    private fun updateGlyphMatrix(bitmap: Bitmap) {
        try {
            val matrixObject = GlyphMatrixObject.Builder().setImageSource(bitmap).build()
            val frame = GlyphMatrixFrame.Builder().addTop(matrixObject).build(applicationContext)
            glyphMatrixManagerInstance?.setMatrixFrame(frame.render())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBatteryLevel(): Int {
        return currentBatteryPct
    }

    private val glyphMatrixManagerInstanceCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManagerInstance?.register(Glyph.DEVICE_23112)
            startClock()

            if (isCharging) {
                startChargingAnimation()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            stopClock()
            stopChargingAnimation()
        }
    }

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == GlyphToy.MSG_GLYPH_TOY) {
                val data = msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (data == GlyphToy.EVENT_AOD || data == GlyphToy.EVENT_CHANGE) {
                    renderCurrentTime()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return Messenger(serviceHandler).binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopClock()
        isGlyphConnected = false
        lastTimeStr = ""
        return true
    }
}