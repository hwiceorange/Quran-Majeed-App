package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.ui.BaseActivity;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class TimingTableActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timing_table);
        TimingTablePagerAdapter timingTableIndexPagerAdapter = new TimingTablePagerAdapter(getSupportFragmentManager(), getLifecycle());
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(timingTableIndexPagerAdapter);

        ImageView imgFavorite = (ImageView) findViewById(R.id.back);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimingTableActivity.this.finish();
            }
        });

        String[] titles = {StringUtils.capitalize(UiUtils.formatShortDate(LocalDate.now())),
                StringUtils.capitalize(UiUtils.formatShortDate(LocalDate.now().plusMonths(1)))};

        TabLayout tabLayout = findViewById(R.id.tabs);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(titles[position])
        ).attach();

        SharedPreferences sharedPreferences = getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);

        String lastKnownLocality = sharedPreferences.getString(PreferencesConstants.LAST_KNOWN_LOCALITY, "");
        String toolBarTitle;

        if (!lastKnownLocality.isEmpty()) {
            toolBarTitle = getString(R.string.calendar_view_title) + " " + sharedPreferences.getString(PreferencesConstants.LAST_KNOWN_LOCALITY, "");
        } else {
            toolBarTitle = getString(R.string.title_calendar);
        }

        ((TextView) findViewById(R.id.timing_table_toolbar_title)).setText(toolBarTitle);

    }
}