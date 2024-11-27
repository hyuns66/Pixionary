package com.renovatio.pixionary.data

import android.util.Log
import com.renovatio.pixionary.domain.model.Feature
import io.objectbox.Box
import io.objectbox.exception.UniqueViolationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRepositoryImpl @Inject constructor(
    private val featureBox : Box<FeatureDTO>
) : FeatureRepository {
    override fun saveFeatures(key: List<String>, value: Array<FloatArray>) {
        val datas = key.zip(value).map { (k, v) ->
            FeatureDTO(path = k, feature = v)
        }
        for (data in datas){
            try {
                featureBox.put(data)  // Inserts or updates based on the unique constraint
            } catch (e: UniqueViolationException) {     // 중복되는 아이템들은 저장하지 않고 continue
                continue
            }
        }
    }

    override fun loadFeatures(): List<Feature> {
        val items = featureBox.all
        Log.d("LILILISDfjlskd", items.size.toString())
        val results = items.map{
            it.toModel()
        }
        return results
    }
}