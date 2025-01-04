package com.renovatio.pixionary.domain.usecase

import android.net.Uri
import android.util.Log
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.util.VisionTransformerRunner
import javax.inject.Inject

class PrepareUnSynchronizedImagesUseCase @Inject constructor(
    private val featureStoreRepository: FeatureRepository
){
    operator fun invoke(
        imageItemUris : MutableList<Uri>,
        dummyItem : Uri
    ) :  MutableList<MutableList<Uri>> {
        val uriSet: Set<Uri> = featureStoreToSet()
        var uriCnt = 0
        var batchCnt = 0
        val inputItems = mutableListOf<MutableList<Uri>>()
        for (itemUri in imageItemUris){
            if (itemUri in uriSet) {
                continue
            }       // featureStore에 이미 있는 이미지면 continue
            if (uriCnt == 0){
                inputItems.add(mutableListOf())
            }
            inputItems[batchCnt].add(itemUri)
            uriCnt += 1
            if (uriCnt == VisionTransformerRunner.BATCH_SIZE){
                uriCnt = 0
                batchCnt += 1
            }
        }
        // 마지막 batch 부족한 공간 dummy pair 패딩
        if (uriCnt != 0){
            for (i in uriCnt until VisionTransformerRunner.BATCH_SIZE){
                inputItems[batchCnt].add(dummyItem)
            }
            batchCnt += 1
        }
        return inputItems
    }

    private fun featureStoreToSet() : Set<Uri> {
        val imageFeatures = featureStoreRepository.loadFeatures()
        return imageFeatures.map { it.uri }.toSet()
    }
}