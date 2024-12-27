package com.renovatio.pixionary.di

import androidx.work.WorkManager
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.usecase.ImageProxyToBitmapUseCase
import com.renovatio.pixionary.domain.usecase.LoadAllImageUrisUseCase
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import com.renovatio.pixionary.domain.usecase.SearchUseCase
import com.renovatio.pixionary.domain.usecase.VitWorkManagerUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
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