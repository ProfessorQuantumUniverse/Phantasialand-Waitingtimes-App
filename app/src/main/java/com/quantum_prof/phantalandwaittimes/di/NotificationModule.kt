package com.quantum_prof.phantalandwaittimes.di

import android.content.SharedPreferences
import com.quantum_prof.phantalandwaittimes.data.notification.AlertRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {


    @Provides
    @Singleton
    fun provideAlertRepository(
        sharedPreferences: SharedPreferences,
        json: Json
    ): AlertRepository {
        return AlertRepository(
            sharedPreferences = sharedPreferences,
            json = json
        )
    }
}