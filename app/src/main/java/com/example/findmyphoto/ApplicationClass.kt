package com.example.findmyphoto

import android.app.Application
import android.content.Context

class ApplicationClass : Application() {
    private lateinit var appContext: Context

    companion object {
        private lateinit var instance: ApplicationClass

        fun getContext(): Context {
            return instance.appContext
        }

        fun getInstance() : ApplicationClass{
            return instance
        }
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
    }
}