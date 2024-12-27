package com.renovatio.pixionary.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.renovatio.pixionary.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class RecognizeTextUseCase @Inject constructor(
    @ApplicationContext private val context : Context
){
    operator fun invoke(bitmap : Bitmap) : Flow<Result<String>> = callbackFlow {
        val image = InputImage.fromBitmap(bitmap, 0)
        val textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                val resultText = text.text.replace("\n", "")
                trySend(Result.success(resultText))
            }
            .addOnFailureListener {
                trySend(Result.failure(it))
            }
            .addOnCanceledListener {
                trySend(Result.failure(Exception(context.getString(R.string.recognize_text_failure))))
            }
        awaitClose { textRecognizer.close() } // 리소스 정리
    }
}