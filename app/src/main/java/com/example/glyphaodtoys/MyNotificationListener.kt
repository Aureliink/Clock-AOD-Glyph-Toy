package com.example.glyphclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.util.Log

class MyNotificationListener : NotificationListenerService() {

    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // On considère que ça joue UNIQUEMENT si l'état est explicitement STATE_PLAYING
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            Log.d("Visualizer", "Changement d'état de lecture : $isPlaying")
            notifyService(isPlaying)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("Visualizer", "NotificationListener connecté !")
        val sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager

        // On écoute les changements globaux de sessions média
        sessionManager.addOnActiveSessionsChangedListener({ controllers ->
            updateActiveSession(controllers)
        }, ComponentName(this, MyNotificationListener::class.java))

        // Premier scan au démarrage
        updateActiveSession(sessionManager.getActiveSessions(ComponentName(this, MyNotificationListener::class.java)))
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        Log.d("Visualizer", "Sessions actives trouvées : ${controllers?.size ?: 0}")

        activeController?.unregisterCallback(callback)

        // On filtre les applications système parasites de Nothing OS
        val filteredControllers = controllers?.filter {
            it.packageName != "com.nothing.hearthstone" &&
                    it.packageName != "android" &&
                    it.packageName != "com.android.systemui"
        }

        // On cherche le lecteur actif (ex: Spotify)
        activeController = filteredControllers?.find {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: filteredControllers?.firstOrNull()

        if (activeController != null) {
            Log.d("Visualizer", "Cible identifiée : ${activeController?.packageName}")
            activeController?.registerCallback(callback)

            val state = activeController?.playbackState?.state
            val isPlaying = state == PlaybackState.STATE_PLAYING
            notifyService(isPlaying)
        } else {
            Log.d("Visualizer", "Aucun lecteur média valide.")
            notifyService(false)
        }
    }

    private var animStep = 0

    private fun notifyService(isPlaying: Boolean) {
        val intent = Intent(Prefs.ACTION_REFRESH).apply {
            putExtra("is_visualizer", true)
            putExtra("is_playing", isPlaying)

            if (isPlaying) {
                // On génère un faux échantillon audio (ByteArray) qui évolue avec animStep
                val fakeAudioData = ByteArray(56) // 56 octets (divisible par 7 barres)
                for (i in fakeAudioData.indices) {
                    // Utilisation de fonctions sinusoïdales entrelacées pour simuler un égaliseur vivant
                    val wave = Math.sin((i + animStep) * 0.4) * Math.cos((i - animStep) * 0.2)
                    fakeAudioData[i] = (Math.abs(wave) * 127).toInt().toByte()
                }
                putExtra("visualizer_data", fakeAudioData)

                // On fait progresser l'animation
                animStep++
            }
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        activeController?.unregisterCallback(callback)
        super.onDestroy()
    }
}