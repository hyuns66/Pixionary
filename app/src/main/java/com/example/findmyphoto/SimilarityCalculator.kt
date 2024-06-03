package com.example.findmyphoto

import android.util.Log
import android.widget.Toast
import kotlin.math.sqrt

class SimilarityCalculator(private var query : Array<FloatArray>, private var imageFeatures : Array<FloatArray>) {
    init {
        query = normalizeVector(query)
        imageFeatures = normalizeVector(imageFeatures)
    }

    // vector의 batch 수에 맞게 l2Norm을 구해 배열로 반환
    // l2norm의 경우 batch 1개에 대한 전체 벡터의 평균으로 구함
    private fun l2Norm(vector: Array<FloatArray>): ArrayList<Float> {
        val l2norm = ArrayList<Float>()
        for (vrow in vector){
            var norm = 0f
            for (v in vrow){
                norm += v * v
            }
            norm = sqrt(norm)
            l2norm.add(norm)
        }
        return l2norm
    }

    // l2norm 을 활용해 정규화한 벡터 반환
    fun normalizeVector(vector : Array<FloatArray>) : Array<FloatArray>{
        val l2norms = l2Norm(vector)
        val normalizedArray = Array(l2norms.size) { FloatArray(vector[0].size) }
        for (i in vector.indices){
            val v = vector[i]
            for (j in v.indices){
                normalizedArray[i][j] = v[j] / l2norms[i]
            }
        }
        return normalizedArray
    }

    fun dotProduct(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProd = 0f
        for (i in vec1.indices) {
            dotProd += vec1[i] * vec2[i]
        }
        return dotProd
    }

    fun magnitude(vec: FloatArray): Float {
        var sum = 0f
        for (v in vec) {
            sum += v * v
        }
        return sqrt(sum)
    }

    fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        val dotProd = dotProduct(vec1, vec2)
        val magnitude1 = magnitude(vec1)
        val magnitude2 = magnitude(vec2)
        return dotProd / (magnitude1 * magnitude2)
    }

    fun run() : ArrayList<Float>{
        val sims = arrayListOf<Float>()
        for (feature in imageFeatures){
            val similarity = cosineSimilarity(query[0], feature)
            sims.add(similarity)
        }
        return sims
    }

    fun setImgFeatures(){
        // TODO : 갤러리 연동
    }
}