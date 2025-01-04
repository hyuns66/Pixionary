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

    /**
    안드로이드 Q(API 레벨 29) 이상에서는 더 엄격한 저장소 권한과 보안 정책이 도입되었습니다.
    이를 통해 앱이 외부 저장소에 접근하는 방식이 변경되었고,
    이러한 정책 변화에 따라 쿼리 조건을 더 명확하게 설정하는 것이 중요해졌습니다.
     **/
    @SuppressLint("Range")
    operator fun invoke(context : Context) : MutableList<Uri>{
        val imageItemUris = mutableListOf<Uri>()
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
                imageItemUris.add(Uri.fromFile(File(mediaPath)))
                count += 1
                // TODO 부하가 너무 많이걸려서 소수사진으로 제한. 나중에 제한풀어야함
                if (count == 240) break
            }
        }
        return imageItemUris
    }
}