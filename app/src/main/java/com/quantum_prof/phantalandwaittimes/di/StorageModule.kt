package com.quantum_prof.phantalandwaittimes.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    const val KEY_FAVORITE_CODES = "favorite_codes"
    const val KEY_SORT_TYPE = "sort_type"
    const val KEY_SORT_DIRECTION = "sort_direction"
    const val KEY_FILTER_ONLY_OPEN = "filter_only_open"

    /** Alerts as a single JSON array. */
    const val KEY_WAIT_TIME_ALERTS_V2 = "wait_time_alerts_v2"

    /** Pre-2.2 storage: a `Set<String>` of individual JSON objects. Read once, then removed. */
    const val KEY_WAIT_TIME_ALERTS_LEGACY = "wait_time_alerts"

    private const val PREFS_NAME = "phantasialand_wait_times_prefs"

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
