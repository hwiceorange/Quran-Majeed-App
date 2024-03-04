package com.raiadnan.quranreader.prayertimes.prayers.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.shawnlin.numberpicker.NumberPicker;

public class ManualCorrectionDialog extends BottomSheetDialog {
    public ManualCorrectionDialog(@NonNull Context context, OnDismissListener onDismissListener) {
        super(context);
        setOnDismissListener(onDismissListener);
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_manual_correction);
        final TextView textView = (TextView) findViewById(R.id.tv_fajar_value);
        final TextView textView2 = (TextView) findViewById(R.id.tv_sun_rise_value);
        final TextView textView3 = (TextView) findViewById(R.id.tv_duhar_value);
        final TextView textView4 = (TextView) findViewById(R.id.tv_asar_value);
        final TextView textView5 = (TextView) findViewById(R.id.tv_maghrib_value);
        final TextView textView6 = (TextView) findViewById(R.id.tv_esha_value);
        final NumberPicker numberPicker = (NumberPicker) findViewById(R.id.np_fajar);
        final NumberPicker numberPicker2 = (NumberPicker) findViewById(R.id.np_sun_rise);
        final NumberPicker numberPicker3 = (NumberPicker) findViewById(R.id.np_duhar);
        int correctionsFajr = PrayerTimeSettingsPref.get().getCorrectionsFajr();
        int correctionsSunrize = PrayerTimeSettingsPref.get().getCorrectionsSunrize();
        int correctionsZuhar = PrayerTimeSettingsPref.get().getCorrectionsZuhar();
        int correctionsAsar = PrayerTimeSettingsPref.get().getCorrectionsAsar();
        final NumberPicker numberPicker4 = (NumberPicker) findViewById(R.id.np_esha);
        int correctionsMaghrib = PrayerTimeSettingsPref.get().getCorrectionsMaghrib();
        final NumberPicker numberPicker5 = (NumberPicker) findViewById(R.id.np_maghrib);
        int correctionsIsha = PrayerTimeSettingsPref.get().getCorrectionsIsha();
        final NumberPicker numberPicker6 = (NumberPicker) findViewById(R.id.np_asar);
        textView.setText(String.valueOf(correctionsFajr));
        textView2.setText(String.valueOf(correctionsSunrize));
        textView3.setText(String.valueOf(correctionsZuhar));
        textView4.setText(String.valueOf(correctionsAsar));
        textView5.setText(String.valueOf(correctionsMaghrib));
        textView6.setText(String.valueOf(correctionsIsha));
        numberPicker.setValue(correctionsFajr);
        numberPicker2.setValue(correctionsSunrize);
        numberPicker3.setValue(correctionsZuhar);
        NumberPicker numberPicker7 = numberPicker6;
        numberPicker7.setValue(correctionsAsar);
        NumberPicker numberPicker8 = numberPicker5;
        numberPicker8.setValue(correctionsMaghrib);
        NumberPicker numberPicker9 = numberPicker4;
        numberPicker9.setValue(correctionsIsha);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView.setText(String.valueOf(i2));
            }
        });
        numberPicker2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView2.setText(String.valueOf(i2));
            }
        });
        numberPicker3.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView3.setText(String.valueOf(i2));
            }
        });
        numberPicker7.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {


            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView4.setText(String.valueOf(i2));
            }
        });
        numberPicker8.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {


            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView5.setText(String.valueOf(i2));
            }
        });
        numberPicker9.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {


            public final void onValueChange(NumberPicker numberPicker, int i, int i2) {
                textView6.setText(String.valueOf(i2));
            }
        });
        findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {


            public final void onClick(View view) {
                PrayerTimeSettingsPref.get().setCorrectionsFajr(numberPicker.getValue());
                PrayerTimeSettingsPref.get().setCorrectionsSunrize(numberPicker2.getValue());
                PrayerTimeSettingsPref.get().setCorrectionsZuhar(numberPicker3.getValue());
                PrayerTimeSettingsPref.get().setCorrectionsAsar(numberPicker4.getValue());
                PrayerTimeSettingsPref.get().setCorrectionsMaghrib(numberPicker5.getValue());
                PrayerTimeSettingsPref.get().setCorrectionsIsha(numberPicker6.getValue());
                ManualCorrectionDialog.this.dismiss();

            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                ManualCorrectionDialog.this.dismiss();
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
