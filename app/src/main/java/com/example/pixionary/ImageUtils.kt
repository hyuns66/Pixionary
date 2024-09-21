package com.example.pixionary

import android.graphics.*

open class ImageUtils() {
    fun centerCrop(bitmap : Bitmap, cropWidth : Int, cropHeight : Int) : Bitmap{
        if (bitmap.width <= cropWidth && bitmap.height <= cropHeight){
            return bitmap
        }
        var x = 0
        var y = 0

        if (bitmap.width > cropWidth){
            x = (bitmap.width - cropWidth) / 2
        }

        if (bitmap.height > cropHeight){
            y = (bitmap.height - cropHeight) / 2
        }
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }

    fun resizeFitShort(bitmap : Bitmap) : Bitmap{
        var resizedWidth = VisionTransformerRunner.IMAGE_SIZE_X
        var resizedHeight = VisionTransformerRunner.IMAGE_SIZE_Y
        if (bitmap.width > bitmap.height){
            val hwRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            resizedWidth = (resizedWidth * hwRatio).toInt()
        } else {
            val whRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
            resizedHeight = (resizedHeight * whRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
    }
}