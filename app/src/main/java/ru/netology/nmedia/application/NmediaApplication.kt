package ru.netology.nmedia.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.netology.nmedia.auth.AppAuth

@HiltAndroidApp
class NmediaApplication: Application() {
//    override fun onCreate() {
//        super.onCreate()
//        AppAuth.init(this)
//    }
}