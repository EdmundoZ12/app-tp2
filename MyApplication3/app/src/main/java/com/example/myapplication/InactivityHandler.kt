package com.example.myapplication

import android.os.Handler
import android.os.Looper

class InactivityHandler(
    private val handler: Handler,
    private val onTimeout: () -> Unit
) {
    private var inactivityRunnable: Runnable? = null
    private val inactivityTimeout = 10000L // 10 segundos

    init {
        inactivityRunnable = Runnable {
            onTimeout() // Llama a la función de tiempo de inactividad
        }
    }

    fun resetTimer() {
        handler.removeCallbacks(inactivityRunnable!!) // Remover cualquier callback anterior
        handler.postDelayed(inactivityRunnable!!, inactivityTimeout) // Programar el nuevo callback
    }
}
