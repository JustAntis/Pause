package com.justantis.pause.presentation.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.justantis.pause.presentation.theme.PauseTheme

/** Collects the intents a screen sends; pass it directly as `onIntent`. */
class IntentRecorder<T> : (T) -> Unit {
    val intents: MutableList<T> = mutableListOf()

    override fun invoke(intent: T) {
        intents += intent
    }

    fun last(): T = intents.last()
}

/** Sets [content] wrapped in the app theme. */
fun ComposeContentTestRule.setScreen(content: @Composable () -> Unit) {
    setContent { PauseTheme { content() } }
}
