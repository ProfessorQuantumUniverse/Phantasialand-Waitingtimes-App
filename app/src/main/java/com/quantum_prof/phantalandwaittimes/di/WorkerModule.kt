package com.quantum_prof.phantalandwaittimes.di

import android.content.Context
import com.quantum_prof.phantalandwaittimes.worker.WaitTimeCheckService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWaitTimeCheckService(
        @ApplicationContext context: Context
    ): WaitTimeCheckService {
        return WaitTimeCheckService(context)
    }
}
