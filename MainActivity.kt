package com.example.myra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(1.08f)
            }
        }
        setContent { MyraScreen(onSpeak = { speak(it) }) }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "myra_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun MyraScreen(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("नमस्ते! मैं MYRA हूँ। बोलिए, क्या करना है?") }
    var command by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var pendingContactCall by remember { mutableStateOf<String?>(null) }
    var showConfirmCall by remember { mutableStateOf(false) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening(context) { command = it; listening = false; transcript = "सुना: $it" }
        else { transcript = "माइक्रोफ़ोन अनुमति नहीं मिली।"; listening = false }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val name = pendingContactCall
        if (granted && name != null) {
            val number = findContactNumber(context, name)
            if (number != null) {
                pendingContactCall = number
                showConfirmCall = true
            } else transcript = "$name नाम का कॉन्टैक्ट नहीं मिला।"
        } else transcript = "कॉन्टैक्ट अनुमति नहीं मिली।"
    }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingContactCall
        if (number != null) {
            val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
            context.startActivity(Intent(action, Uri.parse("tel:$number")))
            pendingContactCall = null
        }
    }

    fun runCommand(text: String) {
        when (val action = MyraCommandEngine.parse(text)) {
            is MyraAction.OpenApp -> {
                val pkg = mapOf(
                    "youtube" to "com.google.android.youtube",
                    "instagram" to "com.instagram.android",
                    "whatsapp" to "com.whatsapp",
                    "chrome" to "com.android.chrome",
                    "camera" to "com.android.camera2",
                    "settings" to "com.android.settings"
                )[action.appName]
                val launch = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
                if (launch != null) {
                    context.startActivity(launch)
                    transcript = "${action.appName} खोल रही हूँ।"
                } else transcript = "${action.appName} इस फोन में नहीं मिला।"
            }
            is MyraAction.CallNumber -> {
                pendingContactCall = action.number
                showConfirmCall = true
                transcript = "कॉल करने से पहले नंबर जाँच लें: ${action.number}"
            }
            is MyraAction.CallContact -> {
                pendingContactCall = action.name
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val number = findContactNumber(context, action.name)
                    if (number != null) { pendingContactCall = number; showConfirmCall = true; transcript = "${action.name}: $number — कॉल की पुष्टि करें।" }
                    else transcript = "${action.name} नाम का कॉन्टैक्ट नहीं मिला।"
                } else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
            }
            is MyraAction.WhatsAppShare -> {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, action.message)
                    setPackage("com.whatsapp")
                }
                try {
                    context.startActivity(send)
                    transcript = "WhatsApp में मैसेज तैयार है। व्यक्ति चुनकर खुद Send दबाएँ।"
                } catch (_: Exception) { transcript = "WhatsApp उपलब्ध नहीं है।" }
            }
            is MyraAction.SearchWeb -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(action.query))))
                transcript = "वेब पर खोज रही हूँ: ${action.query}"
            }
            is MyraAction.Reply -> transcript = action.text
        }
        onSpeak(transcript)
    }

    if (showConfirmCall) {
        AlertDialog(
            onDismissRequest = { showConfirmCall = false },
            title = { Text("कॉल की पुष्टि करें") },
            text = { Text("क्या आप ${pendingContactCall ?: ""} पर कॉल करना चाहते हैं?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmCall = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        val n = pendingContactCall
                        if (n != null) context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$n")))
                    } else callPermission.launch(Manifest.permission.CALL_PHONE)
                }) { Text("कॉल करें") }
            },
            dismissButton = { TextButton(onClick = { showConfirmCall = false }) { Text("रद्द करें") } }
        )
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFB9A7FF), background = Color(0xFF090B18), surface = Color(0xFF171A2D)
    )) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF171B3A), Color(0xFF080A14)))
            ).padding(22.dp)
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(22.dp))
                Text("MYRA", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("YOUR PERSONAL AI ASSISTANT", color = Color(0xFFB9A7FF), fontSize = 11.sp)
                Spacer(Modifier.height(30.dp))
                Box(
                    Modifier.size(174.dp).background(
                        Brush.linearGradient(listOf(Color(0xFF7658D8), Color(0xFF20B8C7))), CircleShape
                    ), contentAlignment = Alignment.Center
                ) { Text("M", fontSize = 76.sp, fontWeight = FontWeight.Light, color = Color.White) }
                Spacer(Modifier.height(24.dp))
                Text(if (listening) "सुन रही हूँ…" else "आपकी मदद के लिए तैयार", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF191D33)), shape = RoundedCornerShape(18.dp)) {
                    Text(transcript, Modifier.padding(16.dp).fillMaxWidth(), color = Color(0xFFE7E5F5))
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    label = { Text("कमांड लिखें या बोलें") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = { runCommand(command) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Send, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("कमांड चलाएँ")
                }
                Button(onClick = {
                    listening = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startListening(context) { command = it; transcript = "सुना: $it"; listening = false }
                    } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("बोलकर कमांड दें")
                }
                Spacer(Modifier.height(12.dp))
                Text("Try: YouTube खोलो • कॉल राहुल • मैसेज नमस्ते • खोजो मौसम", color = Color(0xFFB4B6CC), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("MYRA v1.0 • Hindi voice • on-device actions", color = Color(0xFF8589A8), fontSize = 11.sp)
            }
        }
    }
}

private fun findContactNumber(context: android.content.Context, name: String): String? {
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    context.contentResolver.query(
        uri, projection, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
        arrayOf("%$name%"), "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return null
}

private fun startListening(context: android.content.Context, onResult: (String) -> Unit) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onResult("इस डिवाइस पर speech recognition उपलब्ध नहीं है।")
        return
    }
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) { onResult("आवाज़ समझ नहीं आई। फिर कोशिश करें।"); recognizer.destroy() }
        override fun onResults(results: Bundle?) {
            onResult(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: "मैं समझ नहीं पाई।")
            recognizer.destroy()
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
    }
    recognizer.startListening(intent)
}
