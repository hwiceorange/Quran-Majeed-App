package com.raiadnan.quranreader.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.ui.PlayerView;
import com.raiadnan.quranreader.R;

public class LiveActivity extends AppCompatActivity {

    private SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        PlayerView liveView = findViewById(R.id.live_view);


        player = new SimpleExoPlayer.Builder(this)
                        .setMediaSourceFactory(
                                new DefaultMediaSourceFactory(this))
                        .build();

        liveView.setPlayer(player);

        MediaItem mediaItem =
                new MediaItem.Builder()
                        .setUri(getLive())
                        .build();
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        Log.d("TAG", "onCreate: " + getLive());


    }

    private String getLive() {
        return getIntent().getStringExtra("live");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}