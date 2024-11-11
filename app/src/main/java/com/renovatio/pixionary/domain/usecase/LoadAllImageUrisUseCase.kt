package com.renovatio.pixionary.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

class LoadAllImageUrisUseCase {
    // The unique ID for a row.
    private val INDEX_MEDIA_ID = MediaStore.MediaColumns._ID
    // Absolute filesystem path to the media item on disk.
    private val INDEX_MEDIA_URI = MediaStore.MediaColumns.DATA
    // album directory name
    private val INDEX_ALBUM_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    // The time the media item was first added.
    private val INDEX_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED

    @SuppressLint("Range")
    operator fun invoke(context : Context) : MutableList<Pair<String, Uri>>{
        val imageItemUris = mutableListOf<Pair<String, Uri>>()
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
        val sortOrder = "${INDEX_DATE_ADDED} DESC"
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
        return imageItemUris
    }
}