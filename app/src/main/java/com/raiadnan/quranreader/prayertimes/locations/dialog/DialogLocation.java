package com.raiadnan.quranreader.prayertimes.locations.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.locations.helper.AddressHelper;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import java.util.Date;
import java.util.TimeZone;

public class DialogLocation extends BottomSheetDialog {
    private Activity activity;
    private Callback callback;

    public interface Callback {
        void OnLoctionGetSuccess();

        void OnSelectCityClick();
    }

    public DialogLocation(@NonNull Activity activity2, Callback callback2) {
        super(activity2);
        this.activity = activity2;
        this.callback = callback2;
        init();
    }

    @SuppressLint({"SetTextI18n"})
    private void init() {
        setContentView((int) R.layout.dialog_location);
        setCancelable(false);
        final View findViewById = findViewById(R.id.pb_load);
        final TextView textView = (TextView) findViewById(R.id.bt_cancel);
        final TextView textView2 = (TextView) findViewById(R.id.bt_ok);
        final TextView textView3 = (TextView) findViewById(R.id.tv_load);
        new Handler().postDelayed(new Runnable() {


            public final void run() {
                if (DialogLocation.this.isShowing()) {
                    DialogLocation.this.getLocation(textView,findViewById ,textView2, textView3);
                }
            }
        }, 1000);
        textView.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                DialogLocation.lambda$init$1(DialogLocation.this, view);
            }
        });
        textView2.setOnClickListener(new View.OnClickListener() {

            public final void onClick(View view) {
                textView.setText("Turn On Your Location");
                textView2.setVisibility(View.INVISIBLE);
                view.setVisibility(View.VISIBLE);
                DialogLocation.this.getLocation(textView, view, textView3, textView2);
            }
        });
    }



    public static /* synthetic */ void lambda$init$1(DialogLocation dialogLocation, View view) {
        dialogLocation.callback.OnSelectCityClick();
        dialogLocation.callback = null;
    }


    @SuppressLint({"SetTextI18n"})
    private void getLocation(final TextView textView, final View view, final TextView textView2, final TextView textView3) {
        LocationServices.getFusedLocationProviderClient(this.activity)
                .getLastLocation().addOnCompleteListener(this.activity,
                new OnCompleteListener() {


            public final void onComplete(Task task) {
                view.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Location location = (Location) task.getResult();
                    if (location != null) {
                        LocationSave.putLocation(location.getLatitude(), location.getLongitude());
                        LocationSave.setTimeZone(String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60));
                        AddressHelper.getAddress(location.getLatitude(), location.getLongitude());
                        Callback callback2 = DialogLocation.this.callback;
                        if (callback2 != null) {
                            callback2.OnLoctionGetSuccess();
                        }
                        textView.setText("Get location success!");
                        textView2.setText("OK");
                        return;
                    }
                    textView.setText("Tap to Find Location Offline");
                    textView3.setVisibility(View.GONE);
                    return;
                }
                textView.setText("Tap to Find Location Offline");
                textView3.setVisibility(View.VISIBLE);
            }
        });
    }


}
