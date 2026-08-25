package com.jarvis.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class JarvisVoiceService : Service(), RecognitionListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())

    private var commandMode = false
    private var speaking = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(
            1001,
            createNotification()
        )

        tts = TextToSpeech(this) {
            tts.language = Locale.UK
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)

        speechIntent = Intent(
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
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
        }

        startStandbyListening()
    }

    private fun startStandbyListening() {
        if (speaking) return

        commandMode = false

        handler.postDelayed({
            try {
                speechRecognizer.cancel()
                speechRecognizer.startListening(speechIntent)
            } catch (_: Exception) {
            }
        }, 300)
    }

    private fun startCommandListening() {
        commandMode = true

        handler.postDelayed({
            try {
                speechRecognizer.cancel()
                speechRecognizer.startListening(speechIntent)
            } catch (_: Exception) {
            }
        }, 500)
    }

    private fun processSpeech(text: String) {

        val spoken = text
            .lowercase(Locale.getDefault())
            .trim()

        if (!commandMode) {

            if (spoken.contains("jarvis")) {

                speechRecognizer.cancel()

                speak(
                    "Yes, sir?",
                    true
                )
            }

            return
        }

        commandMode = false

        when {

            spoken.contains("stop listening") ||
            spoken.contains("go to sleep") -> {

                speak(
                    "Standing by, sir.",
                    false
                )
            }

            spoken.contains("what time") -> {

                val time = java.text.SimpleDateFormat(
                    "h:mm a",
                    Locale.UK
                ).format(java.util.Date())

                speak(
                    "The time is $time, sir.",
                    false
                )
            }

            spoken.contains("open youtube") -> {

                val launchIntent =
                    packageManager.getLaunchIntentForPackage(
                        "com.google.android.youtube"
                    )

                if (launchIntent != null) {
                    startActivity(
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )

                    speak(
                        "Opening YouTube, sir.",
                        false
                    )
                } else {
                    speak(
                        "YouTube is not installed, sir.",
                        false
                    )
                }
            }

            spoken.contains("open spotify") -> {

                val launchIntent =
                    packageManager.getLaunchIntentForPackage(
                        "com.spotify.music"
                    )

                if (launchIntent != null) {
                    startActivity(
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )

                    speak(
                        "Opening Spotify, sir.",
                        false
                    )
                } else {
                    speak(
                        "Spotify is not installed, sir.",
                        false
                    )
                }
            }

            else -> {

                speak(
                    "Command received, sir.",
                    false
                )
            }
        }
    }

    private fun speak(
        text: String,
        listenAfter: Boolean
    ) {
        speaking = true

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )

        handler.postDelayed({

            speaking = false

            if (listenAfter) {
                startCommandListening()
            } else {
                startStandbyListening()
            }

        }, 1500)
    }

    override fun onResults(results: Bundle?) {

        val matches =
            results?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )

        val text =
            matches?.firstOrNull()

        if (!text.isNullOrBlank()) {
            processSpeech(text)
        } else {
            startStandbyListening()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {

        val matches =
            partialResults?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )

        val text =
            matches?.firstOrNull()
                ?: return

        processSpeech(text)
    }

    override fun onError(error: Int) {

        if (!speaking) {
            startStandbyListening()
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) {}

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                "jarvis_voice",
                "JARVIS Voice",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= 26) {

            Notification.Builder(
                this,
                "jarvis_voice"
            )
                .setContentTitle("JARVIS")
                .setContentText(
                    "Voice activation is ready"
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
                    "Voice activation is ready"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()
        }
    }
}