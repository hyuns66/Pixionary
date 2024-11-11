package com.renovatio.pixionary.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.createSource
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.usecase.SearchUseCase
import com.renovatio.pixionary.domain.model.Feature
import com.renovatio.pixionary.domain.usecase.LoadAllImageUrisUseCase
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import com.renovatio.pixionary.util.VisionTransformerRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor (
    val search : SearchUseCase,
    val prepareUnSynchronizedImages: PrepareUnSynchronizedImagesUseCase,
    val loadAllImageUris : LoadAllImageUrisUseCase
) : ViewModel() {
    private val _imageItemUris = MutableLiveData<MutableList<Pair<String, Uri>>>(mutableListOf())
    val imageItemUris : LiveData<MutableList<Pair<String, Uri>>> get() = _imageItemUris
    private val _inputItems = mutableListOf<MutableList<Pair<String, Uri>>>()
    private val _featureProgressCount = MutableLiveData(0)
    val featureProgressCount get() = _featureProgressCount
    private val _searchResults = MutableLiveData<List<Feature>>(mutableListOf())
    val searchResults : LiveData<List<Feature>> get() = _searchResults

    /**
    안드로이드 Q(API 레벨 29) 이상에서는 더 엄격한 저장소 권한과 보안 정책이 도입되었습니다.
    이를 통해 앱이 외부 저장소에 접근하는 방식이 변경되었고,
    이러한 정책 변화에 따라 쿼리 조건을 더 명확하게 설정하는 것이 중요해졌습니다.
    **/
    @SuppressLint("Range")
    fun fetchImageItemUris(context: Context) {
        _imageItemUris.value = loadAllImageUris(context)
        _searchResults.value = imageItemUris.value!!.map { Feature(it.first, floatArrayOf()) }
    }

    // 전체 이미지들중 featureStore에 없는 이미지들만 골라서 input Data 구축
    fun prepareExtracting() : Int{
        _featureProgressCount.value = 0
        val dummyPair = imageItemUris.value!![0]
        _inputItems.clear()
        _inputItems.addAll(prepareUnSynchronizedImages(imageItemUris.value!!, dummyPair))
        return _inputItems.size * 12
    }

    fun extractFeatures(context : Context){
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        // 커스텀 스레드 풀 생성
        val myThreadPool = Executors.newFixedThreadPool(8)
        val myDispatcher = myThreadPool.asCoroutineDispatcher()

        viewModelScope.launch{
            visionRunner.initializeRuntime()
            val jobs = mutableListOf<Job>()
            try {
                for (uris in _inputItems) {
                    val job = viewModelScope.launch(myDispatcher) {
                        val bitmapList = arrayListOf<Bitmap>()
                        val pathList = arrayListOf<String>()
                        for (uriPair in uris){
                            val path = uriPair.first
                            val uri = uriPair.second
                            val bitmap =
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                    ImageDecoder.decodeBitmap(createSource(context.contentResolver, uri)){ decoder, _, _ ->
                                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                        decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
                                        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                                    }
                                } else {
                                    getBitmap(context.contentResolver, uri)
                                }
                            bitmapList.add(bitmap)
                            pathList.add(path)
                        }
                        val features = visionRunner.runSession(bitmapList)

                        featureStoreRepository.saveFeatures(pathList, features)
                        _featureProgressCount.postValue(_featureProgressCount.value!!.plus(
                            VisionTransformerRunner.BATCH_SIZE
                        ))
                    }
                    jobs.add(job)
                }
                jobs.joinAll()
            } finally {
                visionRunner.destroyRuntime()
                myDispatcher.close()
                myThreadPool.shutdown()
            }
        }
    }

    fun searchFeatures(query : String){
        _searchResults.value = search(query)
    }
    companion object {
        // The unique ID for a row.
        private const val INDEX_MEDIA_ID = MediaStore.MediaColumns._ID
        // Absolute filesystem path to the media item on disk.
        private const val INDEX_MEDIA_URI = MediaStore.MediaColumns.DATA
        // album directory name
        private const val INDEX_ALBUM_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        // The time the media item was first added.
        private const val INDEX_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED

        fun provideFactory(
            visionRunner : VisionTransformerRunner,
            featureStoreRepository: FeatureRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return GalleryViewModel(visionRunner, featureStoreRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}