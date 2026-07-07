package com.nova.ai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                var voiceList by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
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
                        val voices = tts?.voices?.filter { it.locale.language == "hi" }
                        voiceList = voices?.joinToString("\n") { v ->
                            "${v.name} | quality=${v.quality} | gender_hint=${
                                when {
                                    v.name.contains("male", true) -> "male"
                                    v.name.contains("female", true) -> "female"
                                    else -> "unknown"
                                }
                            }"
                        } ?: "No Hindi voices found"
                    }) {
                        Text("List Hindi Voices")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = voiceList, fontSize = 12.sp)
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
