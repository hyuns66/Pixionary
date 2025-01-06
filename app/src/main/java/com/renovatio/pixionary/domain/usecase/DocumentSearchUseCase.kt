package com.renovatio.pixionary.domain.usecase

import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.model.Document
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class DocumentSearchUseCase @Inject constructor (
    val featureStoreRepository: FeatureRepository
) {
    operator fun invoke(query: String): List<Document> {
        return featureStoreRepository.loadQueryDocuments(query)
    }
}