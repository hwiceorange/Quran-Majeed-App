package com.raiadnan.quranreader.prayertimes.locations.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.locations.adapter.CityAdapter;
import com.raiadnan.quranreader.prayertimes.locations.helper.CityDatabase;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.locations.model.City;

public class DialogSelectCity extends BottomSheetDialog {
    public DialogSelectCity(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setContentView((int) R.layout.dialog_city);
        setCancelable(false);
        final CityAdapter r0 = new CityAdapter(getContext(), CityDatabase.get().findAll()) {
            public void OnItemClick(City city) {
                LocationSave.setCity(city.getCity());
                LocationSave.putLocation(Double.parseDouble(city.getLat()), Double.parseDouble(city.getLon()));
                LocationSave.setTimeZone(city.getTimeZone() + "");
                DialogSelectCity.this.dismiss();
            }
        };
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rcv_city);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(r0);
        ((SearchView) findViewById(R.id.edt_search)).setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String str) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String str) {
                r0.setCities(CityDatabase.get().search(str));
                return false;
            }
        /*   public void afterTextChanged(Editable editable) {
            }

            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                r0.setCities(CityDatabase.get().search(charSequence.toString()));
            } */
        });

        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetBehavior.from(((BottomSheetDialog) dialog).findViewById(R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }
}
