package com.justantis.pause.presentation.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.presentation.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The break screen shown over an app whose limit is used up or that is paused during Focus Mode.
 * It lives in its own task, is excluded from recents and never shows the bottom bar.
 */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    private var eventId by mutableLongStateOf(0L)
    private var currentViewModel: BlockViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        eventId = intent.getLongExtra(BlockViewModel.ARG_EVENT_ID, 0L)

        setContent {
            PauseTheme {
                Surface {
                    key(eventId) {
                        val viewModel = hiltViewModel<BlockViewModel>(key = "block-$eventId")
                        SideEffect { currentViewModel = viewModel }
                        BlockFlow(
                            viewModel = viewModel,
                            onGoHome = ::goHome,
                            onReturnToApp = ::returnToApp
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        eventId = intent.getLongExtra(BlockViewModel.ARG_EVENT_ID, 0L)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) {
            currentViewModel?.onIntent(BlockIntent.FlowInterrupted)
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun returnToApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    companion object {
        fun intent(
            context: Context,
            packageName: String,
            eventId: Long,
            reason: BlockReason,
            focusSessionId: Long?
        ): Intent = Intent(context, BlockActivity::class.java)
            .putExtra(BlockViewModel.ARG_PACKAGE_NAME, packageName)
            .putExtra(BlockViewModel.ARG_EVENT_ID, eventId)
            .putExtra(BlockViewModel.ARG_REASON, reason.name)
            .putExtra(BlockViewModel.ARG_FOCUS_SESSION_ID, focusSessionId ?: BlockViewModel.NO_FOCUS_SESSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
