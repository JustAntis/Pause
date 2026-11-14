package com.justantis.pause.data.repository

import com.justantis.pause.data.local.dao.FocusAppDao
import com.justantis.pause.data.local.dao.FocusSessionDao
import com.justantis.pause.data.local.entity.FocusAppEntity
import com.justantis.pause.data.local.entity.FocusSessionEntity
import com.justantis.pause.data.local.toDomain
import com.justantis.pause.domain.model.FocusSession
import com.justantis.pause.domain.model.FocusSessionStatus
import com.justantis.pause.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class FocusRepositoryImpl @Inject constructor(
    private val focusAppDao: FocusAppDao,
    private val focusSessionDao: FocusSessionDao,
    private val clock: Clock
) : FocusRepository {

    override fun observeFocusApps(): Flow<Set<String>> = focusAppDao.observeAll().map { it.toSet() }

    override suspend fun isFocusApp(packageName: String): Boolean = focusAppDao.contains(packageName)

    override suspend fun setFocusApp(packageName: String, paused: Boolean) {
        if (paused) {
            focusAppDao.insert(listOf(FocusAppEntity(packageName)))
        } else {
            focusAppDao.delete(packageName)
        }
    }

    override suspend fun setFocusApps(packageNames: Set<String>) =
        focusAppDao.replaceAll(packageNames.map { FocusAppEntity(it) })

    override suspend fun startSession(plannedMinutes: Int): FocusSession {
        val entity = FocusSessionEntity(
            startedAt = clock.millis(),
            plannedMinutes = plannedMinutes,
            status = FocusSessionStatus.RUNNING
        )
        val id = focusSessionDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun getRunningSession(): FocusSession? = focusSessionDao.getRunning()?.toDomain()

    override fun observeRunningSession(): Flow<FocusSession?> =
        focusSessionDao.observeRunning().map { it?.toDomain() }

    override suspend fun markInterrupted(sessionId: Long) = focusSessionDao.updateStatusIf(
        id = sessionId,
        expected = FocusSessionStatus.RUNNING,
        status = FocusSessionStatus.INTERRUPTED
    )

    override suspend fun finishSession(sessionId: Long, status: FocusSessionStatus) =
        focusSessionDao.finish(sessionId, clock.millis(), status)

    override fun observeSessions(from: LocalDate, to: LocalDate): Flow<List<FocusSession>> {
        val start = from.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return focusSessionDao.observeStartedBetween(start, end).map { list -> list.map { it.toDomain() } }
    }
}
