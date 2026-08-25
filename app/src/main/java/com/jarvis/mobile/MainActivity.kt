package com.jarvis.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()

        // JARVIS HUD
        setContentView(JarvisHudView())

        // Request microphone permission
        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        } else {
            startJarvisVoiceService()
        }
    }

    private fun startJarvisVoiceService() {

        val intent = Intent(
            this,
            JarvisVoiceService::class.java
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startJarvisVoiceService()
        }
    }

    private fun hideSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.hide(
                android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars()
            )

        } else {

            @Suppress("DEPRECATION")

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    /**
     * JARVIS HUD
     * Simple rotating spiral made from dots.
     */
    private inner class JarvisHudView : View(
        this@MainActivity
    ) {

        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)

        private var rotation = 0f

        init {
            setBackgroundColor(
                0xFF000000.toInt()
            )
        }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            val cx = width / 2f
            val cy = height / 2f

            val radius =
                minOf(width, height) * 0.36f

            // Spiral dots
            for (i in 0 until 120) {

                val progress =
                    i / 119f

                val angle =
                    progress *
                        Math.PI.toFloat() *
                        8f +
                        rotation

                val r =
                    radius *
                        (0.08f +
                            progress * 0.85f)

                val x =
                    cx +
                        cos(
                            angle.toDouble()
                        ).toFloat() *
                        r

                val y =
                    cy +
                        sin(
                            angle.toDouble()
                        ).toFloat() *
                        r

                paint.color =
                    0xFF66BFFF.toInt()

                paint.alpha =
                    (255 -
                        progress * 180)
                        .toInt()

                val size =
                    1.5f +
                        (1f - progress) *
                        2.5f

                canvas.drawCircle(
                    x,
                    y,
                    size,
                    paint
                )
            }

            // Centre dot
            paint.alpha = 255

            paint.color =
                0xFFBFE9FF.toInt()

            canvas.drawCircle(
                cx,
                cy,
                5f,
                paint
            )

            // Animation
            rotation += 0.015f

            postInvalidateOnAnimation()
        }
    }
}