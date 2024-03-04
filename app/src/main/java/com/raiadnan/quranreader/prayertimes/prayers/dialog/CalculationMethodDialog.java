package com.raiadnan.quranreader.prayertimes.prayers.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.adapter.CalculationItemAdapter;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;

public class CalculationMethodDialog extends BottomSheetDialog {
    String[] methodAngles;
    int[] methodIndexs;
    String[] methodNames;

    public CalculationMethodDialog(@NonNull Context context, OnDismissListener onDismissListener) {
        super(context);
        setOnDismissListener(onDismissListener);
        init();
    }

    private void init() {
        this.methodNames = getContext().getResources().getStringArray(R.array.array_calculation_methods_new);
        this.methodAngles = getContext().getResources().getStringArray(R.array.array_calculation_methods_angles);
        this.methodIndexs = getContext().getResources().getIntArray(R.array.array_calculation_methods_index);
        setContentView((int) R.layout.dialog_calculation_method);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rcv_calculation);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final CalculationItemAdapter calculationItemAdapter = new CalculationItemAdapter(getContext(), this.methodNames, this.methodAngles);
        recyclerView.setAdapter(calculationItemAdapter);
        findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {


            public final void onClick(View view) {

                PrayerTimeSettingsPref.get().setCalculationMethodIndex(calculationItemAdapter.getPosition());
                PrayerTimeSettingsPref.get().setCalculationMethod(CalculationMethodDialog.this.methodIndexs[calculationItemAdapter.getPosition()]);
                CalculationMethodDialog.this.dismiss();
            }
        });
        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                CalculationMethodDialog.this.dismiss();
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
