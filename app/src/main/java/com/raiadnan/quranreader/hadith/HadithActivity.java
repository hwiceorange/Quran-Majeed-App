package com.raiadnan.quranreader.hadith;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.hadith.book.BookFragment;
import com.raiadnan.quranreader.hadith.bookmark.BookmarkFragment;
import com.raiadnan.quranreader.hadith.search.SearchActivity;
import com.raiadnan.quranreader.hadith.settings.SettingsActivity;
import com.shaheendevelopers.ads.sdk.format.BannerAd;

public class HadithActivity extends AppCompatActivity {
    BannerAd.Builder bannerAd;
    ActionBarDrawerToggle drawerToggle;
    DrawerLayout drawerLayout;

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
            Intent intent = new Intent(HadithActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        ImageView actionbarSettings = this.findViewById(R.id.actionbar_settings);
        actionbarSettings.setOnClickListener(view -> {
            Intent intent = new Intent(HadithActivity.this, SettingsActivity.class);
            startActivity(intent); //, ActivityOptions.makeCustomAnimation(this,android.R.anim.slide_in_left,android.R.anim.slide_out_right).toBundle()
        });
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