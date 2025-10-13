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
    private double qiblaDirection = 0d;
    private boolean isLocationReady = false;
    private android.os.Handler updateHandler;
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
        
        // Update direction text - show absolute Qibla direction from North
        if (tvHeading != null) {
            tvHeading.setText(String.format(Locale.ENGLISH, "%.1f°", qiblaDirection));
            Log.d(TAG, "Updated heading text to: " + String.format(Locale.ENGLISH, "%.1f°", qiblaDirection));
        }
        
        // Update distance display
        updateDistanceDisplay();
        
        Log.d(TAG, "Qibla UI updated - Direction: " + qiblaDirection + "°, Azimuth: " + currentAzimuth + "°, Relative: " + relativeQiblaDirection + "°");
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
        currentAzimuth = azimuth;
        
        // Update compass rotation
        if (imgCompass != null) {
            imgCompass.setRotation(-azimuth);
        }
        
        // Update Qibla UI with real-time data
        updateQiblaUI();
        
        // Force UI update on main thread
        if (requireActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                // Update direction display with current azimuth
                if (tvHeading != null && isLocationReady) {
                    // Show the absolute Qibla direction from North
                    tvHeading.setText(String.format(Locale.ENGLISH, "%.1f°", qiblaDirection));
                }
                
                // Update distance display
                if (tvDistance != null && currentLocation != null) {
                    updateDistanceDisplay();
            }
        });
    }

        Log.d(TAG, "Azimuth changed to: " + azimuth + "°, Qibla direction: " + qiblaDirection + "°");
    }

    @Override
    public void onMagneticFieldChanged(float strength, EnhancedCompass.MagneticFieldStatus status) {
        if (tvMagneticStrength != null) {
            String statusText;
            int statusColor;
            
            switch (status) {
                case NORMAL:
                    statusText = "Normal";
                    statusColor = getResources().getColor(R.color.colorPrimary);
                    hideMagneticWarning();
                    // When magnetic field is normal, update calibration status based on sensor accuracy
                    if (compassAccuracy == android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                        updateCalibrationIndicator(CalibrationStatus.CALIBRATED);
                    }
                    break;
                case WEAK:
                    statusText = "Weak";
                    statusColor = getResources().getColor(R.color.orange);
                    showMagneticWarning("Weak magnetic field signal");
                    updateCalibrationIndicator(CalibrationStatus.CALIBRATING);
                    break;
                case STRONG:
                    statusText = "Strong";
                    statusColor = getResources().getColor(R.color.red);
                    showMagneticWarning("Strong magnetic field interference");
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
                case DISTURBED:
                    statusText = "Disturbed";
                    statusColor = getResources().getColor(R.color.red);
                    showMagneticWarning("Magnetic interference detected");
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
                default:
                    statusText = "Unknown";
                    statusColor = getResources().getColor(R.color.grey);
                    updateCalibrationIndicator(CalibrationStatus.UNCALIBRATED);
                    break;
            }
            
            tvMagneticStrength.setText(statusText);
            tvMagneticStrength.setTextColor(statusColor);
        }
        
        // Update calibration status text
        if (tvCalibrationStatus != null) {
            String statusDisplayText;
            switch (status) {
                case NORMAL:
                    statusDisplayText = "Field";
                    break;
                case WEAK:
                    statusDisplayText = "Field\nWeak";
                    break;
                case STRONG:
                    statusDisplayText = "Field\nStrong";
                    break;
                case DISTURBED:
                    statusDisplayText = "Field\nDisturbed";
                    break;
                default:
                    statusDisplayText = "Field";
                    break;
            }
            tvCalibrationStatus.setText(statusDisplayText);
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
                tvTiltWarning.setText(String.format(Locale.ENGLISH, "Device tilted %.1f°, keep level", tiltAngle));
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
            showMagneticWarning("Sensor accuracy unreliable, device calibration recommended");
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
            new CalibrateCompassDialog(requireActivity(), "For the most accurate Qibla direction, please calibrate your device compass").show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing calibration dialog: " + e.getMessage());
            Toast.makeText(requireContext(), "Please manually calibrate device compass", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasLocationPermission() {
        return requireActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        // TODO: Implement permission request logic
        // For simplicity, show a message
                    Toast.makeText(requireContext(), "Location permission required to calculate Qibla direction", Toast.LENGTH_LONG).show();
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
