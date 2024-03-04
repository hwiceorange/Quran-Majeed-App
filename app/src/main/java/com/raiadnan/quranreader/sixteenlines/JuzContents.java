package com.raiadnan.quranreader.sixteenlines;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.shaheendevelopers.ads.sdk.format.BannerAd;

public class JuzContents extends AppCompatActivity {
    //LinearLayout scrllayout;
    // Array of strings...
    BannerAd.Builder bannerAd;
    private String[] engArray ;
    private String[] arbArray ;
    private String[] srNumArray;
    private String[] surahNumArray;
    ListView juzList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.raiadnan.quranreader.R.layout.activity_juz_contents);

        ImageView imgFavorite = (ImageView) findViewById(R.id.back);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JuzContents.this.finish();
            }
        });

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
        }

        //scrllayout = (LinearLayout)findViewById(R.id.scrollViewlayout);
///////////////////////////////////////////////////////////////////////////
        engArray = new String[30];
        arbArray = new String[30];
        srNumArray= new String[30];
        surahNumArray=new String[30];
        int page_no;

        for(int index=0; index<30; index++){
            page_no=index+1;

            engArray[index]= getStringResourceByName("para"+page_no);
            arbArray[index]= getStringResourceByName("para"+page_no+"_a");
            surahNumArray[index]=""+page_no;
            srNumArray[index]="para"+page_no;

        }

        juzList =(ListView)findViewById(R.id.juzListView);
        CustomArrayAdapter adapter = new CustomArrayAdapter(this, engArray,arbArray,srNumArray,surahNumArray);
        juzList.setAdapter(adapter);
        loadBannerAd();
    }

    private String getStringResourceByName(String aString) {
        int resId = getResources().getIdentifier(aString, "string", getApplicationContext().getPackageName());
        return getString(resId);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);



    }

}
