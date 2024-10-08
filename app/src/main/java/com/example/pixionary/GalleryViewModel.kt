package com.example.pixionary

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(
    val visionRunner : VisionTransformerRunner,
    val featureStoreRepository: FeatureRepository
) : ViewModel() {
    private val _imageItemList = MutableLiveData<MutableList<Array<Pair<String, Uri>?>>>(mutableListOf())
    val imageItemList : LiveData<MutableList<Array<Pair<String, Uri>?>>> get() = _imageItemList
    private val _imageItemPaths = MutableLiveData<MutableList<String>>(mutableListOf())
    val imageItemPaths : LiveData<MutableList<String>> get() = _imageItemPaths
    private val _featureProgressCount = MutableLiveData(0)
    val featureProgressCount get() = _featureProgressCount

    /**
    안드로이드 Q(API 레벨 29) 이상에서는 더 엄격한 저장소 권한과 보안 정책이 도입되었습니다.
    이를 통해 앱이 외부 저장소에 접근하는 방식이 변경되었고,
    이러한 정책 변화에 따라 쿼리 조건을 더 명확하게 설정하는 것이 중요해졌습니다.
    **/
    @SuppressLint("Range")
    fun fetchImageItemList(context: Context) {
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
        val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
        val paths = mutableListOf<String>()
        val uris : Array<Pair<String, Uri>?> = arrayOfNulls(VisionTransformerRunner.BATCH_SIZE)
        var uriCnt = 0
        cursor?.use {
            while(cursor.moveToNext()) {
                val mediaPath = cursor.getString(cursor.getColumnIndex(INDEX_MEDIA_URI))
                uris[uriCnt] = Pair(mediaPath, Uri.fromFile(File(mediaPath)))
                uriCnt += 1
                if (uriCnt >= VisionTransformerRunner.BATCH_SIZE){
                    imageItemList.value!!.add(uris.clone())
                    uriCnt = 0
                    for (i in uris.indices){
                        uris[i] = null
                    }
                }
                paths.add(mediaPath)
                // TODO 부하가 너무 많이걸려서 소수사진으로 제한. 나중에 제한풀어야함
                if (paths.size == 120){
                    break
                }
            }
        }
        _imageItemPaths.value = paths
    }

    fun extractFeatures(context : Context){
        val bitmapList = arrayListOf<Bitmap>()
        val pathList = arrayListOf<String>()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        viewModelScope.launch(Dispatchers.Default){
            for (uris in imageItemList.value!!) {
                for (uriPair in uris){
                    val path = uriPair!!.first
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

                for (i in features.indices){
                    featureStoreRepository.saveFeature(pathList[i], features[i])
                }
                bitmapList.clear()
                pathList.clear()
                _featureProgressCount.postValue(_featureProgressCount.value!!.plus(VisionTransformerRunner.BATCH_SIZE))
            }
        }
    }

    fun getImageFeaturesVector() : Array<FloatArray>{
        val featureArray = Array(120){ FloatArray(1) }
        for (i in imageItemPaths.value!!.indices){
            featureArray[i] = featureStoreRepository.loadFeature(imageItemPaths.value!![i])
        }
        return featureArray
    }

    fun updateImagePaths(similarities : ArrayList<Float>){
        val pairs = imageItemPaths.value!!.zip(similarities)
        _imageItemPaths.value = pairs.sortedBy { -it.second }.map { it.first }.toMutableList()
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