# MYRA Android — Phase 1–4 starter bundle

This is a native Android Studio source project (Kotlin + Jetpack Compose), version 1.0.0.

## Included in this combined delivery

### Phase 1 — Basic app
- MYRA home screen and command input
- Hindi speech recognition via Android SpeechRecognizer
- Hindi text-to-speech via Android TextToSpeech

### Phase 2 — Assistant brain (starter)
- Hindi/Hinglish keyword intent parser
- Routes recognized requests to app launch, call, WhatsApp share, web search, or fallback response
- This is a local rule-based starter brain, not a connected large-language-model service yet

### Phase 3 — Phone actions (starter)
- Launches selected apps (YouTube, Instagram, WhatsApp, Chrome, Camera, Settings)
- Searches contacts by display name after runtime permission
- Requires confirmation before calling a contact or number
- Opens WhatsApp share composer with message text; user chooses recipient and taps Send
- Optional AccessibilityService is declared but intentionally does not read screen contents or click controls

### Phase 4 — App packaging foundation
- Android app identity, launcher activity, app label, Compose UI, version 1.0.0
- Build instructions below to produce a debug APK on your computer

## Build an APK
1. Install Android Studio (stable) and Android SDK Platform 35.
2. Extract this ZIP.
3. Open the `MYRA-Android` folder in Android Studio.
4. Wait for Gradle sync; install any requested SDK components.
5. Connect an Android phone with USB debugging enabled, or use an emulator.
6. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. Android Studio will show the APK location, typically `app/build/outputs/apk/debug/app-debug.apk`.

This ZIP is source code, not a compiled or signed APK. A real release APK must be built and signed with your own release key.

## Example commands
- `YouTube खोलो`
- `Instagram खोलो`
- `WhatsApp खोलो`
- `कॉल 9876543210`
- `कॉल राहुल`
- `मैसेज नमस्ते, मैं थोड़ी देर में पहुँचूँगा`
- `खोजो आज का मौसम`

## Important permissions and safety
- Microphone permission is requested when you tap the voice button.
- Contacts permission is requested only for contact-name calling.
- Call permission is requested only when you confirm a call. If denied, extend the app to fall back to the dialer.
- Messages are never silently sent. WhatsApp opens its share composer; you review the recipient and press Send.
- Accessibility access is not automatically enabled. The service is a deliberately inactive scaffold and currently does not inspect or operate other apps.
- Android restricts background microphone use and cross-app control. Do not attempt to bypass Android permission prompts or accessibility consent.

## What remains before a production-grade AI assistant
- Connect an AI backend (API key must live on a secure server, never in the APK).
- Add robust multilingual speech handling, conversation state, and explicit action confirmation.
- Implement only specific accessibility actions with clear user consent, testing, and Play policy review.
- Add app icon assets, privacy policy, crash reporting, tests, and release signing.
