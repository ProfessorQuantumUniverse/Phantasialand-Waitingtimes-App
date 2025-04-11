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

    private const val PREFS_NAME = "phantasialand_prefs"
    const val KEY_WAIT_TIMES_JSON = "last_wait_times_json" // Konstante für Key
    const val KEY_LAST_FETCH_TIMESTAMP = "last_fetch_timestamp" // Konstante für Key

    // Stelle sicher, dass DIESE ZEILE existiert und korrekt ist:
    const val KEY_FAVORITE_CODES = "favorite_attraction_codes"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}