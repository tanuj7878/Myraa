package com.example.myra

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 3 safety scaffold.
 * User must explicitly enable this service in Android Settings.
 * This initial version intentionally does not inspect screen text,
 * click UI controls, or send messages automatically.
 */
class MyraAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Reserved for narrowly scoped, user-approved automation in a later iteration.
    }

    override fun onInterrupt() = Unit
}
