package com.justantis.pause.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.justantis.pause.data.local.dao.AccessGrantDao
import com.justantis.pause.data.local.dao.BlockEventDao
import com.justantis.pause.data.local.dao.DailyUsageDao
import com.justantis.pause.data.local.dao.FocusAppDao
import com.justantis.pause.data.local.dao.FocusSessionDao
import com.justantis.pause.data.local.dao.LimitDao
import com.justantis.pause.data.local.entity.AccessGrantEntity
import com.justantis.pause.data.local.entity.AppLimitEntity
import com.justantis.pause.data.local.entity.BlockEventEntity
import com.justantis.pause.data.local.entity.DailyAppUsageEntity
import com.justantis.pause.data.local.entity.DailySummaryEntity
import com.justantis.pause.data.local.entity.FocusAppEntity
import com.justantis.pause.data.local.entity.FocusSessionEntity

@Database(
    entities = [
        AppLimitEntity::class,
        FocusAppEntity::class,
        DailyAppUsageEntity::class,
        DailySummaryEntity::class,
        BlockEventEntity::class,
        AccessGrantEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PauseDatabase : RoomDatabase() {
    abstract fun limitDao(): LimitDao
    abstract fun focusAppDao(): FocusAppDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun accessGrantDao(): AccessGrantDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val NAME = "pause.db"
    }
}
