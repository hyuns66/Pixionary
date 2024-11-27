package com.renovatio.pixionary.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.renovatio.pixionary.domain.model.GalleryFetchOptions
import java.io.File

class LoadAllImageUrisUseCase() {
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
        return imageItemUris
    }
}