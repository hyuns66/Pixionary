package com.renovatio.pixionary.di

import com.renovatio.pixionary.data.FeatureDTO
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.data.ObjectBox.store
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}