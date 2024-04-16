package com.quran.quranaudio.online.prayertimes.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.Utils.GifImageView;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.R;

import javax.inject.Inject;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class MainActivity extends BaseActivity {

   /* ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;*/

    @Inject
    PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);

     /*   //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        //Permission End*/

     //   requestPermission();


        NavController navController = Navigation.findNavController(this, R.id.home_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graphmain);

        if (displaySettingsScreenFirst()) {
            navGraph.setStartDestination(R.id.nav_home);
        } else {
            navGraph.setStartDestination(R.id.nav_home);
        }

        navController.setGraph(navGraph);
        preferencesHelper.setFirstTimeLaunch(false);

        WorkCreator.schedulePeriodicPrayerUpdater(this);
    }
/*
    private void requestPermission(){
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
              Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {

            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }
*/

    private boolean displaySettingsScreenFirst() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_DENIED) &&
                preferencesHelper.isFirstLaunch();
    }

    public void onBackPressed() {

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.adview_layout_exit);
        ((GifImageView) dialog.findViewById(R.id.GifImageView)).setGifImageResource(R.drawable.rate);
        ((Button) dialog.findViewById(R.id.btnno)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {dialog.dismiss();}
        });
        ((Button) dialog.findViewById(R.id.btnrate)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    MainActivity mainActivity = MainActivity.this;
                    mainActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + MainActivity.this.getPackageName())));
                } catch (ActivityNotFoundException unused) {
                    MainActivity mainActivity2 = MainActivity.this;
                    mainActivity2.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + MainActivity.this.getPackageName())));
                }
            }
        });
        ((Button) dialog.findViewById(R.id.btnyes)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                finish();
             //   System.exit(0);


            }
        });
        dialog.show();
    }

}
