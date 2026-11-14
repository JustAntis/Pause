package com.justantis.pause.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * One time the break screen was shown (an "attempt"). When the user went through the pause and
 * chose a purpose, [outcome] is BYPASSED and the access details are filled in.
 */
data class BlockEvent(
    val id: Long,
    val timestamp: Instant,
    val date: LocalDate,
    val packageName: String,
    val reason: BlockReason,
    val outcome: BlockOutcome,
    val purpose: AccessPurpose?,
    val grantedMinutes: Int?,
    val pauseSeconds: Int?,
    val focusSessionId: Long?
)

/** Why the break screen was shown: a used-up app limit or a running Focus Mode session. */
enum class BlockReason { LIMIT, FOCUS }

/** CLOSED: the user left the app. BYPASSED: the user was granted time-boxed access. */
enum class BlockOutcome { CLOSED, BYPASSED }

/** The reasons a user can give for opening an app after its limit is used up. */
enum class AccessPurpose { REPLY_TO_MESSAGE, CHECK_SOMETHING, WORK_OR_STUDY, OTHER }
