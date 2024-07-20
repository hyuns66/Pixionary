package com.example.pixionary

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import com.example.pixionary.VisionTransformerRunner.Companion.BATCH_SIZE
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.system.exitProcess


class TextTransformerRunner : InputUtil<String> {
    private lateinit var ortEnvironment : OrtEnvironment
    private val tokenizer = BPETokenizer()
    private lateinit var textTransformerSession : OrtSession
    private lateinit var interpreter : Interpreter

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
            Log.d("read_textTransformer_ERROR", "check readONNXModelFromRaw method.")
        }
        return null
    }

    // TODO : 추후 text 모델도 TF lite로 마이그레이션
//    fun loadModelFile(modelName : String): MappedByteBuffer {
//        val assetFileDescriptor = ApplicationClass.getContext().assets.openFd(modelName)
//        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
//        val fileChannel = fileInputStream.channel
//        val startOffset = assetFileDescriptor.startOffset
//        val declaredLength = assetFileDescriptor.declaredLength
//
//
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//    }
//
//    private fun getTfliteInterpreter(): Interpreter {
//        try {
//            val model = loadModelFile(MODEL_NAME)
//            model.order(ByteOrder.nativeOrder())
//            return Interpreter(model)
//        } catch (e: Exception) {
//            Log.e("runtime textModel failed initializing", e.toString())
//            Toast.makeText(ApplicationClass.getContext(), "어플리케이션 런타임 환경 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show()
//            e.printStackTrace()
//            exitProcess(-1)
//        }
//    }

    override fun preprocess(data: String): IntArray {
        throw UnsupportedOperationException("This function is not supported")
    }

//    override fun makeBatchData(dataList: ArrayList<String>): Array<LongBuffer> {
//        val tokenizedPair = tokenizer.tokenize(dataList[0])
//        val tokenizedArray = tokenizedPair.first
//        val tokenizedNum = tokenizedPair.second
//        // Create an IntBuffer with a capacity equal to the size of the list
//        val tokenizedData = LongBuffer.allocate(tokenizedArray.size)
//
//        // Put each integer from the list into the IntBuffer
//        for (value in tokenizedArray) {
//            tokenizedData.put(value.toLong())
//        }
//
//        // Flip the buffer to prepare it for reading
//        tokenizedData.flip()
//
//        val tokensShape = longArrayOf(
//            BATCH_SIZE.toLong(),
//            TOKEN_LENGTH.toLong()
//        )
//
//        // self attention 범위를 지정하기 위한 attention_mask
//        val attentionMask = LongBuffer.allocate(77)
//        // Fill the buffer with 1s
//        for (i in 0 until attentionMask.capacity()) {
//            if (i < tokenizedNum) {
//                attentionMask.put(1)
//            } else {
//                attentionMask.put(0)
//            }
//        }
//        // Flip the buffer to prepare it for reading
//        attentionMask.flip()
//        val maskShape = longArrayOf(
//            BATCH_SIZE.toLong(),
//            TOKEN_LENGTH.toLong()
//        )
//
//        return arrayOf(tokenizedData, attentionMask)
//    }

    override fun makeBatchData(dataList: ArrayList<String>): Pair<OnnxTensor, OnnxTensor> {
        val tokenizedPair = tokenizer.tokenize(dataList[0])
        val tokenizedArray = tokenizedPair.first
        val tokenizedNum = tokenizedPair.second
        // Create an IntBuffer with a capacity equal to the size of the list
        val tokenizedData = LongBuffer.allocate(tokenizedArray.size)

        // Put each integer from the list into the IntBuffer
        for (value in tokenizedArray) {
            tokenizedData.put(value.toLong())
        }

        // Flip the buffer to prepare it for reading
        tokenizedData.flip()

        val tokensShape = longArrayOf(
            BATCH_SIZE.toLong(),
            TOKEN_LENGTH.toLong()
        )

        // self attention 범위를 지정하기 위한 attention_mask
        val attentionMask = LongBuffer.allocate(77)
        // Fill the buffer with 1s
        for (i in 0 until attentionMask.capacity()) {
            if(i < tokenizedNum){
                attentionMask.put(1)
            } else {
                attentionMask.put(0)
            }
        }
        // Flip the buffer to prepare it for reading
        attentionMask.flip()
        val maskShape = longArrayOf(
            BATCH_SIZE.toLong(),
            TOKEN_LENGTH.toLong()
        )
        return Pair(
            OnnxTensor.createTensor(ortEnvironment, tokenizedData, tokensShape),
            OnnxTensor.createTensor(ortEnvironment, attentionMask, maskShape)
        )
    }

    override fun runSession(dataList: ArrayList<String>): Array<FloatArray> {
        try {
//            interpreter = getTfliteInterpreter()

//            // 모델 입력 데이터 생성 및 전처리 (실제 모델에 따라 필요한 입력 데이터를 준비)
//            val inputTensor = makeBatchData(dataList)

//            // 출력 데이터를 받을 공간 준비 ([1, 512] 크기의 FloatBuffer)
//            val outputBuffer = FloatBuffer.allocate(RESULT_LENGTH)
//
//            // 출력 맵 준비
//            val outputs: MutableMap<Int, Any> = HashMap()
//            outputs[0] = outputBuffer

//            interpreter.runForMultipleInputsOutputs(inputTensor, outputs)

            // 결과 처리 (출력 데이터를 가져와서 사용)
//            outputBuffer.rewind()
//            val result = Array(BATCH_SIZE) { FloatArray(RESULT_LENGTH) }
//            for (i in 0 until BATCH_SIZE) {
//                outputBuffer.get(result[i])
//            }


            initializeRuntime()

            // 모델 입력 데이터 생성 및 전처리 (실제 모델에 따라 필요한 입력 데이터를 준비)
            val inputTensor = makeBatchData(dataList)

            // 모델 실행
            val inputs =
                mapOf("text_embedding" to inputTensor.first , "attention_mask" to inputTensor.second)

            // 추론 실행
            val results = textTransformerSession.run(inputs)

            // 결과 처리 (출력 데이터를 가져와서 사용)
            return results.get(0).value as Array<FloatArray>

            // 출력 데이터를 사용하여 원하는 작업을 수행
//            processModelOutput(outputData)
        } finally {
            // 세션 및 환경 정리
            destroyRuntime()
        }
    }

    private fun initializeRuntime(){
        ortEnvironment = OrtEnvironment.getEnvironment(OrtLoggingLevel.ORT_LOGGING_LEVEL_FATAL)
        // 세션 옵션 설정
        val sessionOptions = OrtSession.SessionOptions()
        val modelRawBytes = readONNXModelFromRaw(ApplicationClass.getContext().resources, R.raw.mobile_text_quant)

        // ONNX 모델을 세션으로 로드
        modelRawBytes?.let {
            textTransformerSession = ortEnvironment.createSession(it, sessionOptions)
        } ?: run{
            Log.e("ModelLoading", "Failed to load the ONNX model.")
            throw IllegalStateException("Failed to load the ONNX model.")
        }
    }

    private fun destroyRuntime(){
        textTransformerSession.close()
        ortEnvironment.close()
//        interpreter.close()
    }

    companion object{
        const val BATCH_SIZE = 1
        const val TOKEN_LENGTH = 77
        const val RESULT_LENGTH = 512
        const val MODEL_NAME = "mobile_text.tflite"
    }
}