package com.nova.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // Multi-step conversation (jaise alarm time poochna)
    private var pendingAction: String? = null

    // ===== NAYA: Undo/Cancel System =====
    private var lastAction: String? = null
    private var lastAlarmTime: String? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val requestExtraPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val appMap = listOf(
        listOf("gmail", "जीमेल") to "com.google.android.gm",
        listOf("google drive", "drive", "ड्राइव") to "com.google.android.apps.docs",
        listOf("google docs", "docs", "डॉक्स") to "com.google.android.apps.docs.editors.docs",
        listOf("whatsapp", "व्हाट्सएप") to "com.whatsapp",
        listOf("instagram", "इंस्टाग्राम") to "com.instagram.android",
        listOf("facebook", "फेसबुक") to "com.facebook.katana",
        listOf("chatgpt", "चैटजीपीटी") to "com.openai.chatgpt",
        listOf("gemini", "जेमिनी") to "com.google.android.apps.bard",
        listOf("claude", "क्लॉड") to "com.anthropic.claude",
        listOf("photos", "फोटोज़") to "com.google.android.apps.photos",
        listOf("notes", "नोट्स") to "com.google.android.keep",
        listOf("chrome", "क्रोम") to "com.android.chrome",
        listOf("youtube", "यूट्यूब") to "com.google.android.youtube",
        listOf("maps", "मैप्स") to "com.google.android.apps.maps",
        listOf("google", "गूगल") to "com.google.android.googlequicksearchbox"
    )

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
                    Text(text = "Hello, I am NOVA AI 🤖", fontSize = 22.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasContacts = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCall = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED

                        when {
                            !hasMic -> requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                            !hasContacts || !hasCall -> requestExtraPermissions.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
                            )
                            else -> {
                                isListening = true
                                startListening(
                                    onResult = { text ->
                                        recognizedText = text
                                        isListening = false
                                        handleCommand(text)
                                    },
                                    onError = {
                                        recognizedText = "Samajh nahi aaya, phir try karo"
                                        isListening = false
                                    }
                                )
                            }
                        }
                    }) {
                        Text(if (isListening) "Sun raha hoon..." else "Listen")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    }) {
                        Text("Enable Floating Button")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = recognizedText, fontSize = 16.sp)
                }
            }
        }
    }

    // ================= MAIN COMMAND HANDLER =================

    private fun handleCommand(command: String) {
        val lower = command.trim().lowercase()

        // --- Priority 1: Agar pehle se koi sawal poocha tha (jaise alarm time) ---
        if (pendingAction == "alarm_time") {
            pendingAction = null
            setAlarmFromText(lower)
            return
        }

        // --- Priority 2: CANCEL / UNDO COMMAND ---
        val isCancelCommand = lower.contains("cancel") || lower.contains("रद्द") ||
                lower.contains("undo") || lower.contains("वापस") ||
                lower.contains("band karo") || lower.contains("बंद करो") ||
                lower.contains("bahar niklo") || lower.contains("बाहर निकलो")

        if (isCancelCommand) {
            when (lastAction) {
                "alarm" -> {
                    speak("अलार्म की लिस्ट खोल रहा हूं, वहां से डिलीट कर दीजिए")
                    startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }
                "app_open" -> {
                    speak("ठीक है, वापस होम स्क्रीन पर जा रहा हूं")
                    startActivity(Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
                else -> {
                    speak("मुझे याद नहीं कि आखिरी काम क्या था, इसलिए मैं उसे रद्द नहीं कर सकता")
                }
            }
            lastAction = null
            return
        }

        // --- Alarm: agar time nahi bola, toh poochna hai ---
        if ((lower.contains("alarm") || lower.contains("अलार्म")) && !Regex("\\d").containsMatchIn(lower)) {
            speak("कितने बजे का अलार्म लगाना है?")
            pendingAction = "alarm_time"
            return
        }
        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            setAlarmFromText(lower)
            return
        }

        // --- Call ---
        Regex("(.+?)\\s+(?:ko|को)\\s+(?:call|कॉल)\\s+(?:karo|करो)").find(lower)?.let {
            makeCall(it.groupValues[1].trim())
            return
        }

        // --- WhatsApp ---
        Regex("(.+?)\\s+(?:ko|को)\\s+(?:whatsapp|व्हाट्सएप)\\s+(?:par|पर|pe)?\\s*(?:message|मैसेज)?\\s*(?:karo|करो|bhejo|भेजो)\\s+(.+)").find(lower)?.let {
            sendWhatsAppMessage(it.groupValues[1].trim(), it.groupValues[2].trim())
            return
        }

        // --- Email ---
        Regex("(.+?)\\s+(?:ko|को)\\s+email\\s+(?:bhejo|भेजो)\\s+subject\\s+(.+?)\\s+body\\s+(.+)").find(lower)?.let {
            sendEmail(it.groupValues[1].trim(), it.groupValues[2].trim(), it.groupValues[3].trim())
            return
        }

        // --- Maps ---
        Regex("(.+?)\\s+(?:ka|की|के)?\\s*(?:rasta|रास्ता)\\s+(?:dikhao|दिखाओ)").find(lower)?.let {
            showRoute(it.groupValues[1].trim())
            return
        }

        // --- YouTube search ---
        Regex("youtube\\s+(?:par|पर)\\s+(.+?)\\s+(?:dhundo|ढूंढो|search karo)").find(lower)?.let {
            searchYouTube(it.groupValues[1].trim())
            return
        }

        // --- Web search ---
        Regex("(?:chrome|google|गूगल)\\s+(?:par|पर)\\s+(.+?)\\s+(?:search karo|dhundo|ढूंढो|खोजो)").find(lower)?.let {
            searchWeb(it.groupValues[1].trim())
            return
        }

        // --- Instagram profile ---
        Regex("instagram\\s+(?:par|पर)\\s+(.+?)\\s+(?:dhundo|ढूंढो)").find(lower)?.let {
            openInstagramProfile(it.groupValues[1].trim())
            return
        }

        // --- Facebook search ---
        Regex("facebook\\s+(?:par|पर)\\s+(.+?)\\s+(?:search karo|dhundo)").find(lower)?.let {
            searchFacebook(it.groupValues[1].trim())
            return
        }

        // --- Notes ---
        Regex("note\\s+(?:me|में)\\s+(?:likho|लिखो)\\s+(.+)").find(lower)?.let {
            createNote(it.groupValues[1].trim())
            return
        }

        // --- Camera: photo / video ---
        if (lower.contains("photo") || lower.contains("फोटो")) {
            speak("फोटो के लिए कैमरा खोल रहा हूं")
            startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            lastAction = "app_open"
            return
        }
        if (lower.contains("video") || lower.contains("वीडियो")) {
            speak("वीडियो के लिए कैमरा खोल रहा हूं")
            startActivity(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
            lastAction = "app_open"
            return
        }

        // --- Universal simple apps ---
        when {
            lower.contains("phone") || lower.contains("dialer") || lower.contains("फोन") -> {
                speak("फोन खोल रहा हूं")
                startActivity(Intent(Intent.ACTION_DIAL))
                lastAction = "app_open"; return
            }
            lower.contains("contact") || lower.contains("कॉन्टैक्ट") -> {
                speak("कॉन्टैक्ट्स खोल रहा हूं")
                startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                lastAction = "app_open"; return
            }
            lower.contains("message") || lower.contains("मैसेज") || lower.contains("sms") -> {
                speak("मैसेज खोल रहा हूं")
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
                lastAction = "app_open"; return
            }
            lower.contains("camera") || lower.contains("कैमरा") -> {
                speak("कैमरा खोल रहा हूं")
                startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                lastAction = "app_open"; return
            }
            lower.contains("gallery") || lower.contains("गैलरी") -> {
                speak("गैलरी खोल रहा हूं")
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY))
                lastAction = "app_open"; return
            }
            lower.contains("clock") || lower.contains("घड़ी") -> {
                speak("क्लॉक खोल रहा हूं")
                startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                lastAction = "app_open"; return
            }
        }

        // --- Baaki apps: package list se ---
        for ((keywords, pkg) in appMap) {
            if (keywords.any { lower.contains(it) }) {
                openApp(pkg, keywords.first())
                return
            }
        }

        speak("माफ कीजिए, मुझे यह समझ नहीं आया")
    }

    // ================= HELPER FUNCTIONS =================

    private fun openApp(packageName: String, label: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            speak("$label खोल रहा हूं")
            startActivity(launchIntent)
            lastAction = "app_open"
        } else {
            speak("यह ऐप installed नहीं है")
        }
    }

    private fun findContactNumber(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val contactName = cursor.getString(nameIdx) ?: continue
                if (contactName.contains(name, ignoreCase = true)) {
                    return cursor.getString(numberIdx)
                }
            }
        }
        return null
    }

    private fun findContactEmail(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val contactName = cursor.getString(nameIdx) ?: continue
                if (contactName.contains(name, ignoreCase = true)) {
                    return cursor.getString(emailIdx)
                }
            }
        }
        return null
    }

    private fun makeCall(name: String) {
        val number = findContactNumber(name)
        if (number != null) {
            speak("$name को कॉल कर रहा हूं")
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            lastAction = "app_open"
        } else {
            speak("मुझे $name नाम का contact नहीं मिला")
        }
    }

    private fun sendWhatsAppMessage(name: String, message: String) {
        val number = findContactNumber(name)
        if (number != null) {
            var cleanNumber = number.replace(Regex("[^0-9+]"), "")
            if (!cleanNumber.startsWith("+")) cleanNumber = "+91$cleanNumber"
            speak("$name के लिए WhatsApp खोल रहा हूं")
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
            })
            lastAction = "app_open"
        } else {
            speak("मुझे $name नाम का contact नहीं मिला")
        }
    }

    private fun sendEmail(name: String, subject: String, body: String) {
        val email = findContactEmail(name)
        speak("Email तैयार कर रहा हूं")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            if (email != null) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            lastAction = "app_open"
        }
    }

    private fun showRoute(location: String) {
        speak("$location का रास्ता दिखा रहा हूं")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${Uri.encode(location)}")
        )
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            lastAction = "app_open"
        }
    }

    private fun searchYouTube(query: String) {
        speak("YouTube पर $query ढूंढ रहा हूं")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")))
        lastAction = "app_open"
    }

    private fun searchWeb(query: String) {
        speak("$query सर्च कर रहा हूं")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        lastAction = "app_open"
    }

    private fun openInstagramProfile(username: String) {
        speak("Instagram पर $username खोल रहा हूं")
        val clean = username.replace(" ", "")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/$clean")))
        lastAction = "app_open"
    }

    private fun searchFacebook(query: String) {
        speak("Facebook पर $query सर्च कर रहा हूं")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/search/top/?q=${Uri.encode(query)}")))
        lastAction = "app_open"
    }

    private fun createNote(text: String) {
        speak("Note बना रहा हूं")
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.google.android.keep")
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            lastAction = "app_open"
        } else {
            speak("Notes ऐप नहीं मिला")
        }
    }

    private fun setAlarmFromText(text: String) {
        val hourMatch = Regex("(\\d{1,2})(?::(\\d{2}))?").find(text)
        if (hourMatch == null) {
            speak("मुझे समय समझ नहीं आया")
            return
        }
        var hour = hourMatch.groupValues[1].toInt()
        val minute = hourMatch.groupValues[2].ifEmpty { "0" }.toInt()

        if ((text.contains("shaam") || text.contains("शाम") || text.contains("raat") || text.contains("रात")) && hour < 12) {
            hour += 12
        }

        speak("ठीक है, $hour बजकर $minute मिनट का अलार्म लगा दिया")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            lastAction = "alarm"
            lastAlarmTime = "$hour:$minute"
        }
    }

    // ================= SPEECH ENGINE =================

    private fun startListening(onResult: (String) -> Unit, onError: () -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onResult(matches?.firstOrNull() ?: "Kuch samajh nahi aaya")
            }
            override fun onError(error: Int) { onError() }
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
