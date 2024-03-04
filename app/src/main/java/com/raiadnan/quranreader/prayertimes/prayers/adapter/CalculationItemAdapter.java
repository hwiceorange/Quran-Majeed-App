package com.raiadnan.quranreader.prayertimes.prayers.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;

public class CalculationItemAdapter extends RecyclerView.Adapter<CalculationItemAdapter.ViewHolder> {
    private Context context;
    private String[] methodAngles;
    private String[] methodNames;
    private int position = PrayerTimeSettingsPref.get().getCalculationMethodIndex();
    private RadioButton rdCheck;

    public CalculationItemAdapter(Context context2, String[] strArr, String[] strArr2) {
        this.context = context2;
        this.methodNames = strArr;
        this.methodAngles = strArr2;
    }

    public int getPosition() {
        return this.position;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_calculation_method, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {
        viewHolder.title.setText(this.methodNames[i]);
        viewHolder.value.setText(this.methodAngles[i]);
        if (this.position == i) {
            this.rdCheck = viewHolder.rdItem;
            viewHolder.rdItem.setChecked(true);
        } else {
            viewHolder.rdItem.setChecked(false);
        }
        viewHolder.rdItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {


            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                if (z) {
                    RadioButton radioButton = CalculationItemAdapter.this.rdCheck;
                    if (radioButton != null) {
                        radioButton.setChecked(false);
                    }
                    CalculationItemAdapter.this.position = i;
                    CalculationItemAdapter.this.rdCheck = viewHolder.rdItem;
                }

            }
        });
    }


    public int getItemCount() {
        return this.methodAngles.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public RadioButton rdItem;
        
        public TextView title;
        
        public TextView value;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.tv_title);
            this.value = (TextView) view.findViewById(R.id.tv_value);
            this.rdItem = (RadioButton) view.findViewById(R.id.rd_item);
        }
    }
}
