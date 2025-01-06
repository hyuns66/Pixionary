package com.renovatio.pixionary.data

import android.net.Uri
import com.renovatio.pixionary.domain.model.Document
import com.renovatio.pixionary.domain.model.Feature

interface FeatureRepository {
    fun saveFeatures(key: List<Uri>, value: Array<FloatArray>)
    fun loadFeatures(): List<Feature>
    fun saveDocumentRecognitionResult(uri : Uri, text : String)
    fun loadAllDocuments() : List<Document>
    fun loadQueryDocuments(query : String) : List<Document>
}