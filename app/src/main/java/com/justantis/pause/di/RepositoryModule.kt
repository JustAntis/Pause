package com.justantis.pause.di

import com.justantis.pause.data.repository.AppCatalogRepositoryImpl
import com.justantis.pause.data.repository.BlockRepositoryImpl
import com.justantis.pause.data.repository.FocusRepositoryImpl
import com.justantis.pause.data.repository.LimitRepositoryImpl
import com.justantis.pause.data.repository.PermissionRepositoryImpl
import com.justantis.pause.data.repository.SettingsRepositoryImpl
import com.justantis.pause.data.repository.StreakRepositoryImpl
import com.justantis.pause.data.repository.UsageRepositoryImpl
import com.justantis.pause.data.worker.WorkManagerScheduler
import com.justantis.pause.domain.repository.AppCatalogRepository
import com.justantis.pause.domain.repository.BackgroundWorkScheduler
import com.justantis.pause.domain.repository.BlockRepository
import com.justantis.pause.domain.repository.FocusRepository
import com.justantis.pause.domain.repository.LimitRepository
import com.justantis.pause.domain.repository.PermissionRepository
import com.justantis.pause.domain.repository.SettingsRepository
import com.justantis.pause.domain.repository.StreakRepository
import com.justantis.pause.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindLimitRepository(impl: LimitRepositoryImpl): LimitRepository

    @Binds
    @Singleton
    abstract fun bindBlockRepository(impl: BlockRepositoryImpl): BlockRepository

    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppCatalogRepository(impl: AppCatalogRepositoryImpl): AppCatalogRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(impl: StreakRepositoryImpl): StreakRepository

    @Binds
    abstract fun bindBackgroundWorkScheduler(impl: WorkManagerScheduler): BackgroundWorkScheduler
}
