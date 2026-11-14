package com.justantis.pause.service

import com.justantis.pause.di.ApplicationScope
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.LimitRepository
import com.justantis.pause.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides what happens when an app comes to the front (spec §6.2), in this order:
 * 1. the app has unexpired time-boxed access → allow and check again when it expires;
 * 2. a Focus Mode session is running and the app is paused in Focus Mode → show the break screen;
 * 3. the app has a limit for today → show the break screen when it is used up, otherwise check
 *    again when the rest of it would be used up;
 * 4. otherwise do nothing.
 *
 * Every wait also ends at local midnight, when limits and counters roll over. The controller is
 * driven only by events (an app coming to the front, a change of limits or Focus Mode) and keeps
 * at most one pending timer: a new event cancels the previous check.
 */
@Singleton
class EnforcementController @Inject constructor(
    private val limitRepository: LimitRepository,
    private val focusRepository: FocusRepository,
    private val blockRepository: BlockRepository,
    private val usageRepository: UsageRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var launcher: BlockScreenLauncher? = null
    private var foregroundPackage: String? = null
    private var blockShownFor: String? = null
    private var checkJob: Job? = null
    private var changesJob: Job? = null

    fun start(launcher: BlockScreenLauncher) {
        this.launcher = launcher
        changesJob?.cancel()
        changesJob = scope.launch {
            combine(
                limitRepository.observeLimits(),
                focusRepository.observeRunningSession(),
                focusRepository.observeFocusApps()
            ) { _, _, _ -> }
                .drop(1)
                .collect { if (blockShownFor == null) check(foregroundPackage) }
        }
    }

    fun stop() {
        changesJob?.cancel()
        checkJob?.cancel()
        launcher = null
        foregroundPackage = null
        blockShownFor = null
    }

    fun onAppInForeground(packageName: String) {
        if (packageName == foregroundPackage) return
        foregroundPackage = packageName
        blockShownFor = null
        check(packageName)
    }

    /** The home screen or Pause itself is in front: nothing to enforce. */
    fun onNoAppInForeground() {
        foregroundPackage = null
        blockShownFor = null
        checkJob?.cancel()
    }

    private fun check(packageName: String?) {
        checkJob?.cancel()
        if (packageName == null) return
        checkJob = scope.launch {
            while (isActive) {
                when (val decision = decide(packageName)) {
                    Decision.Allow -> return@launch
                    is Decision.Block -> {
                        block(packageName, decision)
                        return@launch
                    }
                    is Decision.CheckAgainAt -> {
                        val waitMs = Duration.between(clock.instant(), decision.time).toMillis()
                        delay(waitMs.coerceAtLeast(MIN_WAIT_MS))
                    }
                }
            }
        }
    }

    private suspend fun decide(packageName: String): Decision {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val midnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()

        val grant = blockRepository.getActiveGrant(packageName)
        if (grant != null) return Decision.CheckAgainAt(minOf(grant.expiresAt, midnight))

        val session = focusRepository.getRunningSession()?.takeIf { now.isBefore(it.plannedEnd) }
        if (session != null && focusRepository.isFocusApp(packageName)) {
            return Decision.Block(BlockReason.FOCUS, session.id)
        }

        val limit = limitRepository.getLimit(packageName) ?: return Decision.Allow
        if (!limit.appliesOn(today)) return Decision.CheckAgainAt(midnight)

        val usedMs = usageRepository.usageOn(today).usageOf(packageName)?.foregroundMs ?: 0L
        val remainingMs = Duration.ofMinutes(limit.limitMinutes.toLong()).toMillis() - usedMs
        if (remainingMs <= 0) return Decision.Block(BlockReason.LIMIT, focusSessionId = null)
        return Decision.CheckAgainAt(minOf(now.plusMillis(remainingMs), midnight))
    }

    private suspend fun block(packageName: String, decision: Decision.Block) {
        val launcher = launcher ?: return
        val eventId = blockRepository.recordAttempt(packageName, decision.reason, decision.focusSessionId)
        blockShownFor = packageName
        launcher.showBlockScreen(packageName, eventId, decision.reason, decision.focusSessionId)
    }

    private sealed interface Decision {
        data object Allow : Decision
        data class Block(val reason: BlockReason, val focusSessionId: Long?) : Decision
        data class CheckAgainAt(val time: Instant) : Decision
    }

    private companion object {
        /** Avoids a busy loop when usage data lags behind the clock by a moment. */
        const val MIN_WAIT_MS = 1_000L
    }
}
