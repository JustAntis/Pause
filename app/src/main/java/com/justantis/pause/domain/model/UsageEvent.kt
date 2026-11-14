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
 * FOREGROUND and BACKGROUND mark an app coming to the front and leaving it, UNLOCK is a screen
 * unlock, SCREEN_OFF and SHUTDOWN end whatever was in the foreground.
 */
enum class UsageEventType { FOREGROUND, BACKGROUND, UNLOCK, SCREEN_OFF, SHUTDOWN }
