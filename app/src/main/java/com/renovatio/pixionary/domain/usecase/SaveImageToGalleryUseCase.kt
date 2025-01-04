package com.renovatio.pixionary.domain.usecase

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.renovatio.pixionary.R
import com.renovatio.pixionary.ui.view.CameraPreviewFragment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SaveImageToGalleryUseCase @Inject constructor(
    @ApplicationContext private val context : Context
) {
    operator fun invoke(bitmap: Bitmap): Uri? {
        val isSaved: Boolean
        val timestamp = System.currentTimeMillis()
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.DATE_ADDED, timestamp)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        var uri : Uri? = null

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            // 이미지 저장할 이름, 확장자, 경로
            contentValues.apply {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pixionary")
                put(MediaStore.Images.Media.IS_PENDING, true)
            }
            val resolver = context.contentResolver
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, false)
                resolver.update(uri, contentValues, null, null)
            }
        } else {
            val imageFileFolder = File(Environment.getExternalStorageDirectory().toString() + '/' + context.getString(R.string.app_name))
            if (!imageFileFolder.exists()) {
                imageFileFolder.mkdirs()
            }
            val mImageName = "$timestamp.png"
            val imageFile = File(imageFileFolder, mImageName)
            contentValues.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            val resolver = context.contentResolver
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        }
        return uri
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        var isOffline = false // prevent app crash when goes offline
    }
}