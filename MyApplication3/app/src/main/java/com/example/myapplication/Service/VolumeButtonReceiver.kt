package com.example.myapplication.Service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.example.myapplication.MainActivity

class VolumeButtonReceiver : BroadcastReceiver() {
    private val logTag = "VolumeButtonReceiver"
    private var keyEvents = 0
    private val requiredKeyEvents = 3
    private val detectionInterval = 2000L

    private val handler = Handler(Looper.getMainLooper()) // Usamos un Handler en lugar de un hilo

    override fun onReceive(context: Context, intent: Intent) {
        val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            keyEvents++
            Log.d(logTag, "Key pressed $keyEvents times")
            if (keyEvents == 1) {
                startKeyEventsCounter()
            }

            if (keyEvents == requiredKeyEvents) {
                // Lanzamos la actividad
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launchIntent)
                Log.d(logTag, "Open the app...")
                keyEvents = 0
            }
        }
    }

    private fun startKeyEventsCounter() {
        handler.postDelayed({
            keyEvents = 0
        }, detectionInterval)
    }
}
