package com.raiadnan.quranreader.prayertimes.locations.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.raiadnan.quranreader.activities.HomeActivity;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.fragments.SettingFragment;
import com.raiadnan.quranreader.prayertimes.locations.adapter.CityAdapter;
import com.raiadnan.quranreader.prayertimes.locations.helper.AddressHelper;
import com.raiadnan.quranreader.prayertimes.locations.helper.CityDatabase;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.locations.model.City;
import com.raiadnan.quranreader.prayertimes.utils.Utils;

import java.util.Date;
import java.util.TimeZone;

public class LocationSettingFragment extends BaseFragment {
    private App.SimpleCallback callback;

    public int getLayoutId() {
        return R.layout.fragment_location_setting;
    }

    public static LocationSettingFragment newInstance() {
        Bundle bundle = new Bundle();
        LocationSettingFragment locationSettingFragment = new LocationSettingFragment();
        locationSettingFragment.setArguments(bundle);
        return locationSettingFragment;
    }

    public void setCallback(App.SimpleCallback simpleCallback) {
        this.callback = simpleCallback;
    }

    public void onViewCreated(@NonNull final View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        View findViewById = view.findViewById(R.id.view_search);
        final SearchView searchView = (SearchView) view.findViewById(R.id.edt_search);
        Switch switchR = (Switch) view.findViewById(R.id.sw_auto_location);
        switchR.setChecked(LocationSave.isAutoUpdate());
     //   findViewById.setVisibility(LocationSave.isAutoUpdate() ? View.VISIBLE : View.VISIBLE);
        switchR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {


            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {

                LocationSave.setAutoUpdate(z);
                view.setVisibility(z ? View.VISIBLE : View.VISIBLE);
                if (z) {
                    Utils.hideKeyboard(LocationSettingFragment.this.activity);
                    ((HomeActivity) LocationSettingFragment.this.activity).mRequestObject
                            = PermissionUtil.with(LocationSettingFragment.this.activity)
                            .request("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION").onAllGranted(new Func() {
                                /* access modifiers changed from: protected */
                                @SuppressLint({"MissingPermission", "SetTextI18n"})
                                public void call() {
                                    LocationSettingFragment.this.getMyLocation();
                                }
                            }).ask(12);
                    return;
                }
                Utils.showKeyboard(searchView);


            }
        });
        final CityAdapter r5 = new CityAdapter(getContext(), CityDatabase.get().findAll()) {
            public void OnItemClick(City city) {
                LocationSave.setCity(city.getCity());
                LocationSave.putLocation(Double.parseDouble(city.getLat()), Double.parseDouble(city.getLon()));
                LocationSave.setTimeZone(city.getTimeZone() + "");
               // LocationSettingFragment.this.activity.getSupportFragmentManager().popBackStack();
            }
        };
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rcv_city);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(r5);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String str) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String str) {
                r5.setCities(CityDatabase.get().search(str));
                return false;
            }
        });

        ImageView imgBack = (ImageView) view.findViewById(R.id.setting_btn_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new SettingFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.home_host_fragment, fragment).commit();

            }
        });
    }




    @SuppressLint("MissingPermission")
    public void getMyLocation() {
        LocationServices.getFusedLocationProviderClient((Activity) this.activity)
                .getLastLocation().addOnCompleteListener(this.activity,
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null && LocationSave.isAutoUpdate()) {
                                    Location location = (Location) task.getResult();
                                    LocationSave.putLocation(location.getLatitude(), location.getLongitude());
                                    LocationSave.setTimeZone(String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60));
                                    AddressHelper.getAddress(location.getLatitude(), location.getLongitude());
                                }
                            }
                        });
    }

    static /* synthetic */ void lambda$getMyLocation$1(Task task) {
        if (task.isSuccessful() && task.getResult() != null && LocationSave.isAutoUpdate()) {
            Location location = (Location) task.getResult();
            LocationSave.putLocation(location.getLatitude(), location.getLongitude());
            LocationSave.setTimeZone(String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60));
            AddressHelper.getAddress(location.getLatitude(), location.getLongitude());
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Utils.hideKeyboard(this.activity);
        App.SimpleCallback simpleCallback = this.callback;
        if (simpleCallback != null) {
            simpleCallback.callback(Integer.valueOf(SettingFragment.LOCATION_CHANGE));
        }
    }
}
