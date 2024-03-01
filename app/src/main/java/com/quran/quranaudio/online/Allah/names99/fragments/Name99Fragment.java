package com.quran.quranaudio.online.Allah.names99.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.Allah.fragments.BaseFragment;
import com.quran.quranaudio.online.Allah.names99.adapter.NamesAdapter;
import com.quran.quranaudio.online.Allah.names99.data.NamesData;
import com.quran.quranaudio.online.Allah.names99.helper.Utils;
import com.quran.quranaudio.online.Allah.names99.model.NamesModel;
import com.quran.quranaudio.online.R;

import java.util.ArrayList;

public class Name99Fragment extends BaseFragment {
    
    public String audioTotalTime = "00:00";
    private ImageView btnAudio;
    
    public int currentPosition = -1;
    
    public int delayIndex = 0;
    
    public Handler handler = new Handler();
    
    public LinearLayoutManager layoutManager;
    
    public MediaPlayer mp;
    
    public int[] nameTiming;
    private View nameView;
    
    public NamesAdapter namesAdapter;
    
    public ArrayList<NamesModel> namesData = new ArrayList<>();
    
    public int play = 0;
    private RecyclerView rcvName;
    
    public Runnable runnableTimeUpdate = new Runnable() {
        public void run() {
            if (Name99Fragment.this.mp != null) {
                Name99Fragment.this.handler.removeCallbacks(this);
                int currentPosition = Name99Fragment.this.mp.getCurrentPosition();
                Name99Fragment name99Fragment = Name99Fragment.this;
                String unused = name99Fragment.audioTotalTime = "" + Utils.milliSecondsToTimer((long) (Name99Fragment.this.totalDuration - currentPosition));
                Name99Fragment.this.tvTotalTime.setText(Name99Fragment.this.audioTotalTime);
                Name99Fragment.this.handler.postDelayed(this, 1000);
            }
        }
    };
    
    public SeekBar seekBarNames;
    
    public Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            Name99Fragment.this.focusOnView();
            Name99Fragment.this.handler.postDelayed(this, 0);
        }
    };
    
    public int totalDuration = 0;
    
    public TextView tvTotalTime;
    public int getLayoutId() {
        return R.layout.fragment_99name;
    }

    static  int access$908(Name99Fragment name99Fragment) {
        int i = name99Fragment.delayIndex;
        name99Fragment.delayIndex = i + 1;
        return i;
    }

    public static Name99Fragment newInstance() {
        Bundle bundle = new Bundle();
        Name99Fragment name99Fragment = new Name99Fragment();
        name99Fragment.setArguments(bundle);
        return name99Fragment;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initView();
        this.nameView.setTranslationY((float) getResources().getDisplayMetrics().heightPixels);
        new Handler().postDelayed(new Runnable() {
            public final void run() {
                Name99Fragment.lambda$onViewCreated$1(Name99Fragment.this);
            }
        }, 400);
    }

    public static void lambda$onViewCreated$1(Name99Fragment name99Fragment) {
        if (name99Fragment.isAdded()) {
            name99Fragment.initData();
            name99Fragment.initSb();
            name99Fragment.initAudio();
            name99Fragment.initPlay();
            name99Fragment.initList();
            name99Fragment.nameView.animate().translationY(0.0f).setDuration(250).start();
        }
    }

    private void initPlay() {
        this.btnAudio.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                Name99Fragment.lambda$initPlay$2(Name99Fragment.this, view);
            }
        });
    }

    public static  void lambda$initPlay$2(Name99Fragment name99Fragment, View view) {
        if (name99Fragment.play == 0) {
            name99Fragment.play = 1;
            name99Fragment.handler.removeCallbacks(name99Fragment.sendUpdatesToUI);
            name99Fragment.handler.postDelayed(name99Fragment.sendUpdatesToUI, 0);
            name99Fragment.handler.removeCallbacks(name99Fragment.runnableTimeUpdate);
            name99Fragment.handler.post(name99Fragment.runnableTimeUpdate);
            name99Fragment.mp.start();
            name99Fragment.seekBarNames.setEnabled(true);
            name99Fragment.btnAudio.setImageResource(R.drawable.ic_pause);
            return;
        }
        name99Fragment.handler.removeCallbacks(name99Fragment.sendUpdatesToUI);
        name99Fragment.handler.removeCallbacks(name99Fragment.runnableTimeUpdate);
        name99Fragment.mp.pause();
        name99Fragment.play = 0;
        name99Fragment.seekBarNames.setEnabled(false);
        name99Fragment.btnAudio.setImageResource(R.drawable.ic_play);
    }

    private void initAudio() {
        MediaPlayer mediaPlayer = this.mp;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.mp = null;
        }
        this.mp = new MediaPlayer();
        this.mp = MediaPlayer.create(this.activity, (int) R.raw.allah_full);
        this.mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public final void onPrepared(MediaPlayer mediaPlayer) {
                Name99Fragment.lambda$initAudio$3(Name99Fragment.this, mediaPlayer);
            }
        });
        this.mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public final void onCompletion(MediaPlayer mediaPlayer) {
                Name99Fragment.this.reset();
            }
        });
    }

    public static /* synthetic */ void lambda$initAudio$3(Name99Fragment name99Fragment, MediaPlayer mediaPlayer) {
        name99Fragment.totalDuration = name99Fragment.mp.getDuration();
        name99Fragment.audioTotalTime = "" + Utils.milliSecondsToTimer((long) name99Fragment.totalDuration);
        name99Fragment.tvTotalTime.setText(name99Fragment.audioTotalTime);
    }

    
    public void reset() {
        MediaPlayer mediaPlayer = this.mp;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.mp = null;
        }
        initAudio();
        this.tvTotalTime.setText(this.audioTotalTime);
        this.btnAudio.setImageResource(R.drawable.ic_play);
        this.play = 0;
        this.delayIndex = 0;
        this.namesAdapter.movePosition(0);
        this.layoutManager.scrollToPosition(0);
        this.delayIndex++;
        this.seekBarNames.setProgress(0);
        this.seekBarNames.setEnabled(false);
        this.handler.removeCallbacks(this.runnableTimeUpdate);
        this.handler.removeCallbacks(this.sendUpdatesToUI);
    }

    private void initList() {
        RecyclerView recyclerView = this.rcvName;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.activity);
        this.layoutManager = linearLayoutManager;
        recyclerView.setLayoutManager(linearLayoutManager);
        this.namesAdapter = new NamesAdapter(this.activity, this.namesData) {
            public void OnItemClick(final int i) {
                if (Name99Fragment.this.play == 1) {
                    int unused = Name99Fragment.this.currentPosition = i;
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.post(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.mp.pause();
                    Name99Fragment name99Fragment = Name99Fragment.this;
                    int unused2 = name99Fragment.delayIndex = name99Fragment.currentPosition;
                    Name99Fragment.this.namesAdapter.movePosition(Name99Fragment.this.currentPosition);
                    Name99Fragment.this.layoutManager.scrollToPosition(Name99Fragment.this.currentPosition);
                    Name99Fragment.access$908(Name99Fragment.this);
                    Name99Fragment.this.mp.seekTo(Name99Fragment.this.nameTiming[Name99Fragment.this.currentPosition]);
                    Name99Fragment.this.mp.start();
                    Name99Fragment.this.seekBarNames.setEnabled(true);
                    Name99Fragment.this.seekBarNames.setProgress(Name99Fragment.this.currentPosition);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.handler.post(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.postDelayed(Name99Fragment.this.sendUpdatesToUI, 0);
                    return;
                }

            }
        };
        this.rcvName.setAdapter(this.namesAdapter);
    }

    private void initSb() {
        this.seekBarNames.setMax(99);
        this.seekBarNames.setEnabled(false);
        this.seekBarNames.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (Name99Fragment.this.play == 1 && z) {
                    int unused = Name99Fragment.this.currentPosition = i;
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.post(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.handler.post(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.mp.pause();
                    Name99Fragment name99Fragment = Name99Fragment.this;
                    int unused2 = name99Fragment.delayIndex = name99Fragment.currentPosition;
                    Name99Fragment.this.namesAdapter.movePosition(Name99Fragment.this.currentPosition);
                    Name99Fragment.this.layoutManager.scrollToPosition(Name99Fragment.this.currentPosition);
                    Name99Fragment.access$908(Name99Fragment.this);
                    Name99Fragment.this.mp.seekTo(Name99Fragment.this.nameTiming[Name99Fragment.this.currentPosition]);
                    Name99Fragment.this.mp.start();
                    Name99Fragment.this.seekBarNames.setEnabled(true);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.handler.post(Name99Fragment.this.runnableTimeUpdate);
                    Name99Fragment.this.handler.removeCallbacks(Name99Fragment.this.sendUpdatesToUI);
                    Name99Fragment.this.handler.postDelayed(Name99Fragment.this.sendUpdatesToUI, 0);
                }
            }
        });
    }

    private void initData() {
        NamesData namesData2 = new NamesData(this.activity);
        this.namesData = namesData2.getNamesData();
        this.nameTiming = namesData2.getNameTiming();
    }

    private void initView() {
        this.tvTotalTime = (TextView) this.view.findViewById(R.id.tv_names_total_time);
        this.btnAudio = (ImageView) this.view.findViewById(R.id.btn_play_names_full);
        this.rcvName = (RecyclerView) this.view.findViewById(R.id.rcv_99_name);
        this.seekBarNames = (SeekBar) this.view.findViewById(R.id.seekBarNames);
        this.nameView = this.view.findViewById(R.id.view_name);
    }

    
    public void focusOnView() {
        MediaPlayer mediaPlayer = this.mp;
        if (mediaPlayer != null) {
            int currentPosition2 = mediaPlayer.getCurrentPosition();
            int[] iArr = this.nameTiming;
            int i = this.delayIndex;
            if (currentPosition2 >= iArr[i]) {
                this.currentPosition = i;
                this.namesAdapter.movePosition(this.currentPosition);
                this.layoutManager.scrollToPosition(this.currentPosition);
                this.seekBarNames.setProgress(this.currentPosition);
                int i2 = this.delayIndex;
                if (i2 < this.nameTiming.length - 1) {
                    this.delayIndex = i2 + 1;
                    return;
                }
                return;
            }
            return;
        }
        this.handler.removeCallbacks(this.sendUpdatesToUI);
    }

    public void onDestroy() {
        super.onDestroy();
        MediaPlayer mediaPlayer = this.mp;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.mp = null;
        }
    }
}
