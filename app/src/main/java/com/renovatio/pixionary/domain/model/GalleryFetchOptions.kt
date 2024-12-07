package com.renovatio.pixionary.domain.model

import android.provider.MediaStore

enum class GalleryFetchOptions(val key : String) {
    // The unique ID for a row.
    INDEX_MEDIA_ID(MediaStore.MediaColumns._ID),
    // Absolute filesystem path to the media item on disk.
    INDEX_MEDIA_URI(MediaStore.MediaColumns.DATA),
    // album directory name
    INDEX_ALBUM_NAME(MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
    // The time the media item was first added.
    INDEX_DATE_ADDED(MediaStore.MediaColumns.DATE_ADDED)
}