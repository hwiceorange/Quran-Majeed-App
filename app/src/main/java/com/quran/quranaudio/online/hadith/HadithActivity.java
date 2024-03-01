package com.quran.quranaudio.online.hadith;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.raiadnan.ads.sdk.format.BannerAd;
import com.quran.quranaudio.online.hadith.book.BookFragment;
import com.quran.quranaudio.online.hadith.bookmark.BookmarkFragment;
import com.quran.quranaudio.online.hadith.search.Hadith_SearchActivity;
import com.quran.quranaudio.online.hadith.settings.SettingsActivity;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.ads.data.Constant;

public class HadithActivity extends AppCompatActivity {

    ActionBarDrawerToggle drawerToggle;

    BannerAd.Builder bannerAd;
    LinearLayout bannerAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hadith);

        ImageView imgFavorite = (ImageView) findViewById(R.id.actionbar_left);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {HadithActivity.this.finish();}

        });


        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, new BookFragment()); // give your fragment container id in first parameter
        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        transaction.commit();

        ImageView actionbarBookmark = this.findViewById(R.id.actionbar_bookmark);
        actionbarBookmark.setOnClickListener(view -> {
            FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
            transaction1.replace(R.id.fragment, new BookmarkFragment()); // give your fragment container id in first parameter
            transaction1.addToBackStack(null);  // if written, this transaction will be added to backstack
            transaction1.commit();
        });

        ImageView actionBarSearch = this.findViewById(R.id.actionbar_search);
        actionBarSearch.setOnClickListener(view -> {
            Intent intent = new Intent(HadithActivity.this, Hadith_SearchActivity.class);
            startActivity(intent);
        });

        ImageView actionbarSettings = this.findViewById(R.id.actionbar_settings);
        actionbarSettings.setOnClickListener(view -> {
            Intent intent = new Intent(HadithActivity.this, SettingsActivity.class);
            startActivity(intent); //, ActivityOptions.makeCustomAnimation(this,android.R.anim.slide_in_left,android.R.anim.slide_out_right).toBundle()
        });

        //ads
        bannerAdView = findViewById(R.id.banner_ad_view);
        bannerAdView.addView(View.inflate(this, R.layout.view_banner_ad, null));
        loadBannerAd();

        //ads*
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
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}