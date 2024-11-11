package com.renovatio.pixionary.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.model.Feature
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import com.renovatio.pixionary.ui.GalleryViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@HiltWorker
class VitBackgroundRunner @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    val visionRunner : VisionTransformerRunner,
    private val featureStoreRepository: FeatureRepository,
    private val prepareUnSynchronizedImages: PrepareUnSynchronizedImagesUseCase
) : CoroutineWorker(context, params) {
    // The unique ID for a row.
    private val INDEX_MEDIA_ID = MediaStore.MediaColumns._ID
    // Absolute filesystem path to the media item on disk.
    private val INDEX_MEDIA_URI = MediaStore.MediaColumns.DATA
    // album directory name
    private val INDEX_ALBUM_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    // The time the media item was first added.
    private val INDEX_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED
    private val imageItemUris = mutableListOf<Pair<String, Uri>?>()
    private val inputItems = mutableListOf<MutableList<Pair<String, Uri>>>()
    private var featureProgressCount = 0
    override suspend fun doWork(): Result = coroutineScope{
        fetchImageItemUris(applicationContext)
        prepareExtracting()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        visionRunner.initializeRuntime()
        val jobs = mutableListOf<Job>()
        try {
            for (uris in inputItems) {
                val job = CoroutineScope(Dispatchers.Default).launch{
                    val bitmapList = arrayListOf<Bitmap>()
                    val pathList = arrayListOf<String>()
                    for (uriPair in uris){
                        val path = uriPair.first
                        val uri = uriPair.second
                        val bitmap =
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(
                                        applicationContext.contentResolver,
                                        uri
                                    )
                                ){ decoder, _, _ ->
                                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                    decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
                                    decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                                }
                            } else {
                                MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri)
                            }
                        bitmapList.add(bitmap)
                        pathList.add(path)
                    }
                    val features = visionRunner.runSession(bitmapList)

                    featureStoreRepository.saveFeatures(pathList, features)
                    featureProgressCount += VisionTransformerRunner.BATCH_SIZE
                    setProgress(workDataOf("Progress" to featureProgressCount))
                }
                jobs.add(job)
            }
            jobs.joinAll()
            if (featureProgressCount == inputItems.size * 12) Result.success() else Result.failure()
        } catch (e : Exception) {
            Result.failure()
        } finally {
            visionRunner.destroyRuntime()
        }
    }

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
                imageItemUris.add(Pair(mediaPath, Uri.fromFile(File(mediaPath))))
                count += 1
                // TODO 부하가 너무 많이걸려서 소수사진으로 제한. 나중에 제한풀어야함
                if (count == 2400) break
            }
        }
    }

    // 전체 이미지들중 featureStore에 없는 이미지들만 골라서 input Data 구축
    fun prepareExtracting() : Int{
        featureProgressCount = 0
        var uriCnt = 0
        var batchCnt = 0
        val imageFeatures = featureStoreRepository.loadFeatures()
        val pathSet: Set<String> = imageFeatures.map { it.path }.toSet()
        val dummyPair = imageItemUris[0]!!
        inputItems.clear()
        for (pair in imageItemUris){
            if (pair!!.first in pathSet) {
                continue
            }       // featureStore에 이미 있는 이미지면 continue
            if (uriCnt == 0){
                inputItems.add(mutableListOf())
            }
            inputItems[batchCnt].add(pair)
            uriCnt += 1
            if (uriCnt == VisionTransformerRunner.BATCH_SIZE){
                uriCnt = 0
                batchCnt += 1
            }
        }
        // 마지막 batch 부족한 공간 dummy pair 패딩
        if (uriCnt != 0){
            for (i in uriCnt until VisionTransformerRunner.BATCH_SIZE){
                inputItems[batchCnt].add(dummyPair)
            }
            batchCnt += 1
        }
        return batchCnt * 12
    }
}