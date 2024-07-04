package com.example.pixionary

import android.content.SharedPreferences
import com.google.gson.Gson

class FeatureRepository(private val featureStore : SharedPreferences) {
    fun saveFeature(key: String, value: FloatArray) {
        val jsonData = Gson().toJson(value)
        featureStore.edit().putString(key, jsonData).apply()
    }

    fun loadFeature(key: String): FloatArray {
        return Gson().fromJson(featureStore.getString(key, ""), FloatArray::class.java)
    }

}