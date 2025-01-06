package com.renovatio.pixionary.data

import android.net.Uri
import com.renovatio.pixionary.domain.model.Document
import com.renovatio.pixionary.domain.model.Feature
import io.objectbox.Box
import io.objectbox.exception.UniqueViolationException
import io.objectbox.query.QueryBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRepositoryImpl @Inject constructor(
    private val featureBox : Box<FeatureDTO>,
    private val documentBox : Box<DocumentDTO>
) : FeatureRepository {
    override fun saveFeatures(key: List<Uri>, value: Array<FloatArray>) {
        val datas = key.zip(value).map { (k, v) ->
            FeatureDTO(uriString = k.toString(), feature = v)
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
        val results = items.map{
            it.toModel()
        }
        return results
    }

    override fun saveDocumentRecognitionResult(uri: Uri, text: String) {
        val data = DocumentDTO(uri.toString(), text)
        try {
            documentBox.put(data)
        } catch (_: UniqueViolationException){  }
    }

    override fun loadAllDocuments(): List<Document> {
        val items = documentBox.all
        val results = items.map{
            it.toModel()
        }
        return results
    }

    override fun loadQueryDocuments(query: String): List<Document> {
        val boxQuery = documentBox.query()
            .contains(DocumentDTO_.text, query, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
        val results = boxQuery.find().map{
            it.toModel()
        }
        return results
    }
}