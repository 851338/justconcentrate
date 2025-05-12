package com.mobichill.justconcentration.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class) // This scope will live as long as the application runs
object CoroutineScopesModule {

    @Provides
    @Singleton
    @ApplicationCoroutineScope // Custom qualifier
    fun provideApplicationCoroutineScope(): CoroutineScope {
        // SupervisorJob means if one child coroutine fails, it doesn't cancel the whole scope.
        // Dispatchers.Default is good for CPU-intensive work off the main thread.
        // Use Dispatchers.IO if it's primarily for I/O-bound operations.
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

@Retention(AnnotationRetention.BINARY) // Or AnnotationRetention.RUNTIME
@Qualifier
annotation class ApplicationCoroutineScope