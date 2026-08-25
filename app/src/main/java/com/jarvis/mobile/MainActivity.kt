package com.jarvis.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import android.graphics.*

class MainActivity : Activity() {

    private lateinit var hud: JarvisView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        hud = JarvisView(this)
        setContentView(hud)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        } else {
            startJarvisService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startJarvisService()
        }
    }

    private fun startJarvisService() {
        val intent = Intent(this, JarvisVoiceService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

private class JarvisView(
    activity: Activity
) : View(activity) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var phase = 0f

    var listening = false
        set(value) {
            field = value
            invalidate()
        }

    var command = ""
        set(value) {
            field = value
            invalidate()
        }

    init {
        setBackgroundColor(Color.BLACK)

        textPaint.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.NORMAL
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        phase += if (listening) 0.055f else 0.018f

        val cx = width / 2f
        val cy = height * 0.50f
        val base = min(width, height).toFloat()

        drawTopStatus(canvas)
        drawSpiral(canvas, cx, cy, base)
        drawBottomStatus(canvas)

        if (listening) {
            drawListening(canvas, cx, cy + base * 0.27f)
        } else if (command.isNotBlank()) {
            drawCommand(canvas, command, cx, cy + base * 0.27f)
        } else {
            drawReady(canvas, cx, cy + base * 0.27f)
        }

        postInvalidateOnAnimation()
    }

    private fun drawTopStatus(canvas: Canvas) {
        textPaint.textSize = 22f
        textPaint.color = Color.rgb(0, 205, 255)
        textPaint.alpha = 230

        canvas.drawText(
            "J.A.R.V.I.S.",
            42f,
            62f,
            textPaint
        )

        textPaint.textSize = 12f
        textPaint.alpha = 150

        canvas.drawText(
            "SYSTEM ONLINE",
            42f,
            86f,
            textPaint
        )

        canvas.drawText(
            "100%",
            width - 78f,
            62f,
            textPaint
        )
    }

    private fun drawSpiral(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        base: Float
    ) {
        val maxRadius = base * 0.29f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.argb(80, 0, 190, 255)

        for (i in 1..4) {
            canvas.drawCircle(
                cx,
                cy,
                maxRadius * (0.58f + i * 0.12f),
                paint
            )
        }

        paint.style = Paint.Style.FILL

        val arms = 2.0
        val dots = 170

        for (i in 0 until dots) {
            val t = i.toFloat() / dots
            val radius = maxRadius * (0.06f + t * 0.94f)
            val angle =
                t * Math.PI.toFloat() * 2.0f * arms + phase

            for (arm in 0..1) {
                val a = angle + arm * Math.PI.toFloat()

                val x = cx + cos(a) * radius
                val y = cy + sin(a) * radius

                val size = 1.5f + t * 3.2f

                val alpha =
                    (75 + 180 * (1f - t * 0.55f))
                        .toInt()
                        .coerceIn(40, 255)

                paint.color = Color.argb(
                    alpha,
                    0,
                    190 + (50 * (1f - t))
                        .toInt()
                        .coerceIn(0, 50),
                    255
                )

                canvas.drawCircle(
                    x.toFloat(),
                    y.toFloat(),
                    size,
                    paint
                )
            }
        }

        for (i in 0 until 12) {
            val a =
                phase * 1.7f +
                i * (Math.PI.toFloat() * 2f / 12f)

            val r = maxRadius * 0.80f

            val x = cx + cos(a) * r
            val y = cy + sin(a) * r

            paint.color = Color.argb(
                245,
                70,
                225,
                255
            )

            canvas.drawCircle(
                x.toFloat(),
                y.toFloat(),
                4.2f,
                paint
            )
        }

        paint.color = Color.BLACK

        canvas.drawCircle(
            cx,
            cy,
            maxRadius * 0.055f,
            paint
        )
    }

    private fun drawReady(
        canvas: Canvas,
        cx: Float,
        y: Float
    ) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f
        textPaint.color = Color.argb(
            190,
            0,
            210,
            255
        )

        canvas.drawText(
            "VOICE ACTIVATION",
            cx,
            y,
            textPaint
        )

        textPaint.textSize = 10f
        textPaint.alpha = 100

        canvas.drawText(
            "SAY \"JARVIS\"",
            cx,
            y + 20f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawListening(
        canvas: Canvas,
        cx: Float,
        y: Float
    ) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 15f
        textPaint.color = Color.rgb(
            0,
            220,
            255
        )

        canvas.drawText(
            "LISTENING...",
            cx,
            y,
            textPaint
        )

        paint.color = Color.rgb(
            0,
            210,
            255
        )

        for (i in -12..12) {
            val height =
                3f +
                (sin(phase * 5f + i * 0.8f) + 1f) * 8f

            canvas.drawRect(
                cx + i * 7f,
                y + 16f - height,
                cx + i * 7f + 3f,
                y + 16f + height,
                paint
            )
        }

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawCommand(
        canvas: Canvas,
        command: String,
        cx: Float,
        y: Float
    ) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 12f
        textPaint.color = Color.argb(
            180,
            0,
            210,
            255
        )

        val shown =
            if (command.length > 38)
                command.take(35) + "..."
            else
                command

        canvas.drawText(
            shown,
            cx,
            y,
            textPaint
        )

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawBottomStatus(canvas: Canvas) {
        val y = height - 48f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(
            100,
            0,
            180,
            255
        )

        canvas.drawLine(
            30f,
            y,
            width - 30f,
            y,
            paint
        )

        textPaint.textSize = 10f
        textPaint.color = Color.argb(
            125,
            0,
            200,
            255
        )

        canvas.drawText(
            "READY",
            30f,
            y + 25f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(
            "SECURE CONNECTION",
            width - 30f,
            y + 25f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.LEFT
    }
}