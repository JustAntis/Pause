package com.justantis.pause.di

import android.content.Context
import androidx.room.Room
import com.justantis.pause.data.local.PauseDatabase
import com.justantis.pause.data.local.dao.AccessGrantDao
import com.justantis.pause.data.local.dao.BlockEventDao
import com.justantis.pause.data.local.dao.DailyUsageDao
import com.justantis.pause.data.local.dao.FocusAppDao
import com.justantis.pause.data.local.dao.FocusSessionDao
import com.justantis.pause.data.local.dao.LimitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PauseDatabase =
        Room.databaseBuilder(context, PauseDatabase::class.java, PauseDatabase.NAME).build()

    @Provides
    fun provideLimitDao(database: PauseDatabase): LimitDao = database.limitDao()

    @Provides
    fun provideFocusAppDao(database: PauseDatabase): FocusAppDao = database.focusAppDao()

    @Provides
    fun provideDailyUsageDao(database: PauseDatabase): DailyUsageDao = database.dailyUsageDao()

    @Provides
    fun provideBlockEventDao(database: PauseDatabase): BlockEventDao = database.blockEventDao()

    @Provides
    fun provideAccessGrantDao(database: PauseDatabase): AccessGrantDao = database.accessGrantDao()

    @Provides
    fun provideFocusSessionDao(database: PauseDatabase): FocusSessionDao = database.focusSessionDao()
}
