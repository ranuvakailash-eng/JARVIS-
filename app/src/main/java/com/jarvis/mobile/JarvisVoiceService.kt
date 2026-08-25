package com.jarvis.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisVoiceService : Service(), RecognitionListener {

    private lateinit var recognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())

    private var commandMode = false
    private var speaking = false
    private var destroyed = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            1001,
            createNotification()
        )

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.UK
                tts.setSpeechRate(0.95f)
            }
        }

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizer.setRecognitionListener(this)

        recognizerIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.UK
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }

        startStandby()
    }

    private fun startStandby() {

        if (destroyed || speaking) return

        commandMode = false

        handler.postDelayed({

            if (destroyed || speaking) return@postDelayed

            try {
                recognizer.cancel()
                recognizer.startListening(recognizerIntent)
            } catch (_: Exception) {
                retryListening()
            }

        }, 400)
    }

    private fun startCommandListening() {

        if (destroyed) return

        commandMode = true

        handler.postDelayed({

            if (destroyed) return@postDelayed

            try {
                recognizer.cancel()
                recognizer.startListening(recognizerIntent)
            } catch (_: Exception) {
                retryListening()
            }

        }, 500)
    }

    override fun onResults(results: Bundle?) {

        if (destroyed) return

        val matches =
            results?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )

        val spoken =
            matches?.firstOrNull()
                ?.lowercase(Locale.getDefault())
                ?.trim()

        if (spoken.isNullOrBlank()) {
            startStandby()
            return
        }

        processSpeech(spoken)
    }

    private fun processSpeech(spoken: String) {

        if (!commandMode) {

            if (containsWakeWord(spoken)) {

                commandMode = true

                speak(
                    "Yes, sir?",
                    listenAfterSpeech = true
                )
            } else {
                startStandby()
            }

            return
        }

        commandMode = false

        when {

            spoken.contains("stop listening") ||
            spoken.contains("go to sleep") -> {

                speak(
                    "Standing by, sir.",
                    listenAfterSpeech = false
                )
            }

            spoken.contains("what time") ||
            spoken.contains("current time") -> {

                val time =
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.UK
                    ).format(Date())

                speak(
                    "The time is $time, sir.",
                    listenAfterSpeech = false
                )
            }

            spoken.contains("open youtube") -> {

                openApplication(
                    "com.google.android.youtube",
                    "YouTube",
                    "Opening YouTube, sir."
                )
            }

            spoken.contains("open spotify") -> {

                openApplication(
                    "com.spotify.music",
                    "Spotify",
                    "Opening Spotify, sir."
                )
            }

            spoken.contains("hello") ||
            spoken.contains("hi jarvis") -> {

                speak(
                    "Good day, sir. How may I assist you?",
                    listenAfterSpeech = false
                )
            }

            else -> {

                speak(
                    "Command received, sir.",
                    listenAfterSpeech = false
                )
            }
        }
    }

    private fun containsWakeWord(
        spoken: String
    ): Boolean {

        return spoken.contains("jarvis") ||
               spoken.contains("jarvis") ||
               spoken.contains("javis") ||
               spoken.contains("jarvis")
    }

    private fun openApplication(
        packageName: String,
        applicationName: String,
        successMessage: String
    ) {

        val launchIntent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (launchIntent != null) {

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(launchIntent)

            speak(
                successMessage,
                listenAfterSpeech = false
            )

        } else {

            speak(
                "$applicationName is not installed, sir.",
                listenAfterSpeech = false
            )
        }
    }

    private fun speak(
        text: String,
        listenAfterSpeech: Boolean
    ) {

        if (destroyed) return

        speaking = true

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )

        handler.postDelayed({

            if (destroyed) return@postDelayed

            speaking = false

            if (listenAfterSpeech) {
                startCommandListening()
            } else {
                startStandby()
            }

        }, calculateSpeechDelay(text))
    }

    private fun calculateSpeechDelay(
        text: String
    ): Long {

        val estimated =
            700L + text.length * 45L

        return estimated.coerceIn(
            1500L,
            6000L
        )
    }

    private fun retryListening() {

        if (destroyed || speaking) return

        handler.postDelayed({

            if (!destroyed && !speaking) {
                startStandby()
            }

        }, 1200)
    }

    override fun onError(error: Int) {

        if (destroyed || speaking) return

        retryListening()
    }

    override fun onReadyForSpeech(
        params: Bundle?
    ) {
        updateHud(true)
    }

    override fun onBeginningOfSpeech() {
        updateHud(true)
    }

    override fun onEndOfSpeech() {
        updateHud(false)
    }

    override fun onRmsChanged(
        rmsdB: Float
    ) {
        // Audio level available here
        // for future HUD animation.
    }

    override fun onBufferReceived(
        buffer: ByteArray?
    ) {
    }

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) {
    }

    private fun updateHud(
        listening: Boolean
    ) {

        // MainActivity can later receive
        // this state through a shared state
        // mechanism for the animated HUD.
    }

    override fun onDestroy() {

        destroyed = true

        handler.removeCallbacksAndMessages(null)

        try {
            recognizer.cancel()
            recognizer.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "jarvis_voice",
                "JARVIS Voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "JARVIS voice activation"
            }

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                "jarvis_voice"
            )
                .setContentTitle("JARVIS")
                .setContentText(
                    "Voice activation is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()

        } else {

            Notification.Builder(this)
                .setContentTitle("JARVIS")
                .setContentText(
                    "Voice activation is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()
        }
    }
}