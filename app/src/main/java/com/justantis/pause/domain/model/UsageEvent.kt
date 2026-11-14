package com.justantis.pause.domain.model

/**
 * One system usage event reduced to what the app needs: which package, when and what happened.
 * [timestamp] is in epoch milliseconds.
 */
data class UsageEvent(
    val packageName: String,
    val timestamp: Long,
    val type: UsageEventType
)

/**
 * FOREGROUND and BACKGROUND mark an app coming to the front and leaving it. UNLOCK is the lock
 * screen being dismissed and KEYGUARD_SHOWN the lock screen appearing; SCREEN_ON is the screen
 * turning on. SCREEN_OFF and SHUTDOWN end whatever was in the foreground.
 */
enum class UsageEventType { FOREGROUND, BACKGROUND, UNLOCK, KEYGUARD_SHOWN, SCREEN_ON, SCREEN_OFF, SHUTDOWN }
