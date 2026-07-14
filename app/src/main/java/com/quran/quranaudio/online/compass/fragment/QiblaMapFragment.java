package com.quran.quranaudio.online.compass.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.compass.helper.EnhancedCompass;
import com.quran.quranaudio.online.compass.helper.LocationSave;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.utils.UserPreferencesUtils;

import java.util.Locale;

/**
 * Qibla 地图视图。
 *
 * 为什么需要它：T3 低端机（Tecno/Infinix/itel 及低配 Redmi/realme）大量缺失或磁力计精度差，
 * 罗盘 Tab 在这些设备上不可用。地图视图只依赖定位、零传感器，是全设备可用的朝向兜底，
 * 也让用户能凭地图参照物自行确信方向（宗教场景的信任验证需求）。
 *
 * 一致性保证：
 * - 定位来源与罗盘 Tab 共用 {@link LocationSave}（罗盘会写入 GPS 定位），两 Tab 数字同源；
 * - 方位角/距离用与 {@link QiblaFragment} 逐字相同的大圆公式，避免两个 Tab 显示不一致；
 * - 天房连线用 GMS 自带 Polyline.geodesic(true)（测地线，贴合地球曲面），不依赖任何外部工具库。
 */
public class QiblaMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "QiblaMapFragment";
    private static final int REQ_LOCATION = 4801;

    // 与 QiblaFragment 完全一致的天房坐标
    private static final double KAABA_LATITUDE = 21.42251d;
    private static final double KAABA_LONGITUDE = 39.82616d;
    private static final LatLng KAABA = new LatLng(KAABA_LATITUDE, KAABA_LONGITUDE);

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView tvBearing;
    private TextView tvDistance;
    private TextView tvAlignHud;
    private ImageButton btnMapTypeToggle;

    private boolean satelliteMode = false;
    private LatLng userLatLng;

    // ===== 实时朝向指示（复用罗盘页的传感器融合 EnhancedCompass）=====
    private EnhancedCompass compass;
    private Vibrator vibrator;
    private Marker userHeadingMarker;      // 用户箭头标记：随设备朝向实时旋转
    private float declination = 0f;        // 磁北→真北 校正（按用户经纬度算）
    private Double qiblaBearing = null;     // 当前定位到天房的真北方位角
    private float smoothedHeading = 0f;     // 平滑后的真北朝向
    private boolean headingInitialized = false;
    private boolean wasAligned = false;

    // 对准阈值 ±5°（宗教场景：过严则永远"未对准"焦虑，过松失去意义，5° 是同类产品常用值）
    private static final float ALIGN_THRESHOLD_DEG = 5f;
    private static final int HUD_COLOR_NEUTRAL = 0xCC2D3A53;
    private static final int HUD_COLOR_ALIGNED = 0xCC2E9E7A;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qibla_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvBearing = view.findViewById(R.id.tv_qibla_bearing);
        tvDistance = view.findViewById(R.id.tv_qibla_distance);
        tvAlignHud = view.findViewById(R.id.tv_qibla_align_hud);
        btnMapTypeToggle = view.findViewById(R.id.btn_map_type_toggle);

        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        // 复用罗盘页的传感器融合引擎（旋转矢量优先 + 低通滤波 + 磁场干扰检测）
        try {
            compass = new EnhancedCompass(requireContext());
            compass.setListener(new EnhancedCompass.EnhancedCompassListener() {
                @Override
                public void onAzimuthChanged(float magneticAzimuth) {
                    onHeadingChanged(magneticAzimuth);
                }

                @Override public void onMagneticFieldChanged(float s, EnhancedCompass.MagneticFieldStatus st) { }
                @Override public void onTiltChanged(float tiltAngle, boolean isDeviceLevel) { }
                @Override public void onCalibrationNeeded(String reason) { }
                @Override public void onAccuracyChanged(String sensorName, int accuracy) { }
            });
        } catch (Exception e) {
            Log.w(TAG, "EnhancedCompass unavailable on this device", e);
            compass = null;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.qibla_map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.qibla_map_container, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        btnMapTypeToggle.setOnClickListener(v -> toggleMapType());

        View emptyBtn = view.findViewById(R.id.btn_qibla_map_enable_location);
        if (emptyBtn != null) {
            emptyBtn.setOnClickListener(v -> requestLocationForMap());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        wasAligned = false;
        if (compass != null) {
            // GAME 采样率：更跟手的实时朝向（约 20ms）
            compass.start(SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (compass != null) {
            compass.stop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (compass != null) {
            compass.stop();
        }
        userHeadingMarker = null;
    }

    // ===== 实时朝向 → 箭头旋转 + 对准 HUD =====

    private void onHeadingChanged(float magneticAzimuth) {
        if (!isAdded()) return;

        // 磁北 → 真北（与罗盘页、qibla 方位角同一基准）
        float trueHeading = (magneticAzimuth + declination + 360f) % 360f;

        if (!headingInitialized) {
            smoothedHeading = trueHeading;
            headingInitialized = true;
        } else {
            smoothedHeading = smoothHeading(smoothedHeading, trueHeading);
        }

        if (userHeadingMarker != null) {
            userHeadingMarker.setRotation(smoothedHeading);
        }
        updateAlignHud();
    }

    /**
     * 自适应低通 + 跨 0/360° 最短路径插值：大偏差更跟手、小偏差更稳,且不会 359°→1° 反向绕圈。
     */
    private float smoothHeading(float current, float target) {
        float diff = ((target - current + 540f) % 360f) - 180f; // 归一化到 [-180,180)
        float factor = Math.abs(diff) > 25f ? 0.35f : 0.18f;
        return (current + factor * diff + 360f) % 360f;
    }

    private void updateAlignHud() {
        if (tvAlignHud == null) return;
        if (qiblaBearing == null || !headingInitialized) {
            tvAlignHud.setVisibility(View.GONE);
            return;
        }

        // delta>0 表示需顺时针（右转）才能对准 qibla
        float delta = (float) (((qiblaBearing - smoothedHeading + 540f) % 360f) - 180f);
        int absDelta = Math.round(Math.abs(delta));
        boolean aligned = absDelta <= ALIGN_THRESHOLD_DEG;

        tvAlignHud.setVisibility(View.VISIBLE);
        if (aligned) {
            tvAlignHud.setText(getString(R.string.qibla_facing_kaaba));
            tvAlignHud.setBackgroundTintList(android.content.res.ColorStateList.valueOf(HUD_COLOR_ALIGNED));
            if (!wasAligned) {
                vibrateAligned();
                wasAligned = true;
            }
        } else {
            tvAlignHud.setText(delta > 0
                    ? getString(R.string.qibla_turn_right, absDelta)
                    : getString(R.string.qibla_turn_left, absDelta));
            tvAlignHud.setBackgroundTintList(android.content.res.ColorStateList.valueOf(HUD_COLOR_NEUTRAL));
            wasAligned = false;
        }
    }

    private void vibrateAligned() {
        try {
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(60);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.d(TAG, "onMapReady: GoogleMap ready");
        this.googleMap = map;

        // T3 市场默认普通地图（省流量、低端机渲染更轻），卫星图作为可选
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);

        // 若定位在地图就绪前已拿到（按钮触发/缓存），此刻补画标注
        if (userLatLng != null) {
            drawOnMap(userLatLng);
        }

        resolveLocationThenDraw();
    }

    private void toggleMapType() {
        if (googleMap == null) return;
        satelliteMode = !satelliteMode;
        googleMap.setMapType(satelliteMode ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
    }

    /**
     * 先用缓存定位（LocationSave，与罗盘 Tab 同源）立即出图，
     * 再异步取一次最新定位刷新，避免用户等待白屏。
     */
    private void resolveLocationThenDraw() {
        // 立即用"已知定位"出图：优先罗盘 Tab 写入的定位，其次复用 App 祈祷时间已解析的定位。
        // 后者最关键——中国等地 Google fused 定位常失败/超时，而祈祷时间那套定位已经成功过，
        // 直接复用可保证地图立刻显示天房+方向+距离，而不是干等 fused 或卡空状态。
        LatLng seed = getSeedLocation();
        boolean hasCached = seed != null;
        if (hasCached) {
            drawForLocation(seed);
        }

        boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            // 已有种子定位则仅"尽力刷新"（拿不到不回退空状态）；无种子则必须靠 fused 出图
            fetchFreshLocationAndDraw(hasCached);
        } else if (!hasCached) {
            showEmptyState(true);
        }
    }

    /**
     * App 当前"已知定位"：① 罗盘 Tab 的 {@link LocationSave}；② 祈祷时间模块已解析并缓存的定位
     * （{@link PreferencesConstants#LOCATION} 里的经纬度）。任一有效即返回，都没有返回 null。
     */
    @Nullable
    private LatLng getSeedLocation() {
        double lat = LocationSave.getLat();
        double lon = LocationSave.getLon();
        if (lat != 0.0 && lon != 0.0) {
            return new LatLng(lat, lon);
        }
        try {
            SharedPreferences p = requireContext()
                    .getSharedPreferences(PreferencesConstants.LOCATION, Context.MODE_PRIVATE);
            double plat = UserPreferencesUtils.getDouble(p, PreferencesConstants.LAST_KNOWN_LATITUDE, 0);
            double plon = UserPreferencesUtils.getDouble(p, PreferencesConstants.LAST_KNOWN_LONGITUDE, 0);
            if (plat != 0.0 && plon != 0.0) {
                return new LatLng(plat, plon);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 已授权时主动拉取当前定位并出图。
     *
     * ⚠️ 修复"点击‘تفعيل الموقع’无反应"：旧逻辑只用 {@code getLastLocation()}，在没有近期
     * 定位缓存时返回 null → 直接回到空状态引导 → 按钮看起来毫无响应。现改为优先
     * {@code getCurrentLocation()} 主动出一个新鲜定位，拿不到再退 {@code getLastLocation()}，
     * 都没有才回空状态。回调在主线程执行（地图绘制要求主线程）。
     */
    @SuppressLint("MissingPermission")
    private void fetchFreshLocationAndDraw(boolean hasCached) {
        try {
            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(current -> {
                        if (current != null && isAdded()) {
                            LocationSave.putLocation(current.getLatitude(), current.getLongitude());
                            drawForLocation(new LatLng(current.getLatitude(), current.getLongitude()));
                        } else {
                            fallbackToLastLocation(hasCached);
                        }
                    })
                    .addOnFailureListener(e -> fallbackToLastLocation(hasCached));
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission revoked at runtime", e);
            if (!hasCached && isAdded()) showEmptyState(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void fallbackToLastLocation(boolean hasCached) {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && isAdded()) {
                    LocationSave.putLocation(location.getLatitude(), location.getLongitude());
                    drawForLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                } else if (!hasCached && isAdded()) {
                    Log.w(TAG, "Both getCurrentLocation and getLastLocation returned null despite permission granted");
                    showEmptyState(true);
                }
            }).addOnFailureListener(e -> {
                if (!hasCached && isAdded()) showEmptyState(true);
            });
        } catch (SecurityException e) {
            if (!hasCached && isAdded()) showEmptyState(true);
        }
    }

    private void requestLocationForMap() {
        boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            resolveLocationThenDraw();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                resolveLocationThenDraw();
            } else if (isAdded()
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // 永久拒绝（"不再询问"）：系统不再弹框，否则按钮点了毫无反应；引导去应用设置手动开启
                Toast.makeText(requireContext(),
                        getString(R.string.qibla_location_permission_required), Toast.LENGTH_LONG).show();
                openAppLocationSettings();
            } else {
                resolveLocationThenDraw();
            }
        }
    }

    private void openAppLocationSettings() {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "openAppLocationSettings failed", e);
        }
    }

    /**
     * 定位可用时的处理。关键：**方位/距离与空状态解除不依赖 googleMap**——
     * 即使地图瓦片因 API Key 授权等问题无法渲染，只要拿到定位就应展示朝向与距离、
     * 并撤掉误导性的"启用位置"空状态（权限其实已授予）。地图标注在 googleMap 就绪后补画。
     */
    private void drawForLocation(LatLng user) {
        if (!isAdded()) return;
        this.userLatLng = user;

        // 缓存到天房的真北方位角 + 该点磁偏角（用于把设备磁北朝向转真北，与实时指示对齐）
        qiblaBearing = computeBearing(user);
        try {
            GeomagneticField geo = new GeomagneticField(
                    (float) user.latitude, (float) user.longitude, 0f, System.currentTimeMillis());
            declination = geo.getDeclination();
        } catch (Exception e) {
            declination = 0f;
        }

        // 有定位即隐藏空状态引导 + 先出方位/距离（都不依赖地图对象）
        showEmptyState(false);
        updateInfoPanel(user);

        if (googleMap != null) {
            drawOnMap(user);
        }
        // googleMap 为空时：userLatLng 已保存，onMapReady 就绪后会补画标注
    }

    private void drawOnMap(LatLng user) {
        if (googleMap == null || !isAdded()) return;

        googleMap.clear();

        double bearing = computeBearing(user);

        // 用户位置箭头：**随设备朝向实时旋转**（sensor 未就绪前先指向天房方位作为合理默认）。
        // 固定的测地线+天房标记表示"目标方向"，这个箭头表示"你正朝向哪"，两者重合即对准。
        float initialRotation = headingInitialized ? smoothedHeading : (float) bearing;
        MarkerOptions userMarker = new MarkerOptions()
                .position(user)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .rotation(initialRotation)
                .title(getString(R.string.qibla_map_your_location));
        try {
            userMarker.icon(bitmapFromVector(R.drawable.ic_qibla_user_arrow));
        } catch (Exception e) {
            userMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }
        userHeadingMarker = googleMap.addMarker(userMarker);

        // 天房标记：使用可辨识的 Kaaba 图标（回退到绿针，保证任何情况都有标记）
        MarkerOptions kaabaMarker = new MarkerOptions()
                .position(KAABA)
                .title(getString(R.string.qibla_map_kaaba));
        try {
            kaabaMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.kaaba));
        } catch (Exception e) {
            kaabaMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }
        googleMap.addMarker(kaabaMarker);

        // 测地线连线（贴合地球曲面的真实朝向）
        googleMap.addPolyline(new PolylineOptions()
                .add(user, KAABA)
                .width(6f)
                .color(0xFF2E9E7A)
                .geodesic(true));

        // 取景：同时容纳用户与天房；跨度过大时回退到以用户为中心
        try {
            LatLngBounds bounds = new LatLngBounds.Builder().include(user).include(KAABA).build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        } catch (Exception e) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(user, 4f));
        }
    }

    /**
     * 大圆方位角：与 QiblaFragment.calculateQiblaDirection() 同公式（真北基准）。
     */
    private double computeBearing(LatLng user) {
        double lat1 = Math.toRadians(user.latitude);
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double deltaLon = Math.toRadians(KAABA_LONGITUDE - user.longitude);
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private double computeDistanceKm(LatLng user) {
        double lat1 = Math.toRadians(user.latitude);
        double lat2 = Math.toRadians(KAABA_LATITUDE);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(KAABA_LONGITUDE - user.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void updateInfoPanel(LatLng user) {
        double bearing = computeBearing(user);
        double distance = computeDistanceKm(user);

        if (tvBearing != null) {
            // 度数 + 罗经点方位词（如「45.0° NE」），比裸角度更直观
            tvBearing.setText(String.format(Locale.ENGLISH, "%.1f° %s",
                    bearing, compassPointWord(bearing)));
        }
        if (tvDistance != null) {
            tvDistance.setText(distance < 1
                    ? "< 1 km"
                    : String.format(Locale.ENGLISH, "%.0f km", distance));
        }
    }

    /**
     * 把方位角映射到 8 向罗经点本地化词（N/NE/E/SE/S/SW/W/NW）。
     */
    private String compassPointWord(double bearing) {
        int[] labels = {
                R.string.compass_point_n, R.string.compass_point_ne, R.string.compass_point_e,
                R.string.compass_point_se, R.string.compass_point_s, R.string.compass_point_sw,
                R.string.compass_point_w, R.string.compass_point_nw};
        int idx = (int) Math.round(bearing / 45.0) % 8;
        return getString(labels[idx]);
    }

    /**
     * 矢量 drawable → BitmapDescriptor（GoogleMap 标记不接受矢量资源直接使用）。
     */
    private com.google.android.gms.maps.model.BitmapDescriptor bitmapFromVector(int drawableId) {
        android.graphics.drawable.Drawable d =
                androidx.core.content.ContextCompat.getDrawable(requireContext(), drawableId);
        if (d == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 96;
        int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 96;
        d.setBounds(0, 0, w, h);
        android.graphics.Bitmap bmp =
                android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        d.draw(new android.graphics.Canvas(bmp));
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    /**
     * 空状态：无缓存定位且无法取得定位时，展示"开启定位"引导，避免用户面对空白地图。
     */
    private void showEmptyState(boolean show) {
        View root = getView();
        if (root == null) return;
        View emptyView = root.findViewById(R.id.qibla_map_empty_state);
        View panel = root.findViewById(R.id.qibla_map_info_panel);
        if (emptyView != null) emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (panel != null) panel.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
