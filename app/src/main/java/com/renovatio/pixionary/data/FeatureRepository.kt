package com.renovatio.pixionary.data

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.renovatio.pixionary.domain.model.Feature
import io.objectbox.Box
import io.objectbox.kotlin.boxFor

class FeatureRepository(private val featureBox : Box<FeatureDTO>) {
    fun saveFeatures(key: List<String>, value: Array<FloatArray>) {
        val datas = key.zip(value).map { (k, v) ->
            FeatureDTO(path = k, feature = v)
        }
        featureBox.put(datas)
    }

    fun loadFeatures(): List<Feature> {
        val items = featureBox.all
        Log.d("LILILISDfjlskd", items.size.toString())
        val results = items.map{
            it.toModel()
        }
        return results
    }
}