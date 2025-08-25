package com.quran.quranaudio.online.compass.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.compass.adapter.CompassAdapter;
import com.quran.quranaudio.online.compass.helper.AddressHelper;
import com.quran.quranaudio.online.compass.helper.CompassUtils;
import com.quran.quranaudio.online.compass.helper.EnhancedCompass;
import com.quran.quranaudio.online.compass.helper.LocationSave;
import com.quran.quranaudio.online.compass.view.CalibrateCompassDialog;
import com.quran.quranaudio.online.fragments.BaseFragment;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 增强版Qibla Direction Fragment
 * 特性：
 * 1. 移除Google地图依赖，专注于罗盘功能
 * 2. 使用增强型传感器数据处理
 * 3. 实时磁场检测和校准提示
 * 4. 设备倾斜补偿
 */
public class QiblaFragment extends BaseFragment implements EnhancedCompass.EnhancedCompassListener {

    private static final String TAG = "QiblaFragmentEnhanced";
    
    // 天房坐标 (麦加)
    public final double KAABA_LATITUDE = 21.42251d;
    public final double KAABA_LONGITUDE = 39.82616d;
    
    // 传感器和位置相关
    private EnhancedCompass enhancedCompass;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation;
    private CompositeDisposable compositeDisposable;
    
    // UI元素
    private View view;
    public ImageView imgCompass;
    public ImageView imgCompassK;
    private TextView tvHeading;
    private TextView tvDistance;
    private TextView tvMagneticStrength;
    private TextView tvTiltWarning;
    private LinearLayout calibrationWarning;
    private TextView btnCalibrate;
    private RecyclerView rcvCompass;
    private View calibrationIndicator;
    
    // 状态变量
    private float currentAzimuth = 0f;
    private double qiblaDirection = 0d;
    private boolean isLocationReady = false;
    
    // 校准状态相关变量
    private int compassAccuracy = android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE;
    private CalibrationStatus currentCalibrationStatus = CalibrationStatus.UNCALIBRATED;
    
    // 罗盘数据适配器
    private CompassAdapter compassAdapter;
    
    // 校准状态枚举
    public enum CalibrationStatus {
        UNCALIBRATED,   // 红色 - 未校准/需要校准
        CALIBRATING,    // 黄色 - 校准中/部分校准
        CALIBRATED      // 绿色 - 已校准/精度良好
    }
    
    @Override
    public int getLayoutId() {
        return R.layout.fragment_qibla;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(getLayoutId(), container, false);
        Log.d(TAG, "增强版Qibla Fragment创建");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "初始化增强版Qibla Direction功能");
        
        initializeUI();
        initializeLocation();
        initializeCompass();
        initializeCompassList();
        
        compositeDisposable = new CompositeDisposable();
    }

    private void initializeUI() {
        // 绑定UI元素
        imgCompass = view.findViewById(R.id.compass);
        imgCompassK = view.findViewById(R.id.compass_k);
        tvHeading = view.findViewById(R.id.tv_heading);
        tvDistance = view.findViewById(R.id.tv_distance);
        tvMagneticStrength = view.findViewById(R.id.tv_magnetic_strength);
        tvTiltWarning = view.findViewById(R.id.tv_tilt_warning);
        calibrationWarning = view.findViewById(R.id.calibration_warning);
        btnCalibrate = view.findViewById(R.id.btn_calibrate);
        rcvCompass = view.findViewById(R.id.rcv_compass);
        calibrationIndicator = view.findViewById(R.id.calibration_indicator);
        
        // 设置校准按钮点击事件
        if (btnCalibrate != null) {
            btnCalibrate.setOnClickListener(v -> showCalibrationDialog());
        }
        
        // 初始设置校准指示器为未校准状态
        updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
        
        Log.d(TAG, "UI元素初始化完成");
    }

    private void initializeLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // 创建位置请求
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10秒更新间隔
        locationRequest.setFastestInterval(5000); // 最快5秒
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        
        // 位置回调
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocation(location);
                }
            }
        };
        
        // 获取当前位置
        getCurrentLocation();
        
        Log.d(TAG, "位置服务初始化完成");
    }

    private void initializeCompass() {
        enhancedCompass = new EnhancedCompass(requireContext());
        enhancedCompass.setListener(this);
        
        Log.d(TAG, "增强型罗盘初始化完成");
        Log.d(TAG, "支持旋转向量传感器: " + enhancedCompass.isRotationVectorAvailable());
    }

    private void initializeCompassList() {
        // 简化实现 - 暂时隐藏RecyclerView直到修复CompassAdapter
        if (rcvCompass != null) {
            rcvCompass.setVisibility(View.GONE);
        }
        
        Log.d(TAG, "罗盘方向列表初始化完成（简化版）");
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            updateLocation(task.getResult());
                        } else {
                            Log.w(TAG, "无法获取当前位置");
                            // 使用保存的位置作为备用
                            loadSavedLocation();
                        }
            }
        });
    }

    private void updateLocation(Location location) {
        currentLocation = location;
        isLocationReady = true;
        
        Log.d(TAG, "位置更新: " + location.getLatitude() + ", " + location.getLongitude());
        
        // 保存位置
        LocationSave.putLocation(location.getLatitude(), location.getLongitude());
        
        // 计算Qibla方向
        calculateQiblaDirection();
        
        // 获取地址信息
        AddressHelper.getAddress(location.getLatitude(), location.getLongitude());
    }

    private void loadSavedLocation() {
        double savedLat = LocationSave.getLat();
        double savedLon = LocationSave.getLon();
        
        if (savedLat != 0.0 && savedLon != 0.0) {
            Location savedLocation = new Location("saved");
            savedLocation.setLatitude(savedLat);
            savedLocation.setLongitude(savedLon);
            updateLocation(savedLocation);
            
            Log.d(TAG, "使用保存的位置: " + savedLat + ", " + savedLon);
        }
    }

    private void calculateQiblaDirection() {
        if (currentLocation == null) {
            Log.w(TAG, "当前位置为空，无法计算Qibla方向");
            return;
        }
        
        // 使用大圆航线公式计算Qibla方向
        double lat1 = Math.toRadians(currentLocation.getLatitude());
        double lon1 = Math.toRadians(currentLocation.getLongitude());
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double lon2 = Math.toRadians(KAABA_LONGITUDE);
        
        double deltaLon = lon2 - lon1;
        
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        
        qiblaDirection = Math.toDegrees(Math.atan2(y, x));
        qiblaDirection = (qiblaDirection + 360) % 360; // 确保在0-360度范围内
        
        Log.d(TAG, "Qibla方向计算完成: " + qiblaDirection + "°");
        
        // 计算距离
        calculateDistanceToKaaba();
        
        // 更新UI
        updateQiblaUI();
    }

    private void calculateDistanceToKaaba() {
        if (currentLocation == null) return;
        
        // 使用球面余弦定理计算距离
        double R = 6371; // 地球半径 (km)
        double lat1 = Math.toRadians(currentLocation.getLatitude());
        double lon1 = Math.toRadians(currentLocation.getLongitude());
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double lon2 = Math.toRadians(KAABA_LONGITUDE);
        
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = R * c;
        
        // 更新距离显示
        if (tvDistance != null) {
            if (distance < 1.0) {
                tvDistance.setText("You are within the range of Qibla");
            } else {
                tvDistance.setText(String.format(Locale.ENGLISH, "Distance to Kaaba %.1f KM", distance));
            }
        }
        
        Log.d(TAG, "距离天房: " + distance + " km");
    }

    private void updateQiblaUI() {
        if (!isLocationReady) return;
        
        // 计算相对于当前方向的Qibla方向
        double relativeQiblaDirection = qiblaDirection - currentAzimuth;
        relativeQiblaDirection = (relativeQiblaDirection + 360) % 360;
        
        // 更新罗盘指针方向
        if (imgCompassK != null) {
            imgCompassK.setRotation((float) relativeQiblaDirection);
        }
        
        // 更新方向文本
        if (tvHeading != null) {
            String directionText = String.format(Locale.ENGLISH, "%.0f° %s", 
                qiblaDirection, CompassUtils.getDirectionString((float) qiblaDirection));
            tvHeading.setText(directionText);
        }
    }

    // EnhancedCompass.EnhancedCompassListener 实现
    @Override
    public void onAzimuthChanged(float azimuth) {
        currentAzimuth = azimuth;
        
        // 更新罗盘旋转
        if (imgCompass != null) {
            imgCompass.setRotation(-azimuth);
        }
        
        // 更新Qibla UI
        updateQiblaUI();
        
        // 更新罗盘适配器（暂时禁用）
        // if (compassAdapter != null) {
        //     compassAdapter.updateAzimuth(azimuth);
        // }
    }

    @Override
    public void onMagneticFieldChanged(float strength, EnhancedCompass.MagneticFieldStatus status) {
        if (tvMagneticStrength != null) {
            String statusText;
            int statusColor;
            
            switch (status) {
                case NORMAL:
                    statusText = "正常";
                    statusColor = getResources().getColor(R.color.colorPrimary);
                    hideMagneticWarning();
                    // 磁场正常时，根据当前传感器精度更新校准状态
                    if (compassAccuracy == android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                        updateCalibrationIndicator(CalibrationStatus.CALIBRATED);
                    }
                    break;
                case WEAK:
                    statusText = "较弱";
                    statusColor = getResources().getColor(R.color.orange);
                    showMagneticWarning("磁场信号较弱");
                    updateCalibrationIndicator(CalibrationStatus.CALIBRATING);
                    break;
                case STRONG:
                    statusText = "过强";
                    statusColor = getResources().getColor(R.color.red);
                    showMagneticWarning("磁场信号过强");
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
                case DISTURBED:
                    statusText = "干扰";
                    statusColor = getResources().getColor(R.color.red);
                    showMagneticWarning("检测到磁场干扰");
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
                default:
                    statusText = "未知";
                    statusColor = getResources().getColor(R.color.grey);
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
            }
            
            tvMagneticStrength.setText(statusText);
            tvMagneticStrength.setTextColor(statusColor);
        }
        
        Log.d(TAG, "磁场状态: " + status + " (" + strength + " μT)");
    }

    @Override
    public void onTiltChanged(float tiltAngle, boolean isDeviceLevel) {
        if (tvTiltWarning != null) {
            if (isDeviceLevel) {
                tvTiltWarning.setVisibility(View.GONE);
            } else {
                tvTiltWarning.setVisibility(View.VISIBLE);
                tvTiltWarning.setText(String.format(Locale.ENGLISH, "设备倾斜 %.1f°，请保持水平", tiltAngle));
            }
        }
        
        Log.v(TAG, "设备倾斜: " + tiltAngle + "° (水平: " + isDeviceLevel + ")");
    }

    @Override
    public void onCalibrationNeeded(String reason) {
        showMagneticWarning(reason);
        Log.w(TAG, "需要校准: " + reason);
    }

    @Override
    public void onAccuracyChanged(String sensorName, int accuracy) {
        Log.d(TAG, sensorName + " 精度变化: " + accuracy);
        
        // 更新校准指示器状态
        updateCalibrationIndicator(accuracy);
        
        // 如果传感器精度不可靠，显示校准建议
        if (accuracy == android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE) {
            showMagneticWarning("传感器精度不可靠，建议校准设备");
        }
    }

    private void showMagneticWarning(String message) {
        if (calibrationWarning != null) {
            calibrationWarning.setVisibility(View.VISIBLE);
            
            // 更新警告消息 - 直接使用LinearLayout中的TextView
            for (int i = 0; i < calibrationWarning.getChildCount(); i++) {
                View child = calibrationWarning.getChildAt(i);
                if (child instanceof TextView && child.getId() != R.id.btn_calibrate) {
                    ((TextView) child).setText("⚠️ " + message + "，请校准罗盘以获得准确方向");
                    break;
                }
            }
        }
    }

    private void hideMagneticWarning() {
        if (calibrationWarning != null) {
            calibrationWarning.setVisibility(View.GONE);
        }
    }

    /**
     * 更新校准状态指示器
     * @param accuracy 传感器精度值
     */
    private void updateCalibrationIndicator(int accuracy) {
        // 在主线程中更新UI
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                CalibrationStatus newStatus = determineCalibrationStatus(accuracy);
                
                // 只有状态变化时才更新UI
                if (newStatus != currentCalibrationStatus) {
                    currentCalibrationStatus = newStatus;
                    compassAccuracy = accuracy;
                    
                    if (calibrationIndicator != null) {
                        switch (newStatus) {
                            case UNCALIBRATED:
                                calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_red);
                                Log.d(TAG, "校准状态: 未校准 (红色)");
                                break;
                            case CALIBRATING:
                                calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_yellow);
                                Log.d(TAG, "校准状态: 校准中 (黄色)");
                                break;
                            case CALIBRATED:
                                calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_green);
                                Log.d(TAG, "校准状态: 已校准 (绿色)");
                                break;
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 基于传感器精度值的重载方法
     */
    private void updateCalibrationIndicator(CalibrationStatus status) {
        // 直接更新状态的重载方法
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                currentCalibrationStatus = status;
                
                if (calibrationIndicator != null) {
                    switch (status) {
                        case UNCALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_red);
                            break;
                        case CALIBRATING:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_yellow);
                            break;
                        case CALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_green);
                            break;
                    }
                }
            });
        }
    }
    
    /**
     * 根据传感器精度确定校准状态
     * @param accuracy 传感器精度值
     * @return 校准状态
     */
    private CalibrationStatus determineCalibrationStatus(int accuracy) {
        switch (accuracy) {
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return CalibrationStatus.CALIBRATED;
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return CalibrationStatus.CALIBRATING;
            case android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return CalibrationStatus.CALIBRATING;
            case android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE:
            default:
                return CalibrationStatus.UNCALIBRATED;
        }
    }

    private void showCalibrationDialog() {
        try {
            new CalibrateCompassDialog(requireActivity(), "为了获得最准确的Qibla方向，请校准您的设备罗盘").show();
        } catch (Exception e) {
            Log.e(TAG, "显示校准对话框出错: " + e.getMessage());
            Toast.makeText(requireContext(), "请手动校准设备罗盘", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasLocationPermission() {
        return requireActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        // 这里应该实现权限请求逻辑
        // 为简化起见，显示提示信息
        Toast.makeText(requireContext(), "需要位置权限来计算Qibla方向", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment恢复，启动传感器");
        
        if (enhancedCompass != null) {
            enhancedCompass.start();
        }
        
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Fragment暂停，停止传感器");
        
        if (enhancedCompass != null) {
            enhancedCompass.stop();
        }
        
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment销毁");
        
        if (enhancedCompass != null) {
            enhancedCompass.stop();
        }
        
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (hasLocationPermission() && mFusedLocationClient != null && locationCallback != null) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            Log.d(TAG, "开始位置更新");
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null && locationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "停止位置更新");
        }
    }
}
