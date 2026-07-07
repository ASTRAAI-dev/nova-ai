package com.nova.ai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
                tts?.setPitch(0.7f)
                setMaleVoice()
            }
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
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
                }
            }
        }
    }

    private fun setMaleVoice() {
        val voices: Set<Voice>? = tts?.voices
        val maleVoice = voices?.firstOrNull {
            it.name.contains("male", ignoreCase = true) &&
            !it.name.contains("female", ignoreCase = true)
        }
        maleVoice?.let { tts?.voice = it }
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
