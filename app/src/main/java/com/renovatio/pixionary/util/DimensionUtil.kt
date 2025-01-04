package com.renovatio.pixionary.util

import android.content.Context

object DimensionUtil {
    fun pxToDp(px: Float, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return px / density
    }

    fun dpToPx(dp: Float, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }
}
