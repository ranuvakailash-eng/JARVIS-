package com.jarvis.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
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

class JarvisVoiceService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())

    private var listeningForCommand = false
    private var destroyed = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startJarvisForeground()

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.UK
                textToSpeech?.setSpeechRate(0.9f)
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            setupSpeechRecognizer()
            startListening()
        }
    }

    private fun setupSpeechRecognizer() {

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!destroyed) {
                        restartListening()
                    }
                }

                override fun onResults(results: Bundle?) {

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
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )
    }

    private fun startListening() {

        if (destroyed) return

        handler.postDelayed({

            if (destroyed) return@postDelayed

            try {

                val intent =
                    Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    ).apply {

                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )

                        // FIXED:
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            Locale.UK.toLanguageTag()
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

                speechRecognizer?.cancel()
                speechRecognizer?.startListening(intent)

            } catch (_: Exception) {
                restartListening()
            }

        }, 500)
    }

    private fun restartListening() {

        if (destroyed) return

        handler.postDelayed({

            if (!destroyed) {
                startListening()
            }

        }, 1000)
    }

    private fun processVoice(text: String) {

        if (!listeningForCommand) {

            if (
                text.contains("jarvis") ||
                text.contains("javis")
            ) {

                listeningForCommand = true

                speak("Yes, sir?")

                handler.postDelayed({

                    if (!destroyed) {
                        startListening()
                    }

                }, 1600)

            } else {

                restartListening()
            }

            return
        }

        listeningForCommand = false

        when {

            text.contains("what time") -> {

                val time =
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.UK
                    ).format(Date())

                speak("The time is $time, sir.")
            }

            text.contains("open youtube") -> {

                openApp(
                    "com.google.android.youtube",
                    "YouTube"
                )
            }

            text.contains("open spotify") -> {

                openApp(
                    "com.spotify.music",
                    "Spotify"
                )
            }

            text.contains("hello") -> {

                speak("Good day, sir.")
            }

            text.contains("stop listening") ||
            text.contains("go to sleep") -> {

                speak("Standing by, sir.")
                return
            }

            else -> {

                speak("Command received, sir.")
            }
        }

        handler.postDelayed({

            if (!destroyed) {
                startListening()
            }

        }, 2500)
    }

    private fun speak(message: String) {

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS"
        )
    }

    private fun openApp(
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

            speak("Opening $name, sir.")

        } else {

            speak("$name is not installed, sir.")
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "jarvis",
                "JARVIS Voice",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "JARVIS voice activation"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun startJarvisForeground() {

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification.Builder(
                    this,
                    "jarvis"
                )
                    .setContentTitle("JARVIS")
                    .setContentText(
                        "Voice activation active"
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
                        "Voice activation active"
                    )
                    .setSmallIcon(
                        android.R.drawable.ic_btn_speak_now
                    )
                    .setOngoing(true)
                    .build()
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

        } else {

            startForeground(
                1001,
                notification
            )
        }
    }

    override fun onDestroy() {

        destroyed = true

        handler.removeCallbacksAndMessages(null)

        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}