package com.example.findmyphoto

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.get
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.FloatBuffer
import java.text.DecimalFormat
import java.util.Collections

class VisionTransformerRunner : InputUtil<Bitmap>, ImageUtils(){

    private lateinit var ortEnvironment : OrtEnvironment

    //    private lateinit var visionTransformerByte : ByteArray
    private lateinit var visionTransformerSession : OrtSession

    fun readONNXModelFromRaw(resources: Resources, rawResourceId: Int): ByteArray? {
        try {
            val inputStream: InputStream = resources.openRawResource(rawResourceId)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            return outputStream.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("read_VisionTransformer_ERROR", "check readONNXModelFromRaw method.")
        }
        return null
    }

    override fun preprocess(data: Bitmap): IntArray {
        val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
        val bmpData = IntArray(stride)

        val resizedBitmap = resizeFitShort(data)
        val croppedBitmap = centerCrop(resizedBitmap, IMAGE_SIZE_X, IMAGE_SIZE_Y)
        Log.d("sizeeeeeeeee", "(${data.width}, ${data.height}) -> (${resizedBitmap.width}, ${resizedBitmap.height}) -> (${croppedBitmap.width}, ${croppedBitmap.height}")

        croppedBitmap.getPixels(bmpData, 0, croppedBitmap.width, 0, 0, croppedBitmap.width, croppedBitmap.height)   // 비트맵 픽셀값 bmpData에 저장 row 단위로 저장
        bmpData.sliceArray(0 until 20).forEach {
            val red = (it shr 16) and 0xFF
            Log.d("sizee_data", "$red")
        }
        return bmpData
    }

    override fun makeBatchData(dataList: ArrayList<Bitmap>): OnnxTensor {
        val imgData = FloatBuffer.allocate(
            BATCH_SIZE
                    * CHANNEL_SIZE
                    * IMAGE_SIZE_X
                    * IMAGE_SIZE_Y
        )
        imgData.rewind()
        val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y

        for (b in 0 until BATCH_SIZE){
            val bmpData = preprocess(dataList[b])

            // 데이터를 리틀 엔디안 순서로 적재 (Native order)
            // floatBuffer를 onnxTensor로 shape에 맞게 변환하려면 Native Order 순서로 적재되어야함
            for (h in 0 until IMAGE_SIZE_Y) {
                for (w in 0 until IMAGE_SIZE_X) {
                    // bmpData에서 픽셀값을 가져온 다음 RGB로 분해하여 floatBuffer에 저장
                    val idx = (IMAGE_SIZE_Y * h) + w
                    val batch = (IMAGE_SIZE_X * IMAGE_SIZE_Y * CHANNEL_SIZE * b)
                    val pixelValue = bmpData[idx]
                    imgData.put(batch + idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.48145467f) / 0.26862955f))          // R
                    imgData.put(batch + idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.4578275f) / 0.2613026f))    // G
                    imgData.put(batch + idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.40821072f) / 0.2757771f))     // B
                }
            }
        }
        imgData.rewind()

        val shape = longArrayOf(
            BATCH_SIZE.toLong(),
            CHANNEL_SIZE.toLong(),
            IMAGE_SIZE_Y.toLong(),
            IMAGE_SIZE_X.toLong()
        )
        return OnnxTensor.createTensor(ortEnvironment, imgData, shape)
    }

    override fun runSession(dataList: ArrayList<Bitmap>) : Array<FloatArray> {
        try{
            initializeRuntime()
            val inputTensor = makeBatchData(dataList)
            Log.d("sizeee", inputTensor.info.toString())
            inputTensor.floatBuffer
            val inputName = visionTransformerSession.inputNames.iterator().next()
            val resultTensor = visionTransformerSession.run(Collections.singletonMap(inputName, inputTensor))
            val outputs = resultTensor.get(0).value as Array<FloatArray> // [1 84 8400]
            return outputs
//        val results = dataProcess.outputsToNPMSPredictions(outputs)
        } finally {
            destroyRuntime()
        }
    }

    fun initializeRuntime(){
        ortEnvironment = OrtEnvironment.getEnvironment(OrtLoggingLevel.ORT_LOGGING_LEVEL_FATAL)
        // 세션 옵션 설정
        val sessionOptions = OrtSession.SessionOptions()
        val modelRawBytes = readONNXModelFromRaw(ApplicationClass.getContext().resources, R.raw.mobile_vision_quant)

        // ONNX 모델을 세션으로 로드
        modelRawBytes?.let {
            visionTransformerSession = ortEnvironment.createSession(it, sessionOptions)
        } ?: run{
            Log.e("ModelLoading", "Failed to load the ONNX model.")
            throw IllegalStateException("Failed to load the ONNX model.")
        }
    }

    fun destroyRuntime(){
        visionTransformerSession.close() // OrtSession을 닫습니다.
        ortEnvironment.close()
    }

    companion object{
        const val BATCH_SIZE = 12;
        const val CHANNEL_SIZE = 3;
        const val IMAGE_SIZE_X = 224;
        const val IMAGE_SIZE_Y = 224;
    }
}