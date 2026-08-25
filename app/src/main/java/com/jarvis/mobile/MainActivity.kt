package com.jarvis.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(JarvisHudView(this))

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO
            )

        } else {

            startJarvisService()
        }
    }

    private fun startJarvisService() {

        val intent =
            Intent(this, JarvisVoiceService::class.java)

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

        if (requestCode == REQUEST_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            startJarvisService()
        }
    }

    companion object {
        private const val REQUEST_AUDIO = 1001
    }
}