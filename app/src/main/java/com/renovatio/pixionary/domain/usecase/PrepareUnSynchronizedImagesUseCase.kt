package com.renovatio.pixionary.domain.usecase

import android.net.Uri
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.util.VisionTransformerRunner
import javax.inject.Inject

class PrepareUnSynchronizedImagesUseCase @Inject constructor(
    val featureStoreRepository: FeatureRepository
){
    operator fun invoke(
        imageItemUris : MutableList<Pair<String, Uri>>,
        dummyPair : Pair<String, Uri>
    ) :  MutableList<MutableList<Pair<String, Uri>>> {
        val pathSet: Set<String> = featureStoreToSet()
        var uriCnt = 0
        var batchCnt = 0
        val inputItems = mutableListOf<MutableList<Pair<String, Uri>>>()
        for (pair in imageItemUris){
            if (pair.first in pathSet) {
                continue
            }       // featureStore에 이미 있는 이미지면 continue
            if (uriCnt == 0){
                inputItems.add(mutableListOf())
            }
            inputItems[batchCnt].add(pair)
            uriCnt += 1
            if (uriCnt == VisionTransformerRunner.BATCH_SIZE){
                uriCnt = 0
                batchCnt += 1
            }
        }
        // 마지막 batch 부족한 공간 dummy pair 패딩
        if (uriCnt != 0){
            for (i in uriCnt until VisionTransformerRunner.BATCH_SIZE){
                inputItems[batchCnt].add(dummyPair)
            }
            batchCnt += 1
        }
        return inputItems
    }

    private fun featureStoreToSet() : Set<String> {
        val imageFeatures = featureStoreRepository.loadFeatures()
        return imageFeatures.map { it.path }.toSet()
    }
}