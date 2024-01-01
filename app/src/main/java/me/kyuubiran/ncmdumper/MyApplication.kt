package me.kyuubiran.ncmdumper

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("ncmdumper")
    }
}