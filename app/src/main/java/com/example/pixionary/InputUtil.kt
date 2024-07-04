package com.example.pixionary

interface InputUtil<T> {
    fun preprocess(data : T) : IntArray
    fun makeBatchData(dataList : ArrayList<T>) : Any
    fun runSession(dataList : ArrayList<T>): Array<FloatArray>
}