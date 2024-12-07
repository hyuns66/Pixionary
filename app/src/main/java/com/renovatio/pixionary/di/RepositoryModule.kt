package com.renovatio.pixionary.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import com.renovatio.pixionary.data.FeatureDTO
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.data.FeatureRepositoryImpl
import com.renovatio.pixionary.domain.model.Feature
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun provideFeatureRepository(impl: FeatureRepositoryImpl) : FeatureRepository
}