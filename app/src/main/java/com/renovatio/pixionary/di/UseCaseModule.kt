package com.renovatio.pixionary.di

import com.renovatio.pixionary.domain.usecase.ImageProxyToBitmapUseCase
import com.renovatio.pixionary.domain.usecase.LoadAllImageUrisUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun providesLoadAllImageUrisUseCase() : LoadAllImageUrisUseCase {
        return LoadAllImageUrisUseCase()
    }

    @Provides
    @Singleton
    fun provideImageProxyToBitmapUseCase() : ImageProxyToBitmapUseCase {
        return ImageProxyToBitmapUseCase()
    }

//    @Provides
//    fun providesPrepareUnSynchronizedImageUseCase(featureRepository : FeatureRepository) : PrepareUnSynchronizedImagesUseCase {
//        return PrepareUnSynchronizedImagesUseCase(featureRepository)
//    }

//    @Provides
//    fun providesSearchUseCase(featureRepository : FeatureRepository) : SearchUseCase {
//        return SearchUseCase(featureRepository)
//    }
//
//    @Provides
//    fun provideVitWorkManagerUseCase(workManager: WorkManager): VitWorkManagerUseCase {
//        return VitWorkManagerUseCase(workManager)
//    }

}