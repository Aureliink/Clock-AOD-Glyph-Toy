package com.example.glyphclock

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
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm")
    private var isCharging = false
    private val chargingHandler = Handler(Looper.getMainLooper())
    private var isAnimatingTransition = false

    // Remplacement du Timer par un couple Handler / Runnable sur le thread principal
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            renderCurrentTime()
            clockHandler.postDelayed(this, 1000) // S'exécute toutes les secondes proprement
        }
    }

    // --- RECEIVERS SYSTEME ---
    private var lastRingerMode = -1
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION && context != null) {
                val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                val currentMode = audioManager.ringerMode

                // On ne déclenche l'icône QUE si le mode a réellement changé par rapport à avant
                if (lastRingerMode != -1 && lastRingerMode != currentMode) {
                    handleAudioStatusChange(currentMode)
                }

                lastRingerMode = currentMode
            }
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Prefs.ACTION_REFRESH) {
                if (isCharging) {
                    // On relance instantanément l'animation avec le nouveau style
                    startChargingAnimation()
                } else {
                    lastTimeStr = ""
                    renderCurrentTime()
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
            // Extraction asynchrone et non-bloquante du pourcentage
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
    override fun onCreate() {
        super.onCreate()
        registerReceiver(ringerModeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
        registerReceiver(refreshReceiver, IntentFilter(Prefs.ACTION_REFRESH), RECEIVER_NOT_EXPORTED)
        registerReceiver(callReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // On mémorise le mode audio de départ pour bloquer le faux déclenchement
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        lastRingerMode = audioManager.ringerMode

        glyphMatrixManagerInstance = GlyphMatrixManager.getInstance(applicationContext)
        glyphMatrixManagerInstance?.init(glyphManagerCallback)
    }

    override fun onDestroy() {
        stopClock()
        unregisterReceiver(ringerModeReceiver)
        unregisterReceiver(refreshReceiver)
        unregisterReceiver(callReceiver)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Sécurité
        }
        chargingHandler.removeCallbacksAndMessages(null)
        callHandler.removeCallbacksAndMessages(null)
        resetHandler.removeCallbacksAndMessages(null)
        clockHandler.removeCallbacksAndMessages(null)
        glyphMatrixManagerInstance?.unInit()
        super.onDestroy()
    }

    // --- LOGIQUE DE L'HORLOGE (MAIN THREAD LOOP) ---

    private fun startClock() {
        clockHandler.removeCallbacks(clockRunnable)
        clockHandler.post(clockRunnable) // Planification séquentielle saine
    }

    private fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun renderCurrentTime() {
        // On ajoute isAnimatingTransition à la condition de sortie
        if (isPhoneCalling || isShowingIcon || isAnimatingTransition) return

        val currentTimeStr = LocalTime.now().format(TIME_FORMATTER)

        if (lastTimeStr.isEmpty() || lastTimeStr == currentTimeStr) {
            lastTimeStr = currentTimeStr
            if (!isCharging) {
                drawStaticTime(currentTimeStr)
            }
        } else {
            // LA MINUTE A CHANGÉ !
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
                // 1. Nouveau Bitmap (Tout noir)
                val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)

                // 2. DESSIN DE LA BATTERIE (Fond stable : Blanc + Gris)
                if (showBattery) {
                    // C'est cet appel qui dessine la structure complète (les 30,30,30 inclus)
                    GlyphBatteryRenderer.drawBattery(bitmap, batteryPct, style)
                }

                // 3. CALQUE DES CHIFFRES (Scrolling)
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

                // 4. TRANSFERT AVEC CLIPPING (On ne touche pas à la jauge !)
                for (py in 0..24) {
                    val inHourBox = py in hourY until (hourY + digitBoxHeight)
                    val inMinuteBox = py in minuteY until (minuteY + digitBoxHeight)

                    if (inHourBox || inMinuteBox) {
                        for (px in 0..24) {
                            val pixelColor = digitsLayer.getPixel(px, py)
                            // On ne copie que les pixels des chiffres (blancs)
                            if (pixelColor != 0 && pixelColor != Color.BLACK) {
                                bitmap.setPixel(px, py, pixelColor)
                            }
                        }
                    }
                }

                updateGlyphMatrix(bitmap)
                Thread.sleep(frameDuration)
            }

            // ANIMATION TERMINÉE
            isAnimatingTransition = false // ON LIBÈRE

            if (isCharging) {
                Handler(Looper.getMainLooper()).post { startChargingAnimation() }
            }
            else {
                // Optionnel : forcer un rendu propre immédiat pour éviter d'attendre la seconde suivante
                Handler(Looper.getMainLooper()).post {
                    renderCurrentTime()
                }
            }
        }.start()
    }

    // --- LOGIQUE DE L'ANIMATION DE CHARGE ---

    private fun startChargingAnimation() {
        chargingHandler.removeCallbacksAndMessages(null)

        val realBatteryPct = getBatteryLevel()
        chargingPixelIndex = 0

        val sharedPrefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val style = sharedPrefs.getString(Prefs.KEY_BATTERY_STYLE, "ring") ?: "ring"
        val maxSteps = if (style == "gauge") 25 else GlyphBatteryRenderer.circlePixelsCount
        val targetStep = (maxSteps * realBatteryPct) / 100

        val timeStr = LocalTime.now().format(TIME_FORMATTER)
        val baseClockBitmap = GlyphClockRenderer.render25x25(timeStr.substring(0, 2), timeStr.substring(2, 4), style)

        val runnable = object : Runnable {
            override fun run() {
                if (!isCharging || isShowingIcon || isPhoneCalling) return

                val virtualPct = (chargingPixelIndex * 100) / maxSteps
                val workingBitmap = baseClockBitmap.copy(Bitmap.Config.ARGB_8888, true)

                GlyphBatteryRenderer.drawBattery(workingBitmap, virtualPct, style)
                updateGlyphMatrix(workingBitmap)

                chargingPixelIndex++
                // On boucle simplement sur la jauge, sans se soucier de l'heure
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
            if (!isPhoneCalling) return // Sécurité : on stoppe net si l'appel est fini

            val bitmap = GlyphCallRenderer.renderPhoneIcon(emissionStep)
            updateGlyphMatrix(bitmap)

            emissionStep++
            animationHandler.postDelayed(this, 300)
        }
    }

    private fun startCallAnimation() {
        isPhoneCalling = true // <--- CRUCIAL : Active le verrou pour le Receiver
        animationHandler.removeCallbacks(animationRunnable)
        animationHandler.post(animationRunnable)
    }

    private fun stopCallAnimation() {
        isPhoneCalling = false // <--- CRUCIAL : Relâche le verrou
        animationHandler.removeCallbacks(animationRunnable)
        emissionStep = 0

        // REFRESH DE L'ÉCRAN : On efface l'icône d'appel pour revenir à l'état normal
        lastTimeStr = ""
        if (isCharging) {
            startChargingAnimation()
        } else {
            startClock()
            renderCurrentTime()
        }
    }

    // --- LOGIQUE DES ICONES TEMPORAIRES (STATUS & AUDIO) ---

    private fun handleAudioStatusChange(ringerMode: Int) {
        if (isPhoneCalling) return
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

    // --- INTERACTION AVEC LE SDK NOTHING (CONFORME & SECURISE) ---

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

    // --- GESTION DU CALLBACK ET DU SERVICE HANDLER ---

    private val glyphManagerCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManagerInstance?.register(Glyph.DEVICE_23112)
            startClock()

            // On se base sur le vrai état capturé par le batteryReceiver dans le onCreate
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
        // L'initialisation est déplacée dans onCreate pour paralléliser le chargement
        return Messenger(serviceHandler).binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopClock()
        isGlyphConnected = false
        lastTimeStr = ""
        return true
    }
}