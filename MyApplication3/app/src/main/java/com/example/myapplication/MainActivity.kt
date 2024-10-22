package com.example.myapplication

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.Service.AppOpeningService
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private var showDialog by mutableStateOf(false)
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var inactivityTimer: MyCountDownTimer
    private val inactivityTimeout = 10000L // 10 segundos de inactividad

    private lateinit var activityReceiver: ActivityReceiver // Declarar el BroadcastReceiver

    companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 1
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el servicio de accesibilidad está habilitado
        if (!isAccessibilityServiceEnabled(AppOpeningService::class.java)) {
            showDialog = true
        }

        // Solicitar permiso de grabación de audio
        requestAudioPermission()

        // Inicializa el SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Inicializa TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Language not supported")
                }
            }
        }

        // Inicializa el temporizador de inactividad
        resetInactivityTimer()

        // Inicializa y registra el BroadcastReceiver
        activityReceiver = ActivityReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON) // Para encendido de pantalla
            addAction(Intent.ACTION_SCREEN_OFF) // Para apagado de pantalla
        }
        registerReceiver(activityReceiver, filter)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { resetInactivityTimer() }) // Reinicia el temporizador al tocar
                            }
                    ) {
                        PressToTalkButton { command ->
                            Log.d("MainActivity", "Recognized: $command")
                            speak(command) // Habla lo que se reconoció
                            resetInactivityTimer() // Reinicia el temporizador al reconocer
                        }
                        if (showDialog) {
                            AccessibilityPermissionDialog(onDismiss = { showDialog = false })
                        }
                    }
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service.name) ?: false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST_CODE)
        }
    }

    @Composable
    fun PressToTalkButton(onRecognized: (String) -> Unit) {
        var isRecording by remember { mutableStateOf(false) }
        var recognizedText by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
                .background(Color.Cyan)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            resetInactivityTimer() // Reinicia el temporizador al tocar
                            isRecording = true
                            startListening()
                            tryAwaitRelease() // Espera a que se suelte el dedo
                            isRecording = false
                            stopListening()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            BasicText(text = if (isRecording) "Hablando..." else "Presiona para hablar", color = Color.White)
        }

        // Configura el listener para el SpeechRecognizer
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Listo para hablar")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Comienzo del habla")
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "Fin del habla")
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error de reconocimiento: $error")
            }

            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches[0]
                    onRecognized(recognizedText) // Llama a la función de callback con el texto reconocido
                    resetInactivityTimer() // Reiniciar el temporizador al recibir resultados
                }
            }

            // Otros métodos de RecognitionListener pueden permanecer vacíos
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun BasicText(text: String, color: Color) {

    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
    }

    private fun speak(text: String) {
        if (text.isNotEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun resetInactivityTimer() {
        if (::inactivityTimer.isInitialized) {
            inactivityTimer.cancel() // Cancela el temporizador anterior
        }
        inactivityTimer = MyCountDownTimer(inactivityTimeout, 1000) // 10 segundos
        inactivityTimer.start() // Inicia el nuevo temporizador
    }

    @Composable
    fun AccessibilityPermissionDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permiso de Accesibilidad Requerido") },
            text = { Text("Esta aplicación requiere acceso a los servicios de accesibilidad para funcionar correctamente. Por favor, actívalo en la configuración de tu dispositivo.") },
            confirmButton = {
                Button(onClick = {
                    openAccessibilitySettings()
                    onDismiss()
                }) {
                    Text("Ir a Configuración")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy() // Libera los recursos del SpeechRecognizer
        textToSpeech.shutdown() // Libera los recursos del TextToSpeech
        unregisterReceiver(activityReceiver) // Desregistra el BroadcastReceiver
    }

    // BroadcastReceiver para detectar cambios en la configuración
    class ActivityReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context is MainActivity) {
                context.resetInactivityTimer() // Reinicia el temporizador al recibir un evento
            }
        }
    }

    inner class MyCountDownTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onFinish() {
            // Aquí se anuncia el cierre de la aplicación
            speakAndCloseApp("Cerrando aplicación")
        }

        override fun onTick(millisUntilFinished: Long) {
            // Puedes dejar este método vacío o usarlo para actualizar la interfaz de usuario
        }
    }

    private fun speakAndCloseApp(message: String) {
        // Usa corutinas para manejar el retraso
        CoroutineScope(Dispatchers.Main).launch {
            // Reproducir el mensaje
            speak(message)

            // Espera 2 segundos antes de cerrar la aplicación
            delay(2000)

            // Ahora cierra la aplicación
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Column {
            PressToTalkButton {}
        }
    }
}

@Composable
fun PressToTalkButton(content: () -> Unit) {

}
