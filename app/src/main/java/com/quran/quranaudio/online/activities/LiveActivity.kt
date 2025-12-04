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
        // Get backup URL list
        backupUrls = intent.getStringArrayExtra("backup_urls")
        
        val liveView = findViewById<PlayerView>(R.id.live_view)
        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
            )
            .build()
        liveView.player = player
        
        // Try to play current URL
        tryPlayUrl(live)
        
        // Add player event listener
        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                android.util.Log.e("LiveActivity", "Player error: " + error.message)
                android.util.Log.e("LiveActivity", "Error cause: " + error.cause?.message)
                
                // Try next backup URL
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
            Toast.makeText(this, R.string.live_url_empty, Toast.LENGTH_LONG).show()
            return
        }
        
        // Check if it's a YouTube URL
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            android.util.Log.d("LiveActivity", "YouTube URL detected, opening in browser/YouTube app")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.setPackage("com.google.android.youtube") // Try to use YouTube app
                startActivity(intent)
                finish() // Close current Activity
                return
            } catch (e: Exception) {
                // If YouTube app doesn't exist, open in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                finish()
                return
            }
        }
        
        // For other URLs, use ExoPlayer to play
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
            Toast.makeText(this, getString(R.string.live_trying_backup, currentUrlIndex + 1), Toast.LENGTH_SHORT).show()
            tryPlayUrl(nextUrl)
        } else {
            android.util.Log.e("LiveActivity", "All URLs failed")
            Toast.makeText(this, R.string.live_all_sources_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (player != null) {
            player!!.release()
        }
    }
}