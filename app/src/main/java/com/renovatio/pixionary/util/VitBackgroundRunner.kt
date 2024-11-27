package com.renovatio.pixionary.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.renovatio.pixionary.R
import com.renovatio.pixionary.data.FeatureRepository
import com.renovatio.pixionary.domain.model.GalleryFetchOptions
import com.renovatio.pixionary.domain.usecase.PrepareUnSynchronizedImagesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@HiltWorker
class VitBackgroundRunner @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    val visionRunner : VisionTransformerRunner,
    private val featureStoreRepository: FeatureRepository,
    private val prepareUnSynchronizedImages: PrepareUnSynchronizedImagesUseCase
) : CoroutineWorker(context, params) {
    private val imageItemUris = mutableListOf<Pair<String, Uri>?>()
    private val inputItems = mutableListOf<MutableList<Pair<String, Uri>>>()
    private var featureProgressCount = 0
    override suspend fun doWork(): Result = coroutineScope{
        setForeground(createForegroundInfo(0))

        fetchImageItemUris(applicationContext)
        // 작업에 필요한 데이터셋 생성 밑 progress 정보 전달
        val maxInputImagesCount = prepareExtracting()
        val maxProgressCount = workDataOf(PROGRESS_MAX_KEY to maxInputImagesCount)
        setProgress(maxProgressCount)

        val availableCores = Runtime.getRuntime().availableProcessors()
        val myThreadPool = Executors.newFixedThreadPool(availableCores)
        val myDispatcher = myThreadPool.asCoroutineDispatcher()
        val bmpFactoryOption = BitmapFactory.Options()
        bmpFactoryOption.inScaled = false
        visionRunner.initializeRuntime()
        val jobs = mutableListOf<Job>()
        val currentProgress = AtomicInteger(0)
        setProgress(workDataOf(PROGRESS_INFO_KEY to currentProgress.get()))
        try {
            for (uris in inputItems) {
                val job = CoroutineScope(myDispatcher).launch{
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
                    val progress = currentProgress.addAndGet(VisionTransformerRunner.BATCH_SIZE)
                    setProgress(workDataOf(PROGRESS_INFO_KEY to progress))
                }
                jobs.add(job)
            }
            jobs.joinAll()
            if (featureProgressCount == inputItems.size * 12) Result.success() else Result.failure()
        } catch (e : Exception) {
            Result.failure()
        } finally {
            visionRunner.destroyRuntime()
            myDispatcher.close()
            myThreadPool.shutdown()
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
            GalleryFetchOptions.INDEX_MEDIA_ID.key,
            GalleryFetchOptions.INDEX_MEDIA_URI.key,
            GalleryFetchOptions.INDEX_ALBUM_NAME.key,
            GalleryFetchOptions.INDEX_DATE_ADDED.key
        )
        val selection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.SIZE + " > ?"
            else null
        val sortOrder = "${GalleryFetchOptions.INDEX_DATE_ADDED.key} DESC"
        val selectionArgs = arrayOf(
            "0"
        )
        var count = 0
        val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            while(cursor.moveToNext()) {
                val mediaPath = cursor.getString(cursor.getColumnIndex(GalleryFetchOptions.INDEX_MEDIA_URI.key))
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

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val channelId = "your_channel_id"
        val title = "작업 진행 중"
        val cancel = "취소"

        // Notification 채널 생성 (API 26 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "작업 알림",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 작업 취소 인텐트 생성
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        // Notification 생성
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("진행률: $progress%")
            .setSmallIcon(R.drawable.img_7)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .setProgress(100, progress, false)
            .build()
        return ForegroundInfo(1, notification)
    }

    private fun updateNotification(progress: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createForegroundInfo(progress).notification
        notificationManager.notify(1, notification)
    }

    companion object{
        const val PROGRESS_INFO_KEY = "progress"
        const val PROGRESS_MAX_KEY = "max-progress"
    }

}