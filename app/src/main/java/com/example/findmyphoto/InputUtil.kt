package com.example.findmyphoto

import ai.onnxruntime.OnnxTensor

interface InputUtil<T> {
    fun preprocess(data : T) : IntArray
    fun makeBatchData(dataList : ArrayList<T>) : Any
    fun runSession(dataList : ArrayList<T>): Array<FloatArray>
}