package com.renovatio.pixionary.data

import com.renovatio.pixionary.domain.model.Feature

interface FeatureRepository {
    fun saveFeatures(key: List<String>, value: Array<FloatArray>)
    fun loadFeatures(): List<Feature>
}