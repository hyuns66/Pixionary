package com.renovatio.pixionary.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.renovatio.pixionary.R
import com.renovatio.pixionary.domain.usecase.ImageProxyToBitmapUseCase
import com.renovatio.pixionary.domain.usecase.RecognizeTextUseCase
import com.renovatio.pixionary.domain.usecase.SaveImageToGalleryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val proxy2bitmap : ImageProxyToBitmapUseCase,
    private val textRecognition : RecognizeTextUseCase,
    private val saveImageToGallery : SaveImageToGalleryUseCase
    ) : ViewModel(){
        private val _imageBitmap = MutableLiveData<Bitmap>()
    val imageBitmap : LiveData<Bitmap> get() = _imageBitmap
    private val _recognizeTextResult = MutableStateFlow<Result<String>?>(null)
    val recognizeTextResult: StateFlow<Result<String>?> = _recognizeTextResult
    // SharedFlow 정의
    private val _toastMessageEvents = MutableSharedFlow<Int>()
    val toastMessageEvents: SharedFlow<Int> get() = _toastMessageEvents


    // 이미지 캡처 결과를 설정하는 함수
    fun setCapturedImage(imageProxy : ImageProxy) {
        val bitmap = proxy2bitmap(imageProxy)
        _imageBitmap.value = bitmap
        recognizeText()
    }

    fun recognizeText(){
        viewModelScope.launch {
            textRecognition(imageBitmap.value!!)
                .catch { exception ->
                    _recognizeTextResult.value = Result.failure(exception)
                }
                .collect { result ->
                    _recognizeTextResult.value = result
                }
        }
    }

    fun saveImage(){
        val uri = saveImageToGallery(imageBitmap.value!!)
        if (uri == null){
            viewModelScope.launch {
                _toastMessageEvents.emit(R.string.error_failed_to_save_image)
            }
        }
    }
}