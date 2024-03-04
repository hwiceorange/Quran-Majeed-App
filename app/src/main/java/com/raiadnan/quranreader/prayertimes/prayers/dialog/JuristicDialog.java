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

public class JuristicDialog extends BottomSheetDialog {
    public JuristicDialog(@NonNull Context context, OnDismissListener onDismissListener) {
        super(context);
        setOnDismissListener(onDismissListener);
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_juristic);
        final RadioButton radioButton = (RadioButton) findViewById(R.id.rb_0);
        RadioButton radioButton2 = (RadioButton) findViewById(R.id.rb_1);
        if (PrayerTimeSettingsPref.get().getJuristic() == 1) {
            radioButton.setChecked(true);
        } else {
            radioButton2.setChecked(true);
        }
        findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {


            public final void onClick(View view) {
                PrayerTimeSettingsPref.get().setJuristic(radioButton.isChecked() ? 1 : 2);
                JuristicDialog.this.dismiss();
            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                JuristicDialog.this.dismiss();
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
