package com.quran.quranaudio.online.compass.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
// import com.raiadnan.ads.sdk.format.BannerAd; // 广告导入已移除
import com.quran.quranaudio.online.activities.HomeActivity;
import com.quran.quranaudio.online.fragments.BaseFragment;
import com.quran.quranaudio.online.activities.HomeActivity;
// import com.quran.quranaudio.online.ads.data.Constant; // 广告常量导入已移除
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.compass.helper.AddressHelper;
import com.quran.quranaudio.online.compass.helper.CompassUtils;
import com.quran.quranaudio.online.compass.helper.LocationSave;
import com.quran.quranaudio.online.compass.view.CalibrateCompassDialog;
import com.quran.quranaudio.online.compass.view.MyMaker;
import com.quran.quranaudio.online.compass.adapter.CompassAdapter;
import com.quran.quranaudio.online.compass.helper.Compass;
import com.quran.quranaudio.online.compass.helper.CompassManager;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class QiblaFragment extends BaseFragment implements OnMapReadyCallback {

    private Compass compass;
    private float currentAzimuth;
    public ImageView imgCompass;
    public ImageView imgCompassK;
    private FusedLocationProviderClient mFusedLocationClient;


    public Location mLocation;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private MapView mMapView;

    public MyMaker myMaker;
    private Polyline polyline;
    private LinearLayout qiblaLayout;
    public final LatLng qiblaLocation = new LatLng(21.42251d, 39.82616d);
    private double result;
    private TextView tvDistance;
    private TextView tvHeading;
    // 广告相关变量已移除

    public int getLayoutId() {
        return R.layout.fragment_qibla;
    }

    public static QiblaFragment newInstance() {
        Bundle bundle = new Bundle();
        QiblaFragment qiblaFragment = new QiblaFragment();
        qiblaFragment.setArguments(bundle);
        return qiblaFragment;
    }

    public void onMapReady(GoogleMap googleMap) {
        Log.d("QiblaFragment", "onMapReady called with GoogleMap: " + (googleMap != null ? "valid" : "null"));
        if (googleMap != null) {
            Log.d("QiblaFragment", "GoogleMap is ready, setting up...");
            this.mMap = googleMap;
            this.myMaker = new MyMaker(this.activity);
            this.myMaker.enableSensor(this.mMap);
            this.mMap.setMyLocationEnabled(false);
            this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
            
            Log.d("QiblaFragment", "Map is ready! Initializing Qibla marker...");
            initQibla();
            
            Log.d("QiblaFragment", "Requesting location permissions...");
            ((QiblaDirectionActivity) this.activity).mRequestObject = PermissionUtil.with(this.activity).request("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION").onAllGranted(new Func() {

                @SuppressLint({"MissingPermission", "SetTextI18n"})
                public void call() {
                    Log.d("QiblaFragment", "Location permissions granted, getting device location...");
                    QiblaFragment.this.getDeviceLocation(false);
                    QiblaFragment.this.listenLocationChange();
                }
            }).ask(12);
        } else {
            Log.e("QiblaFragment", "GoogleMap is null in onMapReady!");
        }
    }

    private void initQibla() {
        this.mMap.addMarker(new MarkerOptions().position(this.qiblaLocation)
                .anchor(0.5f, 0.5f).icon(CompassUtils.getBitmapFromVectorDrawable(this.activity, R.drawable.kaaba)));
    }


    public void zoomPoints(LatLng latLng, LatLng latLng2) {
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(latLng);
            builder.include(latLng2);
            LatLngBounds build = builder.build();
            int i = getResources().getDisplayMetrics().widthPixels;
            int i2 = getResources().getDisplayMetrics().heightPixels;
            double min = (double) Math.min(i, i2);
            Double.isNaN(min);
            this.mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(build, i, i2, (int) (min * 0.4d)));
        } catch (Exception e) {
            e.getMessage();
        }
    }


    public void addPolyline(LatLng latLng, LatLng latLng2) {
        Polyline polyline2 = this.polyline;
        if (polyline2 != null) {
            polyline2.remove();
        }
        this.polyline = this.mMap.addPolyline(new PolylineOptions().geodesic(true).color(this.activity.getResources().getColor(R.color.colorPrimary)).width(7.0f).add(latLng).add(latLng2).pattern(Arrays.asList(new Dash(10.0f), new Gap(10.0f))));
    }

    private void initMapView(Bundle bundle) {
        this.mMapView = (MapView) this.view.findViewById(R.id.map);
        if (this.mMapView != null) {
            Log.d("QiblaFragment", "MapView found, initializing with robust strategy...");
            try {
                // 健壮的地图初始化策略
                this.mMapView.onCreate(bundle);
                
                // 延迟500ms再加载地图，确保所有组件都准备好
                this.mMapView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d("QiblaFragment", "Starting delayed map loading...");
                            if (mMapView != null && isAdded() && !isDetached()) {
                                mMapView.getMapAsync(QiblaFragment.this);
                            } else {
                                Log.w("QiblaFragment", "Fragment not ready for map loading");
                            }
                        } catch (Exception e) {
                            Log.e("QiblaFragment", "Error in delayed map loading: " + e.getMessage());
                        }
                    }
                }, 500); // 延迟500ms
                
                Log.d("QiblaFragment", "MapView delayed initialization scheduled");
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error initializing MapView: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.e("QiblaFragment", "MapView is null!");
        }
    }

    @SuppressLint({"SetTextI18n"})
    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initLocation();
        initView();
        // 异步初始化地图，不阻塞其他组件
        initMapView(bundle);
        // 正常初始化指南针，与地图加载并行
        setupCompass();
        initBottomCompass();

        // 广告代码已移除
    }

    // loadBannerAd方法已移除

    private void initLocation() {
        this.mLocation = new Location("dummyprovider");
        this.mLocation.setLatitude(LocationSave.getLat());
        this.mLocation.setLongitude(LocationSave.getLon());
    }

    private void setupCompass() {
        this.compass = new Compass(this.activity);
        this.compass.setListener(new Compass.CompassListener() {
            public void onNewAzimuth(float f) {
                QiblaFragment.this.adjustGambarDial(f);
                QiblaFragment.this.adjustArrowQiblat(f);
            }

            public void onAccuracyChanged(String str) {
                // 正常显示校准对话框，与地图加载并行进行
                Log.d("QiblaFragment", "Compass accuracy changed: " + str + ", showing calibration dialog");
                try {
                    new CalibrateCompassDialog(QiblaFragment.this.activity, str).show();
                } catch (Exception e) {
                    Log.e("QiblaFragment", "Error showing calibration dialog: " + e.getMessage());
                }
            }
        });
    }

    public void adjustGambarDial(float f) {
        RotateAnimation rotateAnimation = new RotateAnimation(-this.currentAzimuth, -f, 1, 0.5f, 1, 0.5f);
        this.currentAzimuth = f;
        rotateAnimation.setDuration(500);
        rotateAnimation.setRepeatCount(0);
        rotateAnimation.setFillAfter(true);
        this.imgCompass.startAnimation(rotateAnimation);
    }

    public void adjustArrowQiblat(float f) {
        double d = (double) (-this.currentAzimuth);
        double d2 = this.result;
        Double.isNaN(d);
        RotateAnimation rotateAnimation = new RotateAnimation((float) (d + d2), -f, 1, 0.5f, 1, 0.5f);
        this.currentAzimuth = f;
        rotateAnimation.setDuration(500);
        rotateAnimation.setRepeatCount(0);
        rotateAnimation.setFillAfter(true);
        this.imgCompassK.startAnimation(rotateAnimation);
        if (this.result > 0.0d) {
            this.imgCompassK.setVisibility(View.VISIBLE);
            return;
        }
        this.imgCompassK.setVisibility(View.INVISIBLE);
        this.imgCompassK.setVisibility(View.GONE);
    }

    private void initBottomCompass() {
        this.imgCompass.setImageResource(CompassManager.getCompass());
        this.imgCompassK.setImageResource(CompassManager.getCompassK());
        RecyclerView recyclerView = (RecyclerView) this.view.findViewById(R.id.rcv_compass);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity, RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(new CompassAdapter(this.activity) {
            public void OnItemClick(int i, int i2) {
                CompassManager.setCompass(i);
                CompassManager.setCompassK(i2);
                QiblaFragment.this.imgCompass.setImageResource(i);
                QiblaFragment.this.imgCompassK.setImageResource(i2);
            }
        });
    }

    private void initView() {
        this.imgCompass = (ImageView) this.view.findViewById(R.id.compass);
        this.imgCompassK = (ImageView) this.view.findViewById(R.id.compass_k);
        this.tvHeading = (TextView) this.view.findViewById(R.id.tv_heading);
        this.tvDistance = (TextView) this.view.findViewById(R.id.tv_distance);
        this.qiblaLayout = (LinearLayout) this.view.findViewById(R.id.qibla_layout);
    }

    public void myLocationClick(final Boolean bool) {
        ((QiblaDirectionActivity) this.activity).mRequestObject = PermissionUtil.with(this.activity).request("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION").onAllGranted(new Func() {
            /* access modifiers changed from: protected */
            @SuppressLint({"MissingPermission", "SetTextI18n"})
            public void call() {
                QiblaFragment.this.getDeviceLocation(bool);
            }
        }).ask(12);
    }

    @SuppressLint("MissingPermission")
    public void getDeviceLocation(final Boolean bool) {
        try {
            LocationServices.getFusedLocationProviderClient((Activity)
                    this.activity).getLastLocation().addOnCompleteListener(this.activity, new OnCompleteListener() {

                public final void onComplete(Task task) {

                    if (task.isSuccessful() && task.getResult() != null && LocationSave.isAutoUpdate()) {
                        QiblaFragment.this.mLocation = (Location) task.getResult();
                        LocationSave.putLocation(mLocation.getLatitude(), mLocation.getLongitude());
                        LocationSave.setTimeZone(String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60));
                        AddressHelper.getAddress(mLocation.getLatitude(), mLocation.getLongitude());
                    }
                    fetchGps();
                    myMaker.updateLocation(mLocation);
                    addPolyline(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()),qiblaLocation);
                    if (bool.booleanValue()) {
                       mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(),
                               mLocation.getLongitude()), 16.0f));
                    } else {
                        zoomPoints(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()),
                                qiblaLocation);
                    }
                    view.animate().alpha(0.0f).setDuration(300).withEndAction(new Runnable() {
                        public final void run() {


                            if (bool.booleanValue()) {
                                qiblaLayout.setWeightSum(4.0f);
                                qiblaLayout.requestLayout();
                                initBottomCompass();
                            } else {
                                qiblaLayout.setWeightSum(10.0f);
                                qiblaLayout.requestLayout();
                                initBottomCompass();
                            }
                            view.animate().alpha(1.0f).setDuration(300).start();



                        }
                    }).start();








                }
            });
        } catch (Exception e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }




    @SuppressLint("MissingPermission")
    public void listenLocationChange() {
        this.mLocationRequest = new LocationRequest();
        this.mLocationRequest.setInterval(120000);
        this.mLocationRequest.setFastestInterval(120000);
        this.mLocationRequest.setPriority(102);
        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient((Activity) this.activity);
        this.mFusedLocationClient.requestLocationUpdates(this.mLocationRequest, new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLastLocation() != null && LocationSave.isAutoUpdate()) {
                    Location unused = QiblaFragment.this.mLocation = locationResult.getLastLocation();
                    AddressHelper.getAddress(QiblaFragment.this.mLocation.getLatitude(), QiblaFragment.this.mLocation.getLongitude());
                }
                LocationSave.putLocation(QiblaFragment.this.mLocation.getLatitude(), QiblaFragment.this.mLocation.getLongitude());
                LocationSave.setTimeZone(String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60));
                LatLng latLng = new LatLng(QiblaFragment.this.mLocation.getLatitude(), QiblaFragment.this.mLocation.getLongitude());
                QiblaFragment.this.fetchGps();
                QiblaFragment.this.myMaker.updateLocation(QiblaFragment.this.mLocation);
                QiblaFragment qiblaFragment = QiblaFragment.this;
                qiblaFragment.zoomPoints(latLng, qiblaFragment.qiblaLocation);
                QiblaFragment qiblaFragment2 = QiblaFragment.this;
                qiblaFragment2.addPolyline(new LatLng(qiblaFragment2.mLocation.getLatitude(), QiblaFragment.this.mLocation.getLongitude()), QiblaFragment.this.qiblaLocation);
            }
        }, Looper.myLooper());
    }

    public void updateLocation() {
        this.mLocation.setLatitude(LocationSave.getLat());
        this.mLocation.setLongitude(LocationSave.getLon());
        fetchGps();
        this.myMaker.updateLocation(this.mLocation);
        addPolyline(new LatLng(this.mLocation.getLatitude(), this.mLocation.getLongitude()), this.qiblaLocation);
    }

    
    @SuppressLint({"SetTextI18n"})
    public void fetchGps() {
        double radians = Math.toRadians(21.42251d);
        double radians2 = Math.toRadians(this.mLocation.getLatitude());
        double radians3 = Math.toRadians(39.82616d - this.mLocation.getLongitude());
        this.result = (Math.toDegrees(Math.atan2(Math.sin(radians3) * Math.cos(radians), (Math.cos(radians2) * Math.sin(radians)) - ((Math.sin(radians2) * Math.cos(radians)) * Math.cos(radians3)))) + 360.0d) % 360.0d;
        this.tvHeading.setText(String.format(Locale.ENGLISH, "%.0f", Float.valueOf((float) this.result)) + "°" + CompassUtils.getDirectionString((float) this.result));
        double atan2 = Math.atan2(1.0d, 1.0d) * 4.0d;
        double d = atan2 / 180.0d;
        double d2 = 180.0d / atan2;
        double latitude = this.mLocation.getLatitude() * d;
        double d3 = 21.42251d * d;
        double acos = Math.acos(Math.cos(latitude - d3) - (((1.0d - Math.cos((this.mLocation.getLongitude() * d) - (d * 39.82616d))) * Math.cos(latitude)) * Math.cos(d3))) * 60.0d * 1.852d * d2;
        double d4 = acos * 1000.0d;
        String[] split = String.valueOf(acos).split("\\.");
        String str = split[0] + "." + split[1].substring(0, 2);
        if (d4 < 1000.0d) {
            this.tvDistance.setText("You are within the range of Qibla");
            return;
        }
        this.tvDistance.setText("Distance to Kaaba " + str + " KM");
    }

    public void onResume() {
        super.onResume();
        // 安全地恢复MapView
        if (this.mMapView != null) {
            try {
                this.mMapView.onResume();
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error resuming MapView: " + e.getMessage());
            }
        }
        // 启动指南针传感器
        Compass compass2 = this.compass;
        if (compass2 != null) {
            Log.d("QiblaFragment", "Starting compass in onResume");
            try {
                compass2.start(this.activity);
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error starting compass: " + e.getMessage());
            }
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mMapView != null) {
            try {
                this.mMapView.onPause();
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error pausing MapView: " + e.getMessage());
            }
        }
        Compass compass2 = this.compass;
        if (compass2 != null) {
            compass2.stop();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mMapView != null) {
            try {
                this.mMapView.onDestroy();
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error destroying MapView: " + e.getMessage());
            }
        }
        MyMaker myMaker2 = this.myMaker;
        if (myMaker2 != null) {
            myMaker2.removeSensor();
        }
    }

    public void onLowMemory() {
        super.onLowMemory();
        if (this.mMapView != null) {
            try {
                this.mMapView.onLowMemory();
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error handling low memory for MapView: " + e.getMessage());
            }
        }
    }

    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mMapView != null) {
            try {
                this.mMapView.onSaveInstanceState(bundle);
            } catch (Exception e) {
                Log.e("QiblaFragment", "Error saving MapView state: " + e.getMessage());
            }
        }
    }
}
