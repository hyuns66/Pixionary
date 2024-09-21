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
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.model.Feature
import com.renovatio.pixionary.util.SimilarityCalculator
import com.renovatio.pixionary.util.TextTransformerRunner
import com.renovatio.pixionary.util.VisionTransformerRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(
    val visionRunner : VisionTransformerRunner,
    val featureStoreRepository: FeatureRepository
) : ViewModel() {
    private val _imageItemUris = MutableLiveData<MutableList<Pair<String, Uri>?>>(mutableListOf())
    val imageItemUris : LiveData<MutableList<Pair<String, Uri>?>> get() = _imageItemUris
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
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            INDEX_MEDIA_ID,
            INDEX_MEDIA_URI,
            INDEX_ALBUM_NAME,
            INDEX_DATE_ADDED
        )
        val selection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.SIZE + " > ?"
            else null
        val sortOrder = "$INDEX_DATE_ADDED DESC"
        val selectionArgs = arrayOf(
            "0"
        )
        var count = 0
        val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            while(cursor.moveToNext()) {
                val mediaPath = cursor.getString(cursor.getColumnIndex(INDEX_MEDIA_URI))
                _imageItemUris.value!!.add(Pair(mediaPath, Uri.fromFile(File(mediaPath))))
                count += 1
                // TODO 부하가 너무 많이걸려서 소수사진으로 제한. 나중에 제한풀어야함
                if (count == 123) break
            }
        }
    }

    // 전체 이미지들중 featureStore에 없는 이미지들만 골라서 input Data 구축
    fun prepareExtracting() : Int{
        _featureProgressCount.value = 0
        var uriCnt = 0
        var batchCnt = 0
        val imageFeatures = featureStoreRepository.loadFeatures()
        val pathSet: Set<String> = imageFeatures.map { it.path }.toSet()
        val dummyPair = _imageItemUris.value!![0]!!
        _inputItems.clear()
        for (pair in _imageItemUris.value!!){
            if (pair!!.first in pathSet) {
                Log.d("LILILISDfjlskd", pair.first)
                continue
            }       // featureStore에 이미 있는 이미지면 continue
            if (uriCnt == 0){
                _inputItems.add(mutableListOf())
            }
            _inputItems[batchCnt].add(pair)
            uriCnt += 1
            if (uriCnt == VisionTransformerRunner.BATCH_SIZE){
                uriCnt = 0
                batchCnt += 1
            }
        }
        if (uriCnt != 0){
            for (i in uriCnt until VisionTransformerRunner.BATCH_SIZE){
                _inputItems[batchCnt].add(dummyPair)
            }
            batchCnt += 1
        }
        return batchCnt * 12
    }

    fun extractFeatures(context : Context){
        val bitmapList = arrayListOf<Bitmap>()
        val pathList = arrayListOf<String>()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        viewModelScope.launch(Dispatchers.Default){
            for (uris in _inputItems) {
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
                bitmapList.clear()
                pathList.clear()
                _featureProgressCount.postValue(_featureProgressCount.value!!.plus(
                    VisionTransformerRunner.BATCH_SIZE
                ))
            }
        }
    }

    fun searchFeatures(query : String){
        val imageFeatures = featureStoreRepository.loadFeatures()
        Log.d("LILILISDfjlskd", imageFeatures.size.toString())
        val textRunner = TextTransformerRunner()
        val returns = textRunner.runSession(arrayListOf(query))

        val calc = SimilarityCalculator(returns, imageFeatures)
        _searchResults.value = calc.run()
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