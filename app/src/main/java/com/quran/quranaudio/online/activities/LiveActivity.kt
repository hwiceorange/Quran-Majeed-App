@file:Suppress("DEPRECATION")

package com.quran.quranaudio.online.activities

import android.os.Bundle
import android.view.WindowManager
import com.quran.quranaudio.online.BaseActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.quran.quranaudio.online.R
import timber.log.Timber

class LiveActivity : BaseActivity() {
    private var player: SimpleExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        
        val liveView = findViewById<PlayerView>(R.id.live_view)
        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
            )
            .build()
        liveView.player = player
        val mediaItem = MediaItem.Builder()
            .setUri(live)
            .build()
        player!!.setMediaItem(mediaItem)
        player!!.prepare()
        player!!.playWhenReady = true
        Timber.tag("TAG").d("onCreate: %s", live)
    }

    // LiveActivity是全屏视频播放，排除系统栏内边距处理
    override fun isExcludedFromSystemBarInsets(): Boolean {
        return true
    }

    // 使用深色状态栏适合视频播放
    override fun isLightStatusBar(): Boolean {
        return false
    }

    private val live: String?
        get() = intent.getStringExtra("live")

    override fun onDestroy() {
        super.onDestroy()
        if (player != null) {
            player!!.release()
        }
    }
}