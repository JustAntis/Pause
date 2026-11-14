package com.justantis.pause.data.repository

import com.justantis.pause.data.local.dao.AccessGrantDao
import com.justantis.pause.data.local.dao.BlockEventDao
import com.justantis.pause.data.local.entity.AccessGrantEntity
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.data.local.toDomain
import com.justantis.pause.domain.model.AccessGrant
import com.justantis.pause.domain.model.AccessPurpose
import com.justantis.pause.domain.model.BlockEvent
import com.justantis.pause.domain.model.BlockOutcome
import com.justantis.pause.domain.model.BlockReason
import com.justantis.pause.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class BlockRepositoryImpl @Inject constructor(
    private val blockEventDao: BlockEventDao,
    private val accessGrantDao: AccessGrantDao,
    private val clock: Clock
) : BlockRepository {

    override suspend fun recordAttempt(
        packageName: String,
        reason: BlockReason,
        focusSessionId: Long?
    ): Long = blockEventDao.insert(
        BlockEventEntity(
            timestamp = clock.millis(),
            date = LocalDate.now(clock),
            packageName = packageName,
            reason = reason,
            outcome = BlockOutcome.CLOSED,
            focusSessionId = focusSessionId
        )
    )

    override suspend fun recordBypass(
        eventId: Long,
        purpose: AccessPurpose,
        grantedMinutes: Int,
        pauseSeconds: Int
    ) = blockEventDao.updateOutcome(
        id = eventId,
        outcome = BlockOutcome.BYPASSED,
        purpose = purpose,
        grantedMinutes = grantedMinutes,
        pauseSeconds = pauseSeconds
    )

    override suspend fun getEvent(eventId: Long): BlockEvent? = blockEventDao.get(eventId)?.toDomain()

    override suspend fun countBypasses(date: LocalDate): Int =
        blockEventDao.countWithOutcome(date, BlockOutcome.BYPASSED)

    override fun observeBypassCount(date: LocalDate): Flow<Int> =
        blockEventDao.observeCountWithOutcome(date, BlockOutcome.BYPASSED)

    override fun observeAttemptCount(date: LocalDate, packageName: String): Flow<Int> =
        blockEventDao.observeAttemptCount(date, packageName)

    override fun observeEvents(from: LocalDate, to: LocalDate): Flow<List<BlockEvent>> =
        blockEventDao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun grantAccess(packageName: String, expiresAt: Instant) {
        accessGrantDao.deleteExpired(clock.millis())
        accessGrantDao.upsert(AccessGrantEntity(packageName, expiresAt.toEpochMilli()))
    }

    override suspend fun getActiveGrant(packageName: String): AccessGrant? =
        accessGrantDao.get(packageName)?.toDomain()?.takeIf { it.isActiveAt(clock.instant()) }
}
