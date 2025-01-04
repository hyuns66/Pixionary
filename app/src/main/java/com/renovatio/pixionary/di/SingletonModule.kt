package com.renovatio.pixionary.di

import android.content.Context
import androidx.work.WorkManager
import com.renovatio.pixionary.data.DocumentDTO
import com.renovatio.pixionary.data.FeatureDTO
import com.renovatio.pixionary.data.ObjectBox.store
import com.renovatio.pixionary.util.VisionTransformerRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonModule {
    @Provides
    @Singleton
    fun provideFeatureBox(): Box<FeatureDTO> {
        return store.boxFor(FeatureDTO::class)
    }
    @Provides
    @Singleton
    fun provideDocumentBox(): Box<DocumentDTO> {
        return store.boxFor(DocumentDTO::class)
    }
    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideVitRunner() : VisionTransformerRunner{
        return VisionTransformerRunner()
    }
}