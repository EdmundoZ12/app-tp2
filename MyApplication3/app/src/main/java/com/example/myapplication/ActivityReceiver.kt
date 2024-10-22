package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ActivityReceiver(private val inactivityHandler: InactivityHandler) : BroadcastReceiver() {

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(Intent.ACTION_ALL_APPS) // Para cambios de volumen
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this) // Desregistra el BroadcastReceiver
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        inactivityHandler.resetTimer() // Reinicia el temporizador al recibir un evento
    }
}
