package com.nova.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Permission denied - user ko baad me phir se try karna hoga
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                val selectedVoice = tts?.voices?.firstOrNull { it.name == "hi-in-x-hie-network" }
                selectedVoice?.let { tts?.voice = it }
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                var recognizedText by remember { mutableStateOf("Kuch bolne ke liye 'Listen' dabao") }
                var isListening by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Hello, I am NOVA AI 🤖",
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        speak("नमस्ते, मैं नोवा एआई हूं। मैं आपकी कैसे मदद कर सकता हूं?")
                    }) {
                        Text("Speak")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            isListening = true
                            startListening(
                                onResult = { text ->
                                    recognizedText = text
                                    isListening = false
                                },
                                onError = {
                                    recognizedText = "Samajh nahi aaya, phir try karo"
                                    isListening = false
                                }
                            )
                        }
                    }) {
                        Text(if (isListening) "Sun raha hoon..." else "Listen")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = recognizedText, fontSize = 16.sp)
                }
            }
        }
    }

    private fun startListening(onResult: (String) -> Unit, onError: () -> Unit) {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onResult(matches?.firstOrNull() ?: "Kuch samajh nahi aaya")
            }

            override fun onError(error: Int) {
                onError()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
