package com.jarvis.mobile

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())

    private var destroyed = false
    private var waitingForCommand = false
    private var listening = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // IMPORTANT for Android 14+
        startJarvisForeground()

        setupTts()
        setupRecognizer()

        handler.postDelayed({
            if (!destroyed) {
                startListening()
            }
        }, 1000)
    }

    private fun startJarvisForeground() {

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun setupTts() {

        tts = TextToSpeech(
            applicationContext
        ) { status ->

            if (status == TextToSpeech.SUCCESS) {

                val result =
                    tts?.setLanguage(Locale.UK)

                if (
                    result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.language = Locale.US
                }

                tts?.setSpeechRate(0.9f)
            }
        }
    }

    private fun setupRecognizer() {

        if (
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (
            !SpeechRecognizer.isRecognitionAvailable(this)
        ) {
            speak(
                "Speech recognition is unavailable, sir."
            )
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

                    if (destroyed) {
                        return
                    }

                    val text =
                        results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            ?.lowercase(
                                Locale.getDefault()
                            )
                            ?.trim()

                    if (text.isNullOrEmpty()) {

                        restartListening()

                    } else {

                        processVoice(text)
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

        if (
            destroyed ||
            recognizer == null ||
            listening
        ) {
            return
        }

        if (
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
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
                        Locale.UK.toLanguageTag()
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
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

                    putExtra(
                        RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        packageName
                    )
                }

            recognizer?.startListening(intent)

        } catch (_: SecurityException) {

            listening = false

        } catch (_: Exception) {

            listening = false
            restartListening()
        }
    }

    private fun restartListening() {

        if (destroyed) {
            return
        }

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed({

            if (!destroyed) {
                startListening()
            }

        }, 1200)
    }

    private fun processVoice(
        text: String
    ) {

        // Waiting for "JARVIS"
        if (!waitingForCommand) {

            if (
                text.contains("jarvis") ||
                text.contains("javis")
            ) {

                waitingForCommand = true

                speak(
                    "Yes, sir."
                )

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

        waitingForCommand = false

        when {

            text.contains("hello") -> {

                speak(
                    "Good day, sir."
                )
            }

            text.contains("what time") ||
            text == "time" ||
            text.contains("current time") -> {

                val time =
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.UK
                    ).format(
                        Date()
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

            text.contains("stop listening") ||
            text.contains("go to sleep") -> {

                speak(
                    "Standing by, sir."
                )

                handler.removeCallbacksAndMessages(
                    null
                )

                return
            }

            else -> {

                speak(
                    "I heard you, sir."
                )
            }
        }

        handler.postDelayed({

            if (!destroyed) {
                startListening()
            }

        }, 2400)
    }

    private fun speak(
        message: String
    ) {

        if (destroyed) {
            return
        }

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

            startActivity(
                launchIntent
            )

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

        if (
            Build.VERSION.SDK_INT >=
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

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
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

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    override fun onDestroy() {

        destroyed = true

        handler.removeCallbacksAndMessages(
            null
        )

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