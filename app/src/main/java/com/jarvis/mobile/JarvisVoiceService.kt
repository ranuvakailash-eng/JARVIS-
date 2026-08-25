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
import java.util.Locale

class JarvisVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val handler =
        Handler(Looper.getMainLooper())

    private var destroyed = false
    private var waitingForCommand = false
    private var listening = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        setupTts()

        setupRecognizer()

        handler.postDelayed(
            {
                startListening()
            },
            1000
        )
    }

    private fun setupTts() {

        tts = TextToSpeech(
            applicationContext
        ) { status ->

            if (status == TextToSpeech.SUCCESS) {

                tts?.language = Locale.UK

                tts?.setSpeechRate(0.9f)
            }
        }
    }

    private fun setupRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is unavailable, sir.")
            return
        }

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    listening = true
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(
                    error: Int
                ) {

                    listening = false

                    if (!destroyed) {
                        restartListening()
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    listening = false

                    if (destroyed) return

                    val resultsList =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        resultsList
                            ?.firstOrNull()
                            ?.lowercase(Locale.getDefault())
                            ?.trim()

                    if (!text.isNullOrEmpty()) {

                        processVoice(text)

                    } else {

                        restartListening()
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    private fun startListening() {

        if (destroyed ||
            recognizer == null ||
            listening
        ) {
            return
        }

        try {

            recognizer?.cancel()

            val intent =
                Intent(
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
                        1
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                        false
                    )
                }

            recognizer?.startListening(intent)

        } catch (_: Exception) {

            restartListening()
        }
    }

    private fun restartListening() {

        if (destroyed) return

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {
                if (!destroyed) {
                    startListening()
                }
            },
            1200
        )
    }

    private fun processVoice(text: String) {

        if (!waitingForCommand) {

            if (
                text.contains("jarvis") ||
                text.contains("javis")
            ) {

                waitingForCommand = true

                speak("Yes, sir.")

                handler.postDelayed(
                    {
                        if (!destroyed) {
                            startListening()
                        }
                    },
                    1300
                )

            } else {

                restartListening()
            }

            return
        }

        waitingForCommand = false

        when {

            text.contains("hello") -> {

                speak("Good day, sir.")
            }

            text.contains("what time") ||
            text.contains("time") -> {

                val time =
                    java.text.SimpleDateFormat(
                        "h:mm a",
                        Locale.UK
                    ).format(
                        java.util.Date()
                    )

                speak(
                    "The time is $time, sir."
                )
            }

            text.contains("open youtube") -> {

                openApplication(
                    "com.google.android.youtube",
                    "YouTube"
                )
            }

            text.contains("open spotify") -> {

                openApplication(
                    "com.spotify.music",
                    "Spotify"
                )
            }

            else -> {

                speak(
                    "I heard you, sir."
                )
            }
        }

        handler.postDelayed(
            {
                if (!destroyed) {
                    startListening()
                }
            },
            2200
        )
    }

    private fun speak(message: String) {

        if (destroyed) return

        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS"
        )
    }

    private fun openApplication(
        packageName: String,
        name: String
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
                "Opening $name, sir."
            )

        } else {

            speak(
                "$name is not installed, sir."
            )
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "JARVIS Voice",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "JARVIS voice activation"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
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

    override fun onDestroy() {

        destroyed = true

        handler.removeCallbacksAndMessages(null)

        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    companion object {

        private const val CHANNEL_ID =
            "jarvis_voice"

        private const val NOTIFICATION_ID =
            1001
    }
}