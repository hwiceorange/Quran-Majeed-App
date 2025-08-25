@file:Suppress("DEPRECATION")

package com.quran.quranaudio.online.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.quran.quranaudio.online.R
import timber.log.Timber
import android.widget.Toast
import android.content.Intent
import android.net.Uri

class LiveActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null
    private var backupUrls: Array<String>? = null
    private var currentUrlIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        window.navigationBarColor = resources.getColor(R.color.black)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // 获取备用URL列表
        backupUrls = intent.getStringArrayExtra("backup_urls")
        
        val liveView = findViewById<PlayerView>(R.id.live_view)
        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
            )
            .build()
        liveView.player = player
        
        // 尝试播放当前URL
        tryPlayUrl(live)
        
        // 添加播放器事件监听器
        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                android.util.Log.e("LiveActivity", "Player error: " + error.message)
                android.util.Log.e("LiveActivity", "Error cause: " + error.cause?.message)
                
                // 尝试下一个备用URL
                tryNextUrl()
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        android.util.Log.d("LiveActivity", "Player state: BUFFERING")
                    }
                    Player.STATE_READY -> {
                        android.util.Log.d("LiveActivity", "Player state: READY")
                    }
                    Player.STATE_ENDED -> {
                        android.util.Log.d("LiveActivity", "Player state: ENDED")
                    }
                    Player.STATE_IDLE -> {
                        android.util.Log.d("LiveActivity", "Player state: IDLE")
                    }
                }
            }
        })
    }

    private val live: String?
        get() = intent.getStringExtra("live")
    
    private fun tryPlayUrl(url: String?) {
        android.util.Log.d("LiveActivity", "Trying to play URL: $url")
        
        if (url.isNullOrEmpty()) {
            android.util.Log.e("LiveActivity", "URL is null or empty!")
            Toast.makeText(this, "直播URL为空", Toast.LENGTH_LONG).show()
            return
        }
        
        // 检查是否是YouTube URL
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            android.util.Log.d("LiveActivity", "YouTube URL detected, opening in browser/YouTube app")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.setPackage("com.google.android.youtube") // 尝试使用YouTube app
                startActivity(intent)
                finish() // 关闭当前Activity
                return
            } catch (e: Exception) {
                // 如果YouTube app不存在，用浏览器打开
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                finish()
                return
            }
        }
        
        // 对于其他URL，使用ExoPlayer播放
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .build()
        player!!.setMediaItem(mediaItem)
        player!!.prepare()
        player!!.playWhenReady = true
    }
    
    private fun tryNextUrl() {
        if (backupUrls != null && currentUrlIndex < backupUrls!!.size - 1) {
            currentUrlIndex++
            val nextUrl = backupUrls!![currentUrlIndex]
            android.util.Log.d("LiveActivity", "Trying backup URL $currentUrlIndex: $nextUrl")
            Toast.makeText(this, "尝试备用直播源 ${currentUrlIndex + 1}", Toast.LENGTH_SHORT).show()
            tryPlayUrl(nextUrl)
        } else {
            android.util.Log.e("LiveActivity", "All URLs failed")
            Toast.makeText(this, "所有直播源都无法连接", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (player != null) {
            player!!.release()
        }
    }
}