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
 * Enhanced Qibla Direction Fragment
 * Features:
 * 1. Removed Google Maps dependency, focused on compass functionality
 * 2. Enhanced sensor data processing
 * 3. Real-time magnetic field detection and calibration prompts
 * 4. Device tilt compensation
 */
@SuppressWarnings("deprecation")
public class QiblaFragment extends BaseFragment implements EnhancedCompass.EnhancedCompassListener {

    private static final String TAG = "QiblaFragmentEnhanced";
    
    // Kaaba coordinates (Mecca)
    public final double KAABA_LATITUDE = 21.42251d;
    public final double KAABA_LONGITUDE = 39.82616d;
    
    // Sensor and location related
    private EnhancedCompass enhancedCompass;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation;
    private CompositeDisposable compositeDisposable;
    
    // UI elements
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
    private TextView tvCalibrationStatus;
    
    // State variables
    private float currentAzimuth = 0f;
    // 磁偏角（磁北与真北的夹角）。传感器方位角基于磁北，Qibla 方位角基于真北，
    // 不校正会导致罗盘指针系统性偏差（磁偏角大的地区可达十几度）。
    private float declination = 0f;
    private double qiblaDirection = 0d;
    private boolean isLocationReady = false;
    private android.os.Handler updateHandler;
    // 对齐状态：仅在"未对齐→对齐"跳变时震动一次，避免持续对齐时反复震动
    private boolean wasAligned = false;
    // 指针平滑与节流：低端机上每个传感器事件全量重绘会掉帧。
    // 用圆周低通滤波去抖，且方位变化 < RENDER_THRESHOLD 时跳过重绘。
    private float smoothedAzimuth = Float.NaN;
    private float lastRenderedAzimuth = Float.NaN;
    private static final float SMOOTHING = 0.15f;      // 低通系数（越小越平滑）
    private static final float RENDER_THRESHOLD = 1.0f; // 小于此角度变化不重绘
    private Runnable updateRunnable;
    
    // Calibration status related variables
    private int compassAccuracy = android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE;
    private CalibrationStatus currentCalibrationStatus = CalibrationStatus.UNCALIBRATED;
    
    // Compass data adapter
    private CompassAdapter compassAdapter;
    
    // Calibration status enum
    public enum CalibrationStatus {
        UNCALIBRATED,   // Red - Uncalibrated/needs calibration
        CALIBRATING,    // Yellow - Calibrating/partial calibration
        CALIBRATED      // Green - Calibrated/good accuracy
    }
    
    @Override
    public int getLayoutId() {
        return R.layout.fragment_qibla;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(getLayoutId(), container, false);
        Log.d(TAG, "Enhanced Qibla Fragment created");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "Initialize enhanced Qibla Direction functionality");
        
        initializeUI();
        initializeLocation();
        initializeCompass();
        initializeCompassList();
        
        compositeDisposable = new CompositeDisposable();
        
        // Initialize update handler for real-time data updates
        updateHandler = new android.os.Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLocationReady && currentLocation != null) {
                    // Force UI update every 1 second
                    updateQiblaUI();
                    double distance = calculateDistanceToKaaba();
                    Log.d(TAG, "Periodic UI update - Qibla: " + qiblaDirection + "°, Distance: " + distance + " km");
                }
                updateHandler.postDelayed(this, 1000); // Update every 1 second
            }
        };
    }

    private void initializeUI() {
        // Bind UI elements
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
        tvCalibrationStatus = view.findViewById(R.id.tv_calibration_status);
        
        // Set calibration button click event
        if (btnCalibrate != null) {
            btnCalibrate.setOnClickListener(v -> showCalibrationDialog());
        }
        
        // Add refresh location button for testing
        if (tvDistance != null) {
            tvDistance.setOnClickListener(v -> {
                Log.d(TAG, "Manual location refresh triggered");
                getCurrentLocation();
            });
        }
        
        // Add test location change button for testing real-time updates
        if (tvHeading != null) {
            tvHeading.setOnClickListener(v -> {
                Log.d(TAG, "Test location change triggered");
                simulateLocationChange();
            });
        }
        
        // Initially set calibration indicator to uncalibrated state
        updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
        
        Log.d(TAG, "UI elements initialization completed");
    }

    private void initializeLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Create location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000); // 2 second update interval for more responsive updates
        locationRequest.setFastestInterval(500); // Fastest 0.5 second
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        
        // Location callback
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
        
        // Get current location
        getCurrentLocation();
        
        Log.d(TAG, "Location service initialization completed");
    }

    private void initializeCompass() {
        enhancedCompass = new EnhancedCompass(requireContext());
        enhancedCompass.setListener(this);
        
        Log.d(TAG, "Enhanced compass initialization completed");
        Log.d(TAG, "Rotation vector sensor supported: " + enhancedCompass.isRotationVectorAvailable());
    }

    private void initializeCompassList() {
        if (rcvCompass != null) {
            rcvCompass.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            
            compassAdapter = new CompassAdapter(requireContext()) {
                @Override
                public void OnItemClick(int compassIndex, int compassKIndex) {
                    // Update compass skin
                    if (imgCompass != null) {
                        imgCompass.setImageResource(compassIndex);
                    }
                    if (imgCompassK != null) {
                        imgCompassK.setImageResource(compassKIndex);
                    }
                    Log.d(TAG, "Compass skin changed: " + compassIndex);
                }
            };
            
            rcvCompass.setAdapter(compassAdapter);
            rcvCompass.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Compass skin selector initialized");
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
                            Log.w(TAG, "Unable to get current location");
                            // Use saved location as backup
                            loadSavedLocation();
                        }
            }
        });
    }

    private void updateLocation(Location location) {
        currentLocation = location;
        isLocationReady = true;
        
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude() + ", isLocationReady: " + isLocationReady);
        
        // Save location
        LocationSave.putLocation(location.getLatitude(), location.getLongitude());

        // 根据经纬度/海拔/当前时间计算磁偏角，用于把磁北方位角转为真北
        try {
            android.hardware.GeomagneticField geoField = new android.hardware.GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    System.currentTimeMillis());
            declination = geoField.getDeclination();
            Log.d(TAG, "Magnetic declination at location: " + declination + "°");
        } catch (Exception e) {
            Log.w(TAG, "Failed to compute magnetic declination, using 0", e);
            declination = 0f;
        }

        // Calculate Qibla direction
        calculateQiblaDirection();
        
        // Force immediate UI update
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                updateQiblaUI();
            });
        }
        
        // Get address information
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
            
            Log.d(TAG, "Using saved location: " + savedLat + ", " + savedLon);
        }
    }
    
    private void simulateLocationChange() {
        if (currentLocation == null) {
            Log.w(TAG, "No current location to simulate change");
            return;
        }
        
        // Simulate moving 1 degree in latitude and longitude
        Location newLocation = new Location("simulated");
        newLocation.setLatitude(currentLocation.getLatitude() + 1.0);
        newLocation.setLongitude(currentLocation.getLongitude() + 1.0);
        
        Log.d(TAG, "Simulating location change from: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + 
              " to: " + newLocation.getLatitude() + ", " + newLocation.getLongitude());
        
        updateLocation(newLocation);
    }

    private void calculateQiblaDirection() {
        if (currentLocation == null) {
            Log.w(TAG, "Current location is null, unable to calculate Qibla direction");
            return;
        }
        
        Log.d(TAG, "Calculating Qibla direction for location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        
        // Calculate Qibla direction using great circle formula
        double lat1 = Math.toRadians(currentLocation.getLatitude());
        double lon1 = Math.toRadians(currentLocation.getLongitude());
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double lon2 = Math.toRadians(KAABA_LONGITUDE);
        
        double deltaLon = lon2 - lon1;
        
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        
        double newQiblaDirection = Math.toDegrees(Math.atan2(y, x));
        newQiblaDirection = (newQiblaDirection + 360) % 360; // Ensure within 0-360 degree range
        
        // Check if direction actually changed
        if (Math.abs(newQiblaDirection - qiblaDirection) > 0.1) {
            Log.d(TAG, "Qibla direction changed from " + qiblaDirection + "° to " + newQiblaDirection + "°");
            qiblaDirection = newQiblaDirection;
        } else {
            Log.d(TAG, "Qibla direction unchanged: " + qiblaDirection + "°");
        }
        
        // Calculate distance
        calculateDistanceToKaaba();
        
        // Update UI
        updateQiblaUI();
    }

    private double calculateDistanceToKaaba() {
        if (currentLocation == null) return 0.0;
        
        // Calculate distance using spherical law of cosines
        double R = 6371; // Earth radius (km)
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
        
        return distance;
    }
    
    private void updateDistanceDisplay() {
        Log.d(TAG, "updateDistanceDisplay called - currentLocation: " + (currentLocation != null));
        
        if (currentLocation == null) {
            Log.w(TAG, "Current location is null, skipping distance update");
            return;
        }
        
        double distance = calculateDistanceToKaaba();
        
        // Update distance display - show only values and units
        if (tvDistance != null) {
            String distanceText;
            if (distance < 1.0) {
                distanceText = "< 1 km";
            } else {
                distanceText = String.format(Locale.ENGLISH, "%.1f kms", distance);
            }
            tvDistance.setText(distanceText);
            Log.d(TAG, "Updated distance text to: " + distanceText);
        }
        
        Log.d(TAG, "Distance to Kaaba: " + distance + " km");
    }

    private void updateQiblaUI() {
        Log.d(TAG, "updateQiblaUI called - isLocationReady: " + isLocationReady + ", currentLocation: " + (currentLocation != null));
        
        if (!isLocationReady) {
            Log.w(TAG, "Location not ready, skipping UI update");
            return;
        }
        
        // Calculate Qibla direction relative to current direction
        double relativeQiblaDirection = qiblaDirection - currentAzimuth;
        relativeQiblaDirection = (relativeQiblaDirection + 360) % 360;
        
        // Update compass pointer direction
        if (imgCompassK != null) {
            imgCompassK.setRotation((float) relativeQiblaDirection);
        }

        // 对齐确认：指针指向麦加（容差 ±5°）时给一次震动 + 视觉反馈，
        // 这是用户找到朝向的情感落点，避免用户盯着晃动的指针不确定"到底对没对上"。
        boolean aligned = relativeQiblaDirection <= 5 || relativeQiblaDirection >= 355;
        onQiblaAlignmentChanged(aligned);

        // Update direction text - show absolute Qibla direction from North
        if (tvHeading != null) {
            tvHeading.setText(String.format(Locale.ENGLISH, "%.1f°", qiblaDirection));
            Log.d(TAG, "Updated heading text to: " + String.format(Locale.ENGLISH, "%.1f°", qiblaDirection));
        }
        
        // Update distance display
        updateDistanceDisplay();
        
        Log.d(TAG, "Qibla UI updated - Direction: " + qiblaDirection + "°, Azimuth: " + currentAzimuth + "°, Relative: " + relativeQiblaDirection + "°");
    }

    /**
     * 对齐状态变化处理：对准麦加时给指针染绿 + 一次性震动确认。
     * 只在跳变边沿触发震动，持续对齐不重复震动。
     */
    private void onQiblaAlignmentChanged(boolean aligned) {
        if (aligned == wasAligned) {
            return;
        }
        wasAligned = aligned;

        // 指针高亮：对齐变绿，未对齐清除着色
        if (imgCompassK != null) {
            if (aligned) {
                imgCompassK.setColorFilter(0xFF2E9E7A, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                imgCompassK.clearColorFilter();
            }
        }

        if (aligned) {
            try {
                android.content.Context ctx = getContext();
                if (ctx != null && isAlignmentHapticAllowed(ctx)) {
                    android.os.Vibrator vibrator =
                            (android.os.Vibrator) ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                                60, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Vibration on alignment failed", e);
            }
        }
    }

    /**
     * 对齐震动是否允许：跟随系统响铃模式，静音（如清真寺内）时不震动，
     * 避免在礼拜静默场景造成尴尬。响铃/震动模式下正常触觉反馈。
     */
    private boolean isAlignmentHapticAllowed(android.content.Context ctx) {
        try {
            android.media.AudioManager am =
                    (android.media.AudioManager) ctx.getSystemService(android.content.Context.AUDIO_SERVICE);
            if (am != null && am.getRingerMode() == android.media.AudioManager.RINGER_MODE_SILENT) {
                return false;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    // Location update method
    public void onLocationUpdate(Location location) {
        if (location != null) {
            currentLocation = location;
            isLocationReady = true;
            Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
            
            // Recalculate Qibla direction
            calculateQiblaDirection();
        }
    }

    // EnhancedCompass.EnhancedCompassListener implementation
    @Override
    public void onAzimuthChanged(float azimuth) {
        // 磁北 → 真北：传感器方位角基于磁北，叠加磁偏角后与基于真北的 Qibla 方位角同基准，
        // 否则罗盘指针会有系统性偏差（磁偏角大的地区可达十几度，影响礼拜朝向）。
        float trueAzimuth = (azimuth + declination + 360f) % 360f;

        // 圆周低通滤波（用向量分量避免 0/360 跳变时的抖动）
        if (Float.isNaN(smoothedAzimuth)) {
            smoothedAzimuth = trueAzimuth;
        } else {
            double curR = Math.toRadians(smoothedAzimuth);
            double newR = Math.toRadians(trueAzimuth);
            double sin = (1 - SMOOTHING) * Math.sin(curR) + SMOOTHING * Math.sin(newR);
            double cos = (1 - SMOOTHING) * Math.cos(curR) + SMOOTHING * Math.cos(newR);
            smoothedAzimuth = (float) ((Math.toDegrees(Math.atan2(sin, cos)) + 360) % 360);
        }

        // 节流：方位变化小于阈值则不重绘，显著降低低端机 UI 负载
        if (!Float.isNaN(lastRenderedAzimuth)) {
            float diff = Math.abs(smoothedAzimuth - lastRenderedAzimuth);
            if (diff > 180) diff = 360 - diff;
            if (diff < RENDER_THRESHOLD) {
                return;
            }
        }
        lastRenderedAzimuth = smoothedAzimuth;
        currentAzimuth = smoothedAzimuth;

        // 罗盘盘面旋转
        if (imgCompass != null) {
            imgCompass.setRotation(-currentAzimuth);
        }

        // 更新 Qibla 指针 + 方位/距离文本（内部已处理，无需再 runOnUiThread）
        updateQiblaUI();
    }

    @Override
    public void onMagneticFieldChanged(float strength, EnhancedCompass.MagneticFieldStatus status) {
        String statusText;
        int statusColor;
        
        switch (status) {
            case NORMAL:
                statusText = getString(R.string.qibla_normal);
                statusColor = getResources().getColor(R.color.colorPrimary);
                hideMagneticWarning();
                // When magnetic field is normal, update calibration status based on sensor accuracy
                if (compassAccuracy == android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    updateCalibrationIndicator(CalibrationStatus.CALIBRATED);
                }
                break;
            case WEAK:
                statusText = getString(R.string.qibla_weak);
                statusColor = 0xFFFF4444; // Red color for weak signal
                showMagneticWarning(getString(R.string.qibla_weak_signal));
                updateCalibrationIndicator(CalibrationStatus.CALIBRATING);
                break;
            case STRONG:
                statusText = getString(R.string.qibla_strong);
                statusColor = 0xFFFFA726; // Orange color for strong interference
                showMagneticWarning(getString(R.string.qibla_strong_interference));
                updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                break;
            case DISTURBED:
                statusText = getString(R.string.qibla_disturbed);
                statusColor = 0xFFFF4444; // Red color for disturbance
                showMagneticWarning(getString(R.string.qibla_magnetic_interference));
                updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                break;
            default:
                statusText = getString(R.string.qibla_unknown);
                statusColor = 0xFF888888; // Grey color
                updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                break;
        }
        
        // Update magnetic strength text and color (main status text)
        if (tvMagneticStrength != null) {
            tvMagneticStrength.setText(statusText);
            tvMagneticStrength.setTextColor(statusColor);
        }
        
        // Update calibration status label (always "Field")
        if (tvCalibrationStatus != null) {
            tvCalibrationStatus.setText(getString(R.string.qibla_field));
            tvCalibrationStatus.setTextColor(0xFF888888); // Grey label color
        }
        
        Log.d(TAG, "Magnetic field status: " + status + " (" + strength + " μT)");
    }

    @Override
    public void onTiltChanged(float tiltAngle, boolean isDeviceLevel) {
        if (tvTiltWarning != null) {
            if (isDeviceLevel) {
                tvTiltWarning.setVisibility(View.GONE);
                    } else {
                tvTiltWarning.setVisibility(View.VISIBLE);
                tvTiltWarning.setText(String.format(getString(R.string.qibla_device_tilted), tiltAngle));
            }
        }
        
        Log.v(TAG, "Device tilt: " + tiltAngle + "° (level: " + isDeviceLevel + ")");
    }

    @Override
    public void onCalibrationNeeded(String reason) {
        showMagneticWarning(reason);
        Log.w(TAG, "Calibration needed: " + reason);
    }

    private void showMagneticWarning(String message) {
        if (calibrationWarning != null) {
            calibrationWarning.setVisibility(View.VISIBLE);
            // Update the warning text
            for (int i = 0; i < calibrationWarning.getChildCount(); i++) {
                View child = calibrationWarning.getChildAt(i);
                if (child instanceof TextView && child.getId() != R.id.btn_calibrate) {
                    ((TextView) child).setText("⚠️ " + message);
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

    @Override
    public void onAccuracyChanged(String sensorName, int accuracy) {
        Log.d(TAG, sensorName + " accuracy changed: " + accuracy);
        
        // Update calibration indicator status
        updateCalibrationIndicator(accuracy);
        
        // If sensor accuracy is unreliable, show calibration suggestion
        if (accuracy == android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE) {
            showMagneticWarning(getString(R.string.qibla_sensor_unreliable));
        }
    }



    /**
     * 更新校准状态指示器
     * @param accuracy 传感器精度值
     */
    private void updateCalibrationIndicator(int accuracy) {
        // Update UI in main thread
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                CalibrationStatus newStatus = determineCalibrationStatus(accuracy);
                
                // Only update UI when status changes
                if (newStatus != currentCalibrationStatus) {
                    currentCalibrationStatus = newStatus;
                    compassAccuracy = accuracy;
                    
                    if (calibrationIndicator != null) {
                                            switch (newStatus) {
                        case UNCALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_red);
                            Log.d(TAG, "Calibration status: Uncalibrated (red ring)");
                            break;
                        case CALIBRATING:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_yellow);
                            Log.d(TAG, "Calibration status: Calibrating (yellow ring)");
                            break;
                        case CALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_green);
                            Log.d(TAG, "Calibration status: Calibrated (red ring with green center)");
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
        // Overloaded method to directly update status
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                currentCalibrationStatus = status;
                
                if (calibrationIndicator != null) {
                    switch (status) {
                        case UNCALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_red);
                            break;
                        case CALIBRATING:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_yellow);
                            break;
                        case CALIBRATED:
                            calibrationIndicator.setBackgroundResource(R.drawable.calibration_status_ring_green);
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
            new CalibrateCompassDialog(requireActivity(), getString(R.string.qibla_calibrate_message)).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing calibration dialog: " + e.getMessage());
            Toast.makeText(requireContext(), R.string.qibla_calibrate_manual, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasLocationPermission() {
        return requireActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        // TODO: Implement permission request logic
        // For simplicity, show a message
                    Toast.makeText(requireContext(), getString(R.string.qibla_location_permission_required), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed, start sensors");
        
        if (enhancedCompass != null) {
            enhancedCompass.start();
        }
        
        startLocationUpdates();
        
        // Start periodic updates
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.post(updateRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Fragment paused, stop sensors");
        
        if (enhancedCompass != null) {
            enhancedCompass.stop();
        }
        
        stopLocationUpdates();
        
        // Stop periodic updates
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        
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
            Log.d(TAG, "Start location updates");
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null && locationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Stop location updates");
        }
    }
}
