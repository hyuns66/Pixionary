package com.example.findmyphoto

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory

class ApplicationClass : Application() {
    private lateinit var appContext: Context

    companion object {
        private lateinit var instance: ApplicationClass
        private const val FEATURE_STORE_DATABASE_NAME = "feature_store"
        lateinit var imageLoader : ImageLoader
        private var spf : SharedPreferences? = null

        fun getContext(): Context {
            return instance.appContext
        }

        fun getInstance() : ApplicationClass{
            return instance
        }

        fun getFeatureStore(): SharedPreferences {
            if (spf == null) {
                synchronized(ApplicationClass::class.java) {
                    // synchronized 동기화 블록을 사용하기 때문에 다른 프로세스에서도 동시에 초기화를 시도할 경우
                    // 다른 프로세스가 이미 초기화를 한 다음 synchronized 블록에 진입할 수 있음
                    // 따라서 중복 초기화를 방지하기 위해 spf == null 더블체크가 필요
                    if (spf == null) {
                        spf = getContext().getSharedPreferences(FEATURE_STORE_DATABASE_NAME, Context.MODE_PRIVATE)
                    }
                }
            }
            return spf!!
        }
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
        imageLoader = ImageLoader.Builder(appContext)
            .crossfade(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowRgb565(true)
            .build()
    }
}