package com.zaaam.liphify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import com.zaaam.liphify.data.youtube.OkHttpDownloader

@HiltAndroidApp
class LiPhifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // NewPipeExtractor wajib init downloader sekali (PRD-003/004).
        try {
            NewPipe.init(OkHttpDownloader())
        } catch (_: Throwable) {
            android.util.Log.w("LiPhifyApp", "NewPipe init gagal — fitur YouTube nonaktif")
        }
    }
}
