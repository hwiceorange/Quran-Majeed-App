package com.raiadnan.quranreader.tasbih.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.tasbih.helper.TasbihManager;
import com.raiadnan.quranreader.prayertimes.utils.Utils;


public class TasbihFragment extends BaseFragment {
    private ImageView btn33;
    private ImageView btnRefresh;
    private ImageView btnSpeak;
    private boolean is33;
    private int speakStatus;
    private ImageView tasbihView;
    private int total;
    private TextView tv33;
    private TextView tvCount;
    private TextView tvTotal;

    public int getLayoutId() {
        return R.layout.fragment_tasbih;
    }

    public static TasbihFragment newInstance() {
        Bundle bundle = new Bundle();
        TasbihFragment tasbihFragment = new TasbihFragment();
        tasbihFragment.setArguments(bundle);
        return tasbihFragment;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initView();
        initToolbar();
        initTasbih();
    }

    private void initTasbih() {
        this.tasbihView.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.this.tasbihClick();
            }
        });
    }

    
    public void tasbihClick() {
        AnimationDrawable animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.tasbih_animation);
        this.tasbihView.setImageDrawable(animationDrawable);
        animationDrawable.start();
        this.total++;
        TasbihManager.get().putTotal(this.total);
        updateText(true);
    }

    @SuppressLint({"SetTextI18n"})
    private void updateText(boolean z) {
        int i;
        this.tv33.setText(this.is33 ? "33" : "99");
        TextView textView = this.tvTotal;
        textView.setText(this.total + "");
        if (this.is33) {
            int i2 = this.total;
            i = i2 % 33;
            if (i == 0 && i2 > 0) {
                if (z) {
                    Utils.vibrator();
                }
                i = 33;
            }
        } else {
            int i3 = this.total;
            i = i3 % 99;
            if (i == 0 && i3 > 0) {
                if (z) {
                    Utils.vibrator();
                }
                i = 99;
            }
        }
        TextView textView2 = this.tvCount;
        textView2.setText(i + "");
        if (z) {
            int i4 = this.speakStatus;
            if (i4 == 0) {
                playSound();
            } else if (i4 == 1) {
                Utils.vibrator();
            }
        }
    }

    private void playSound() {
        MediaPlayer create = MediaPlayer.create(this.activity, (int) R.raw.tasbih_sound);
        if (create != null) {
            create.start();
            create.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });

        }
    }

    private void initView() {
        this.btnSpeak = (ImageView) this.view.findViewById(R.id.bt_speak);
        this.btn33 = (ImageView) this.view.findViewById(R.id.bt_tasbih_count);
        this.btnRefresh = (ImageView) this.view.findViewById(R.id.bt_refresh);
        this.tasbihView = (ImageView) this.view.findViewById(R.id.tasbih_view);
        this.tvCount = (TextView) this.view.findViewById(R.id.tv_tasbih_count);
        this.tv33 = (TextView) this.view.findViewById(R.id.tv_tasbih_33);
        this.tvTotal = (TextView) this.view.findViewById(R.id.tv_tasbih_total);
    }

    private void initToolbar() {
        this.total = TasbihManager.get().getTotal();
        this.speakStatus = TasbihManager.get().getSpeak();
        checkSpeak();
        this.btnSpeak.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$1(TasbihFragment.this, view);
            }
        });
        this.is33 = TasbihManager.get().is33();
        check33();
        this.btn33.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$2(TasbihFragment.this, view);
            }
        });
        this.btnRefresh.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$4(TasbihFragment.this, view);
            }
        });
        updateText(false);
    }

    public static /* synthetic */ void lambda$initToolbar$1(TasbihFragment tasbihFragment, View view) {
        int i = tasbihFragment.speakStatus;
        if (i == 0) {
            tasbihFragment.speakStatus = 1;
            TasbihManager.get().putSpeak(1);
            Utils.vibrator();
        } else if (i == 1) {
            tasbihFragment.speakStatus = 2;
            TasbihManager.get().putSpeak(2);
        } else {
            tasbihFragment.speakStatus = 0;
            TasbihManager.get().putSpeak(0);
        }
        tasbihFragment.checkSpeak();
    }

    public static /* synthetic */ void lambda$initToolbar$2(TasbihFragment tasbihFragment, View view) {
        if (tasbihFragment.is33) {
            tasbihFragment.is33 = false;
            TasbihManager.get().put33(false);
        } else {
            tasbihFragment.is33 = true;
            TasbihManager.get().put33(true);
        }
        tasbihFragment.check33();
        tasbihFragment.updateText(false);
    }

    public static /* synthetic */ void lambda$initToolbar$4(final TasbihFragment tasbihFragment, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(tasbihFragment.activity);
        builder.setMessage("Reset your current and total tasbih counts to zezo?");
        builder.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                tasbihFragment.total = 0;
                tasbihFragment.is33 = true;
                tasbihFragment.speakStatus = 0;
                TasbihManager.get().put33(true);
                TasbihManager.get().putSpeak(0);
                TasbihManager.get().putTotal(0);
                tasbihFragment.check33();
                tasbihFragment.checkSpeak();
                tasbihFragment.updateText(false);
            }
        });
        builder.setNegativeButton("CANCEL", (DialogInterface.OnClickListener) null);
        builder.show();
    }



    private void check33() {
        if (this.is33) {
            this.btn33.setImageResource(R.drawable.ic_33);
        } else {
            this.btn33.setImageResource(R.drawable.ic_99);
        }
    }

    private void checkSpeak() {
        int i = this.speakStatus;
        if (i == 0) {
            this.btnSpeak.setImageResource(R.drawable.ic_volume);
        } else if (i == 1) {
            this.btnSpeak.setImageResource(R.drawable.ic_vibration);
        } else {
            this.btnSpeak.setImageResource(R.drawable.ic_volume_off);
        }
    }
}
