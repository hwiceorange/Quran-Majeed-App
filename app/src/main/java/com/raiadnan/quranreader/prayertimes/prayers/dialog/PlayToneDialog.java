package com.raiadnan.quranreader.prayertimes.prayers.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.ToneHelper;
import com.raiadnan.quranreader.prayertimes.utils.Utils;

public class PlayToneDialog extends BottomSheetDialog {
    
    public Handler handler = new Handler();
    private int position;
    private Runnable runnable;

    public PlayToneDialog(@NonNull Context context, int i) {
        super(context);
        this.position = i;
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_play_tone);
        final TextView textView = (TextView) findViewById(R.id.tv_total_time);
        final ImageView imageView = (ImageView) findViewById(R.id.bt_play);
        final SeekBar seekBar = (SeekBar) findViewById(R.id.sb_audio);
        ((TextView) findViewById(R.id.tv_title)).setText(ToneHelper.get().getToneStringFromPosition(this.position));
        final MediaPlayer create = MediaPlayer.create(getContext(), ToneHelper.get().getToneResoureFromPosition(this.position));
        create.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            public final void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                imageView.setImageResource(R.drawable.ic_pause);
                PlayToneDialog.this.handler.postDelayed(PlayToneDialog.this.runnable, 100);
                seekBar.setMax(mediaPlayer.getDuration());
                textView.setText(Utils.milliSecondsToTimer((long) mediaPlayer.getDuration()));

            }
        });
        this.runnable = new Runnable() {
            public void run() {
                if (create.isPlaying()) {
                    textView.setText(Utils.milliSecondsToTimer((long) create.getDuration()));
                    seekBar.setProgress(create.getCurrentPosition());
                }
                PlayToneDialog.this.handler.postDelayed(this, 100);
            }
        };
        setOnDismissListener(new OnDismissListener() {


            public final void onDismiss(DialogInterface dialogInterface) {

                PlayToneDialog.this.handler.removeCallbacks(PlayToneDialog.this.runnable);
                if (create != null) {
                    create.release();
                }
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {


            public final void onClick(View view) {

                if (create.isPlaying()) {
                    create.pause();
                    imageView.setImageResource(R.drawable.ic_play);
                    PlayToneDialog.this.handler.removeCallbacks(PlayToneDialog.this.runnable);
                    return;
                }
                create.start();
                imageView.setImageResource(R.drawable.ic_pause);
                PlayToneDialog.this.handler.postDelayed(PlayToneDialog.this.runnable, 100);
            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PlayToneDialog.this.dismiss();
            }
        });
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetBehavior.from(((BottomSheetDialog) dialog).findViewById(R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }




}
