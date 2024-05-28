package com.example.findmyphoto

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class GalleryViewModel : ViewModel() {
    val imageItemList = MutableLiveData<MutableList<Uri>>(mutableListOf())

    /**
    안드로이드 Q(API 레벨 29) 이상에서는 더 엄격한 저장소 권한과 보안 정책이 도입되었습니다.
    이를 통해 앱이 외부 저장소에 접근하는 방식이 변경되었고,
    이러한 정책 변화에 따라 쿼리 조건을 더 명확하게 설정하는 것이 중요해졌습니다.
    **/
    @SuppressLint("Range")
    fun fetchImageItemList(context: Context) {
//        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
//        val sortOrder = "$INDEX_DATE_ADDED DESC"
        val selectionArgs = arrayOf(
            "0"
        )
        val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, null)

        cursor?.let {
            while(cursor.moveToNext()) {
                val mediaPath = cursor.getString(cursor.getColumnIndex(INDEX_MEDIA_URI))
                imageItemList.value!!.add(
                    Uri.fromFile(File(mediaPath))
                )
            }
            Log.d("cncncncncncncncn", it.count.toString())
        }

        cursor?.close()
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
    }
}