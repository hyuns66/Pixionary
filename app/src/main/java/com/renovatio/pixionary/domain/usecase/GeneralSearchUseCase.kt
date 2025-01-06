package com.renovatio.pixionary.domain.usecase

import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.model.Feature
import com.renovatio.pixionary.util.SimilarityCalculator
import com.renovatio.pixionary.util.TextTransformerRunner
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class GeneralSearchUseCase @Inject constructor(
    val featureStoreRepository: FeatureRepository
) {
    operator fun invoke(query : String) : List<Feature>{
        val imageFeatures = featureStoreRepository.loadFeatures()
        val textRunner = TextTransformerRunner()
        val returns = textRunner.runSession(arrayListOf(query))

        val calc = SimilarityCalculator(returns, imageFeatures)
        return calc.run()
    }
}