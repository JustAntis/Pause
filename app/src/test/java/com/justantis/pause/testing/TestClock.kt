package com.justantis.pause.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * A clock that moves together with the virtual time of coroutine tests, so `delay` and
 * `clock.instant()` stay consistent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId
) : Clock() {
    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = TestClock(scheduler, start, zone)
}
