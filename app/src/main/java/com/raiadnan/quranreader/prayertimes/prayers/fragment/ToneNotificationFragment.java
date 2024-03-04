package com.raiadnan.quranreader.prayertimes.prayers.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.prayertimes.prayers.adapter.ToneAdapter;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.AdhanReminderDialog;
import com.raiadnan.quranreader.prayertimes.prayers.dialog.PlayToneDialog;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.prayers.helper.ToneHelper;

public class ToneNotificationFragment extends BaseFragment {
    private View btnAdhan;
    private App.SimpleCallback callback;
    private String key;
    
    public PlayToneDialog playToneDialog;
    private AdhanReminderDialog reminderDialog;
    private TextView tvAdhanTime;
    private TextView tvTitle;

    public int getLayoutId() {
        return R.layout.fragment_tone;
    }

    public static ToneNotificationFragment newInstance() {
        Bundle bundle = new Bundle();
        ToneNotificationFragment toneNotificationFragment = new ToneNotificationFragment();
        toneNotificationFragment.setArguments(bundle);
        return toneNotificationFragment;
    }

    /* access modifiers changed from: package-private */
    public void setCallback(App.SimpleCallback simpleCallback) {
        this.callback = simpleCallback;
    }

    public void setPrayItem(String str) {
        this.key = str;
    }

    @SuppressLint({"SetTextI18n"})
    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initView();
        click();
        setTextAdhanReminder();
        initListTone();
        TextView textView = this.tvTitle;
        textView.setText(this.key + " Notification");
    }

    private void initListTone() {
        RecyclerView recyclerView = (RecyclerView) this.view.findViewById(R.id.rcv_tone);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity));
        recyclerView.setAdapter(new ToneAdapter(this.activity, ToneHelper.get().getTones(), this.key) {
            public void OnPlayClick(int i) {
                if (i == 2) {
                    ToneNotificationFragment.this.playDefaultTone();
                } else if (ToneNotificationFragment.this.playToneDialog == null || !ToneNotificationFragment.this.playToneDialog.isShowing()) {
                    ToneNotificationFragment toneNotificationFragment = ToneNotificationFragment.this;
                    PlayToneDialog unused = toneNotificationFragment.playToneDialog = new PlayToneDialog(toneNotificationFragment.activity, i);
                    ToneNotificationFragment.this.playToneDialog.show();
                }
            }
        });
    }

    
    public void playDefaultTone() {
        RingtoneManager.getRingtone(this.activity, RingtoneManager.getDefaultUri(2)).play();
    }

    private void click() {
        this.btnAdhan.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {

                AdhanReminderDialog adhanReminderDialog = ToneNotificationFragment.this.reminderDialog;
                if (adhanReminderDialog == null || !adhanReminderDialog.isShowing()) {
                    ToneNotificationFragment.this.reminderDialog = new AdhanReminderDialog
                            (ToneNotificationFragment.this.activity, ToneNotificationFragment.this.key,
                                    new DialogInterface.OnDismissListener() {
                                        public final void onDismiss(DialogInterface dialogInterface) {
                                            ToneNotificationFragment.this.setTextAdhanReminder();
                                        }
                                    });
                    ToneNotificationFragment.this.reminderDialog.show();
                }
            }
        });
    }

    public void setTextAdhanReminder() {
        String str;
        int adhanReminder = PrayerTimeSettingsPref.get().getAdhanReminder(this.key);
        TextView textView = this.tvAdhanTime;
        if (adhanReminder == 0) {
            str = "None";
        } else {
            str = adhanReminder + " mins";
        }
        textView.setText(str);
    }

    private void initView() {
        this.btnAdhan = this.view.findViewById(R.id.bt_adhan_reminder);
        this.tvAdhanTime = (TextView) this.view.findViewById(R.id.tv_time_adhan);
        this.tvTitle = (TextView) this.view.findViewById(R.id.tv_title);
    }

    public void onDestroy() {
        super.onDestroy();
        App.SimpleCallback simpleCallback = this.callback;
        if (simpleCallback != null) {
            simpleCallback.callback(0);
        }
    }
}
