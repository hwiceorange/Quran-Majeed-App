package com.raiadnan.quranreader.sixteenlines;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.DataHolder.ApplicationData;
import com.raiadnan.quranreader.R;
import com.shaheendevelopers.ads.sdk.format.BannerAd;
import com.shaheendevelopers.ads.sdk.format.NativeAd;

import static com.raiadnan.quranreader.sixteenlines.ReadActivity.ITEM;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

public class SixteenLines_MainPage extends AppCompatActivity {

    BannerAd.Builder bannerAd;
    ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if ((new ApplicationData(this)).getTheme()) {
            this.setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen);


        ImageView imgFavorite = (ImageView) findViewById(R.id.back);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SixteenLines_MainPage.this.finish();
            }
        });


        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
        }
        loadBannerAd();
    }


    private void loadBannerAd() {
        bannerAd = new BannerAd.Builder(this)
                .setAdStatus(Constant.AD_STATUS)
                .setAdNetwork(Constant.AD_NETWORK)
                .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
                .setAdMobBannerId(Constant.ADMOB_BANNER_ID)
                .setGoogleAdManagerBannerId(Constant.GOOGLE_AD_MANAGER_BANNER_ID)
                .setFanBannerId(Constant.FAN_BANNER_ID)
                .setUnityBannerId(Constant.UNITY_BANNER_ID)
                .setAppLovinBannerId(Constant.APPLOVIN_BANNER_ID)
                .setAppLovinBannerZoneId(Constant.APPLOVIN_BANNER_ZONE_ID)
                .setDarkTheme(false)
                .build();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    public void Resume(View v){
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Page_No",0);
        ITEM = pref.getInt("resume",10);
        Intent i = new Intent(getApplicationContext(), ReadActivity.class);
        startActivity(i);
    }

    public void Page(View v){
        LayoutInflater layoutInflater =LayoutInflater.from(SixteenLines_MainPage.this);
        final View view = layoutInflater.inflate(R.layout.dialog_box, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(SixteenLines_MainPage.this).create();
        alertDialog.setCancelable(false);
        final EditText etComments = (EditText) view.findViewById(R.id.etComments);
        Button cancelButton =(Button)view.findViewById(R.id.cancel);
        Button goButton = (Button)view.findViewById(R.id.go);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGo(etComments);
            }
        });
        alertDialog.setView(view);
        alertDialog.show();
    }

    public void Juz(View v){
        Intent i = new Intent(getApplicationContext(),JuzContents.class);
        startActivity(i);
        }

    public void Surah(View v){
        Intent i = new Intent(getApplicationContext(),SuraContents.class);
        startActivity(i);
    }

    public void Bookmarks(View v){
        startActivity(new Intent(getApplicationContext(), BookMark.class));
    }

    public void mGo(EditText ed){

        int page_no;
        int code_pg_no;
        int total =549;
        int converter;

        if(ed.getText().toString().equals("")){
            final AlertDialog error2 = new AlertDialog.Builder(SixteenLines_MainPage.this, R.style.AlertDialogStyle).create();
            error2.setMessage("Enter page number");
            error2.show();
        }
        else {
            page_no = Integer.parseInt(ed.getText().toString());
            if (page_no > 550 || page_no < 1) {
                final AlertDialog error = new AlertDialog.Builder(SixteenLines_MainPage.this, R.style.AlertDialogStyle).create();
                error.setMessage("Please enter valid page number");
                error.show();
            }else {
                converter = page_no-1;
                code_pg_no =total-converter;
                Intent i = new Intent(getApplicationContext(), ReadActivity.class);
                ITEM =code_pg_no;
                startActivity(i);
            }
        }
    }
}
