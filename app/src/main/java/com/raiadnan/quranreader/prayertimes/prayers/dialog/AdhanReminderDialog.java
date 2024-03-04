package com.raiadnan.quranreader.prayertimes.prayers.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.RadioButton;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;

public class AdhanReminderDialog extends BottomSheetDialog {
    private String key;

    public AdhanReminderDialog(@NonNull Context context, String str, OnDismissListener onDismissListener) {
        super(context);
        this.key = str;
        setOnDismissListener(onDismissListener);
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_adhan_reminder);
        final RadioButton radioButton = (RadioButton) findViewById(R.id.rb_0);
        final RadioButton radioButton2 = (RadioButton) findViewById(R.id.rb_1);
        final RadioButton radioButton3 = (RadioButton) findViewById(R.id.rb_2);
        RadioButton radioButton4 = (RadioButton) findViewById(R.id.rb_3);
        int adhanReminder = PrayerTimeSettingsPref.get().getAdhanReminder(this.key);
        if (adhanReminder == 0) {
            radioButton.setChecked(true);
        } else if (adhanReminder == 5) {
            radioButton2.setChecked(true);
        } else if (adhanReminder == 15) {
            radioButton3.setChecked(true);
        } else if (adhanReminder == 20) {
            radioButton4.setChecked(true);
        }
        findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {


            public final void onClick(View view) {


                if (radioButton.isChecked()) {
                    PrayerTimeSettingsPref.get().setAdhanReminder(AdhanReminderDialog.this.key, 5);
                } else if (radioButton2.isChecked()) {
                    PrayerTimeSettingsPref.get().setAdhanReminder(AdhanReminderDialog.this.key, 15);
                } else if (radioButton3.isChecked()) {
                    PrayerTimeSettingsPref.get().setAdhanReminder(AdhanReminderDialog.this.key, 20);
                } else {
                    PrayerTimeSettingsPref.get().setAdhanReminder(AdhanReminderDialog.this.key, 0);
                }
                AdhanReminderDialog.this.dismiss();
            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                AdhanReminderDialog.this.dismiss();
            }
        });
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetBehavior.from(((BottomSheetDialog) dialog)
                        .findViewById(R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });



    }
}
