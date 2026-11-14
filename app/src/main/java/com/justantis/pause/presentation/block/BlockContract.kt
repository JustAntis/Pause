package com.justantis.pause.presentation.block

import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.streak.BypassConsequence

data class BlockState(
    val isLoading: Boolean = true,
    val packageName: String = "",
    val appLabel: String = "",
    val reason: BlockReason = BlockReason.LIMIT,
    val limitMinutes: Int? = null,
    val usedMinutes: Int = 0,
    val focusMinutesLeft: Int? = null,
    val attemptsToday: Int = 0,
    val streakDays: Int = 0,
    val freezes: Int = 0,
    val maxFreezes: Int = 2,
    val accessOrdinal: Int = 1,
    val pauseSeconds: Int = 0,
    val pauseSecondsLeft: Int = 0,
    val isPauseFinished: Boolean = false,
    val purposes: List<AccessPurpose> = emptyList(),
    val accessMinutesOptions: List<Int> = emptyList(),
    val selectedPurpose: AccessPurpose? = null,
    val selectedMinutes: Int? = null,
    val consequence: BypassConsequence = BypassConsequence.NONE,
    val isGranting: Boolean = false
) {
    val canContinue: Boolean
        get() = isPauseFinished

    val canConfirm: Boolean
        get() = selectedPurpose != null && selectedMinutes != null && !isGranting && !isLoading
}

sealed interface BlockIntent {
    data object CloseClicked : BlockIntent
    data object NeedAccessClicked : BlockIntent
    data object PauseStarted : BlockIntent
    data object NextClicked : BlockIntent
    data class PurposeSelected(val purpose: AccessPurpose) : BlockIntent
    data class MinutesSelected(val minutes: Int) : BlockIntent
    data object ConfirmClicked : BlockIntent
    data object BackToStartClicked : BlockIntent

    /** The user left the break screen (home, recents, screen off) without deciding. */
    data object FlowInterrupted : BlockIntent
}

sealed interface BlockEffect {
    data object GoHome : BlockEffect
    data object OpenPause : BlockEffect
    data object OpenPurpose : BlockEffect
    data object BackToStart : BlockEffect
    data class ReturnToApp(val packageName: String) : BlockEffect
}
