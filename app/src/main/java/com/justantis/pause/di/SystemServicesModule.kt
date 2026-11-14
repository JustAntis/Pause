package com.justantis.pause.di

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SystemServicesModule {
    @Provides
    fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager =
        context.getSystemService(UsageStatsManager::class.java)

    @Provides
    fun providePackageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager
}
