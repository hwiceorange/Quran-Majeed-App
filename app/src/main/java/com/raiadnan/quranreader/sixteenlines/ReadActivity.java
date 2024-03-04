package com.raiadnan.quranreader.sixteenlines;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.shaheendevelopers.ads.sdk.format.BannerAd;

import java.util.ArrayList;

public class ReadActivity extends AppCompatActivity {
    BannerAd.Builder bannerAd;

    public ArrayList<Integer> images;
    private BitmapFactory.Options options;
    private ViewPager viewPager;
    private View btnNext, btnPrev;
    private FragmentStatePagerAdapter adapter;
    //private LinearLayout thumbnailsContainer;
    private int[] resourceIDs;

    public MyApplication2 app2 =new MyApplication2();


    private androidx.appcompat.widget.ActionMenuView amvMenu;
    private float x1,x2;
    static int ITEM;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        ImageView imgFavorite = (ImageView) findViewById(R.id.back_btn);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReadActivity.this.finish();
            }
        });




        amvMenu = (androidx.appcompat.widget.ActionMenuView) toolbar.findViewById(R.id.amvMenu);
        amvMenu.setOnMenuItemClickListener((androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener) menuItem -> onOptionsItemSelected(menuItem));

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        //toolbar.setNavigationIcon(R.drawable.ic_chevron_left_black_24dp);
        images = new ArrayList<>();

        idCatcher();



        //find view by id
        viewPager = (ViewPager) findViewById(R.id.view_pager);

        setImagesData();


        // init viewpager adapter and attach
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(ITEM);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // use amvMenu here
        inflater.inflate(R.menu.menu_main, amvMenu.getMenu());
        return true;
    }

    ArrayList<Integer> checks;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        if (id == R.id.action_night_mode) {

            app2.setNightmode(!app2.getNightmode());

            //nightMode =!nightMode;
            ITEM =viewPager.getCurrentItem();
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(ITEM);
            if(app2.getNightmode()){
                Toast.makeText(this, "Night mode On", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Night mode Off", Toast.LENGTH_SHORT).show();
            }

        }
        if(id==R.id.action_bookmark){

            Boolean actionUse=true;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            SharedPreferences.Editor editor = settings.edit();

            for(int index =1; index<=settings.getInt("bookmark_no",0);index++){

                if(settings.getInt("bookmark_"+index,0)==viewPager.getCurrentItem()){
                    actionUse =false;
                }
            }

            if(actionUse) {
                editor.putInt("bookmark_no", settings.getInt("bookmark_no", 0) + 1);
                editor.apply();

                editor.putInt("bookmark_" + settings.getInt("bookmark_no", 0), viewPager.getCurrentItem());
                editor.apply();

                Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Already bookmarked", Toast.LENGTH_SHORT).show();
            }

        }if(id==R.id.highlight_mode){


            app2.setHighlight(!app2.getHighlight());
            //highlight=!highlight;
            ITEM =viewPager.getCurrentItem();
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(ITEM);
            if(app2.getHighlight()){
                Toast.makeText(this, "Highlight On", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Highlight Off", Toast.LENGTH_SHORT).show();
            }
        }



        return super.onOptionsItemSelected(item);
    }

/*
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        MenuItem item = menu.findItem(R.id.action_bookmark);

        if (myItemShouldBeEnabled) {
            item.setEnabled(true);
            item.getIcon().setAlpha(255);
        } else {
            // disabled
            item.setEnabled(false);
            item.getIcon().setAlpha(130);
        }
    }
*/



    private View.OnClickListener onClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i > 0) {
                    //next page
                    if (viewPager.getCurrentItem() < viewPager.getAdapter().getCount() - 1) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                    }
                } else {
                    //previous page
                    if (viewPager.getCurrentItem() > 0) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                    }
                }
            }
        };
    }

    private void setImagesData() {
        for (int i = 0; i < resourceIDs.length; i++) {
            images.add(resourceIDs[i]);
        }
    }


    private View.OnClickListener onChagePageClickListener(final int i) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(i);
            }
        };
    }

    void idCatcher(){

        resourceIDs = new int[550];

        for(int i=0;i<550; i++){

            int index=550-i;
            resourceIDs[i] = getResources().getIdentifier("page_" + index, "raw", getPackageName());

        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Page_No", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("resume",viewPager.getCurrentItem());
        editor.apply();

    }
}

