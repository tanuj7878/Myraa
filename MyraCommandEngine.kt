package com.example.myra

import java.util.Locale

sealed class MyraAction {
    data class OpenApp(val appName: String) : MyraAction()
    data class CallNumber(val number: String) : MyraAction()
    data class CallContact(val name: String) : MyraAction()
    data class WhatsAppShare(val message: String) : MyraAction()
    data class SearchWeb(val query: String) : MyraAction()
    data class Reply(val text: String) : MyraAction()
}

object MyraCommandEngine {
    fun parse(input: String): MyraAction {
        val raw = input.trim()
        val s = raw.lowercase(Locale.ROOT)

        val appTriggers = listOf(
            "youtube" to listOf("youtube", "यूट्यूब"),
            "instagram" to listOf("instagram", "इंस्टाग्राम"),
            "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "व्हाट्स ऐप"),
            "chrome" to listOf("chrome", "क्रोम"),
            "camera" to listOf("camera", "कैमरा"),
            "settings" to listOf("settings", "सेटिंग")
        )
        for ((app, words) in appTriggers) {
            if (words.any { s.contains(it) } && listOf("खोल", "open", "चलाओ", "स्टार्ट").any { s.contains(it) }) {
                return MyraAction.OpenApp(app)
            }
        }

        val number = raw.filter { it.isDigit() || it == '+' }
        if ((s.startsWith("call ") || s.startsWith("कॉल ") || s.startsWith("फोन ")) &&
            number.count { it.isDigit() } >= 5) {
            return MyraAction.CallNumber(number)
        }

        val callPrefixes = listOf("call ", "कॉल ", "फोन लगाओ ", "फोन करो ")
        callPrefixes.firstOrNull { s.startsWith(it) }?.let { prefix ->
            val name = raw.drop(prefix.length).trim()
            if (name.isNotBlank() && name.any { it.isLetter() }) return MyraAction.CallContact(name)
        }

        val messagePrefixes = listOf("मैसेज भेजो ", "whatsapp message ", "व्हाट्सएप मैसेज ", "मैसेज ")
        messagePrefixes.firstOrNull { s.startsWith(it) }?.let { prefix ->
            val message = raw.drop(prefix.length).trim()
            if (message.isNotBlank()) return MyraAction.WhatsAppShare(message)
        }

        if (s.startsWith("search ") || s.startsWith("खोजो ")) {
            return MyraAction.SearchWeb(raw.substringAfter(' ').trim())
        }

        return MyraAction.Reply(
            "मैंने आपकी बात सुनी: $raw। अभी ऐप खोलना, नंबर/कॉन्टैक्ट कॉल करना, वेब सर्च और WhatsApp में मैसेज तैयार करना उपलब्ध है।"
        )
    }
}
