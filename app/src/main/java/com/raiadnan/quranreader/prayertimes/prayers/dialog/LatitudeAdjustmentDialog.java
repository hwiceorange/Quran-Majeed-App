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

public class LatitudeAdjustmentDialog extends BottomSheetDialog {
    public LatitudeAdjustmentDialog(@NonNull Context context, OnDismissListener onDismissListener) {
        super(context);
        setOnDismissListener(onDismissListener);
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_latitude_adjustment);
        final RadioButton radioButton = (RadioButton) findViewById(R.id.rb_0);
        final RadioButton radioButton2 = (RadioButton) findViewById(R.id.rb_1);
        final RadioButton radioButton3 = (RadioButton) findViewById(R.id.rb_2);
        RadioButton radioButton4 = (RadioButton) findViewById(R.id.rb_3);
        switch (PrayerTimeSettingsPref.get().getLatdAdjst()) {
            case 1:
                radioButton.setChecked(true);
                break;
            case 2:
                radioButton2.setChecked(true);
                break;
            case 3:
                radioButton3.setChecked(true);
                break;
            case 4:
                radioButton4.setChecked(true);
                break;
        }
        findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {

            public final void onClick(View view) {

                if (radioButton.isChecked()) {
                    PrayerTimeSettingsPref.get().setsLatdAdjst(1);
                } else if (radioButton2.isChecked()) {
                    PrayerTimeSettingsPref.get().setsLatdAdjst(2);
                } else if (radioButton3.isChecked()) {
                    PrayerTimeSettingsPref.get().setsLatdAdjst(3);
                } else {
                    PrayerTimeSettingsPref.get().setsLatdAdjst(4);
                }
                LatitudeAdjustmentDialog.this.dismiss();




            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                LatitudeAdjustmentDialog.this.dismiss();
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
