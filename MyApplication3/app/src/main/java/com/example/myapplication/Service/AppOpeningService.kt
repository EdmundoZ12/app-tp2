package com.example.myapplication.Service
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.example.myapplication.MainActivity
import kotlin.math.abs

class AppOpeningService : AccessibilityService(), SensorEventListener {
    private val logTag = "AppOpeningService"
    private lateinit var sensorManager: SensorManager
    private var vibrator: Vibrator? = null

    // Variables para controlar la frecuencia de activación
    private var lastLaunchTime: Long = 0
    private val launchDelay = 2000L // Tiempo mínimo entre lanzamientos (en milisegundos)

    // Umbral para detectar la sacudida
    private val shakeThreshold = 15 // Ajusta este valor según sea necesario
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    override fun onServiceConnected() {
        Log.i(logTag, "Service connected")
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Registra el sensor de aceleración
        val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calcular el cambio en los valores
            val deltaX = abs(x - lastX)
            val deltaY = abs(y - lastY)
            val deltaZ = abs(z - lastZ)

            // Verifica si el cambio en algún eje supera el umbral
            if ((deltaX > shakeThreshold && deltaY > shakeThreshold) || (deltaY > shakeThreshold && deltaZ > shakeThreshold) || (deltaX > shakeThreshold && deltaZ > shakeThreshold)) {
                val currentTime = System.currentTimeMillis()
                // Verifica si ha pasado el tiempo de lanzamiento
                if (currentTime - lastLaunchTime > launchDelay) {
                    lastLaunchTime = currentTime // Actualiza el tiempo del último lanzamiento
                    launchApp()
                }
            }

            // Actualiza los valores anteriores
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun launchApp() {
        // Lanza la actividad
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Log.d(logTag, "Open the app...")

        // Vibra el teléfono
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) // 500 ms de vibración
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el sensor al destruir el servicio
        sensorManager.unregisterListener(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
}
