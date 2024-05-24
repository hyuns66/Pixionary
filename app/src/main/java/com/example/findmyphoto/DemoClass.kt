package com.example.findmyphoto

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import ai.onnxruntime.extensions.OrtxPackage


class DemoClass {
    private val ortEnvironment: OrtEnvironment
    private val ortSession: OrtSession? = null

    init {
        ortEnvironment = OrtEnvironment.getEnvironment()
        val sessionOptions = SessionOptions()
        // here where you add the onnxruntime extensions
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
    }
}