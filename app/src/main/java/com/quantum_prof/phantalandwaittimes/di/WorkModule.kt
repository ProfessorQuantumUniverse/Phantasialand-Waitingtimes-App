package com.quantum_prof.phantalandwaittimes.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection Modul für Background-Services
 * Alternative Implementierung ohne WorkManager-Abhängigkeiten
 */
@Module
@InstallIn(SingletonComponent::class)
object BackgroundServiceModule {

    /**
     * Stellt den Anwendungskontext für Background-Services bereit
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
}
