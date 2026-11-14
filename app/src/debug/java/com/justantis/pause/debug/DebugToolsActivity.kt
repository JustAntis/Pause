package com.justantis.pause.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.presentation.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug builds only. Opened from the launcher shortcut "Seed demo history" (long-press the app
 * icon) or with `adb shell am start -n com.justantis.pause/.debug.DebugToolsActivity`.
 */
@AndroidEntryPoint
class DebugToolsActivity : ComponentActivity() {

    @Inject
    lateinit var seeder: DemoDataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PauseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    var status by remember { mutableStateOf("") }
                    var running by remember { mutableStateOf(false) }
                    val doneText = stringResource(R.string.debug_seed_done)

                    Column(
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.debug_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = stringResource(R.string.debug_seed_explanation), style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = {
                                running = true
                                scope.launch {
                                    val days = seeder.seed()
                                    status = doneText.format(days)
                                    running = false
                                }
                            },
                            enabled = !running,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = stringResource(R.string.debug_seed_button))
                        }
                        Text(text = status, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
