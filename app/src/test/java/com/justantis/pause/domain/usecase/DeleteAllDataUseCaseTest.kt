package com.justantis.pause.domain.usecase

import com.justantis.pause.domain.repository.DataWiper
import com.justantis.pause.testing.FakeNotifier
import com.justantis.pause.testing.FakeWorkScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAllDataUseCaseTest {

    private class FakeWiper : DataWiper {
        var wiped = false

        override suspend fun wipe() {
            wiped = true
        }
    }

    @Test
    fun `deleting stops background work, removes notifications and wipes the stored data`() = runTest {
        val wiper = FakeWiper()
        val scheduler = FakeWorkScheduler().apply { scheduleUsageSync(); scheduleWeeklySummary() }
        val notifier = FakeNotifier()

        DeleteAllDataUseCase(wiper, scheduler, notifier)()

        assertTrue(wiper.wiped)
        assertTrue(scheduler.cancelledAll)
        assertTrue(notifier.clearedAll)
    }
}
