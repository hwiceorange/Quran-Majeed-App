package com.quran.quranaudio.online.quran_module.frags.main;

import static com.quran.quranaudio.online.prayertimes.notifier.PrayerAlarmScheduler.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;

// 广告导入已移除
// import com.raiadnan.ads.sdk.format.BannerAd;
// import com.raiadnan.ads.sdk.format.InterstitialAd;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.activities.SixKalmasActivity;
import com.quran.quranaudio.online.activities.ZakatCalculatorActivity;
// import com.quran.quranaudio.online.ads.data.Constant; // 广告常量导入已移除
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.hadith.HadithActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.utils.app.UpdateManager;
import com.quran.quranaudio.online.quran_module.views.VOTDView;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.databinding.FragMainBinding;
import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;
import com.quran.quranaudio.online.quran_module.frags.BaseFragment;
import com.quran.quranaudio.online.quran_module.utils.app.UpdateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

public class FragMain extends BaseFragment {
    private FragMainBinding mBinding;
    private AsyncLayoutInflater mAsyncInflater;
    private VOTDView mVotdView;
    private UpdateManager mUpdateManager;

    // 广告相关变量已移除


    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;


    public FragMain() {
    }

    @Override
    public boolean networkReceiverRegistrable() {
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();


        Context context = getContext();
        if (context == null) {
            return;
        }

        QuranMeta.prepareInstance(context, quranMeta -> {
            if (mVotdView != null) {
                mVotdView.post(() -> mVotdView.refresh(quranMeta));
            }

        });
    }

    @Override
    public void onDestroy() {
        if (mVotdView != null) {
            mVotdView.destroy();
        }


        super.onDestroy();
    }

    public static FragMain newInstance() {
        return new FragMain();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAsyncInflater = new AsyncLayoutInflater(inflater.getContext());

        mBinding = FragMainBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mBinding == null) {
            mBinding = FragMainBinding.bind(view);
        }

        //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        if(!checkLocationPermission()){
            showPermissionWarning();
        }
        //requestPermission();
        //Permission End


        mUpdateManager = new UpdateManager(view.getContext(), mBinding.appUpdateContainer);
        // If update is not critical, proceed to load the rest of the content
        if (!mUpdateManager.check4Update()) {
            QuranMeta.prepareInstance(view.getContext(), quranMeta -> initContent(view, quranMeta));
        }

        mBinding.readQuran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ActivityReaderIndexPage.class));
                // 广告调用已移除
            }
        });
        mBinding.hadithBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), HadithActivity.class));
                // 广告调用已移除
            }
        });
        mBinding.qiblaDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), QiblaDirectionActivity.class));
                // 广告调用已移除
            }
        });
        mBinding.prayerCalender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), CalendarActivity.class));
                // 广告调用已移除
            }
        });
        mBinding.sixKalmas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SixKalmasActivity.class));
                // 广告调用已移除
            }
        });
        mBinding.zakatCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ZakatCalculatorActivity.class));
                // 广告调用已移除
            }
        });
        mBinding.meccaLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 备用直播URL列表
                String[] meccaLiveUrls = {
                    "http://m.live.net.sa:1935/live/quran/playlist.m3u8", // 原始URL
                    "https://ythls.armelin.one/channel/UCos52-JmjOoBnBOnxJCWAQA.m3u8", // Mecca Live YouTube转HLS
                    "https://www.youtube.com/watch?v=e85tJVzKwDU", // YouTube backup 1
                    "https://www.youtube.com/watch?v=yd19lGSibQ4"  // YouTube backup 2
                };
                
                String selectedUrl = meccaLiveUrls[0]; // 默认使用第一个
                Log.d("FragMain", "Mecca Live URL: " + selectedUrl);
                Log.d("FragMain", "Available backup URLs: " + java.util.Arrays.toString(meccaLiveUrls));
                
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", selectedUrl);
                intent.putExtra("backup_urls", meccaLiveUrls);
                startActivity(intent);
            }
        });
        mBinding.medinaLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 备用直播URL列表
                String[] medinaLiveUrls = {
                    "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8", // 原始URL
                    "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8", // Medina Live YouTube转HLS
                    "https://www.youtube.com/watch?v=4Ar8JHRCdSE" // YouTube backup
                };
                
                String selectedUrl = medinaLiveUrls[0]; // 默认使用第一个
                Log.d("FragMain", "Medina Live URL: " + selectedUrl);
                Log.d("FragMain", "Available backup URLs: " + java.util.Arrays.toString(medinaLiveUrls));
                
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", selectedUrl);
                intent.putExtra("backup_urls", medinaLiveUrls);
                startActivity(intent);
            }
        });

        // 广告代码已移除


    }

    // 广告方法已全部移除

    View.OnClickListener dialogListener=new View.OnClickListener() {
        @Override public void onClick(View v) {
            if(v.getId()==R.id.btn_skip){
                dialogWarning.dismiss();
            } else if(v.getId()==R.id.btn_enable_location){
                dialogWarning.dismiss();
                requestPermission();
            }
        }
    };
    Dialog dialogWarning;
    private boolean checkLocationPermission(){
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        return isLocationPermissionGranted;
    }
    private void showPermissionWarning(){
        View view=LayoutInflater.from(getActivity()).inflate(R.layout.layout_dialog_location_warning,null);
        dialogWarning=new AlertDialog.Builder(getActivity()).setView(view).create();
        TextView skip=view.findViewById(R.id.btn_skip);
        Button enable=view.findViewById(R.id.btn_enable_location);
        skip.setOnClickListener(dialogListener);
        enable.setOnClickListener(dialogListener);
        dialogWarning.setCanceledOnTouchOutside(false);
        dialogWarning.show();
    }
    private void requestPermission(){


        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {
            try {
                mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private void initContent(View root, QuranMeta quranMeta) {
        initVOTD(root, quranMeta);
        //        initReadHistory(root, quranMeta);
    }

    private void initVOTD(View root, QuranMeta quranMeta) {
        mVotdView = new VOTDView(root.getContext());
        mVotdView.setId(R.id.homepageVOTD);
        mBinding.container.addView(mVotdView, resolvePosReadHistory(root));
        mVotdView.refresh(quranMeta);
    }


    private void initFeaturedReading(View root, QuranMeta quranMeta) {

    }



    public static int resolvePosUpdateCont() {
        return 0;
    }

    private int resolvePosVOTD(View root) {
        int pos = resolvePosUpdateCont() + 1;
        if (root.findViewById(R.id.appUpdateContainer) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosReadHistory(View root) {
        int pos = resolvePosVOTD(root) + 1;
        if (root.findViewById(R.id.homepageVOTD) == null) {
            pos--;
        }
        return pos;
    }


}
