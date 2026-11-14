package com.justantis.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.justantis.pause.presentation.navigation.OpenTarget
import com.justantis.pause.presentation.navigation.PauseAppContent
import com.justantis.pause.presentation.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openTarget by mutableStateOf<OpenTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openTarget = OpenTarget.from(intent)

        setContent {
            PauseTheme {
                PauseAppContent(
                    openTarget = openTarget,
                    onOpenTargetHandled = { openTarget = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTarget = OpenTarget.from(intent)
    }
}
