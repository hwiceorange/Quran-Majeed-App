package com.quran.quranaudio.online.compass.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.compass.helper.LocationSave;

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
    private ImageButton btnMapTypeToggle;

    private boolean satelliteMode = false;
    private LatLng userLatLng;

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
        btnMapTypeToggle = view.findViewById(R.id.btn_map_type_toggle);

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
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // T3 市场默认普通地图（省流量、低端机渲染更轻），卫星图作为可选
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);

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
        double savedLat = LocationSave.getLat();
        double savedLon = LocationSave.getLon();
        boolean hasCached = savedLat != 0.0 && savedLon != 0.0;
        if (hasCached) {
            drawForLocation(new LatLng(savedLat, savedLon));
        }

        boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null && isAdded()) {
                        LocationSave.putLocation(location.getLatitude(), location.getLongitude());
                        drawForLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else if (!hasCached && isAdded()) {
                        // 已授权但系统还没有可用的 last location：引导用户去有定位的罗盘页触发一次定位
                        showEmptyState(true);
                    }
                });
            } catch (SecurityException e) {
                Log.w(TAG, "Location permission revoked at runtime", e);
                if (!hasCached) showEmptyState(true);
            }
        } else if (!hasCached) {
            // 无缓存且未授权：展示空状态引导
            showEmptyState(true);
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
            resolveLocationThenDraw();
        }
    }

    private void drawForLocation(LatLng user) {
        if (googleMap == null || !isAdded()) return;
        this.userLatLng = user;

        // 有定位则隐藏空状态引导
        showEmptyState(false);

        googleMap.clear();

        double bearing = computeBearing(user);

        // 用户位置标记：箭头指向麦加（flat + rotation），给出"我 → 麦加"的方向感，
        // 而不是仅一条两端都是点、分不清方向的连线。
        MarkerOptions userMarker = new MarkerOptions()
                .position(user)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .rotation((float) bearing)
                .title(getString(R.string.qibla_map_your_location));
        try {
            userMarker.icon(bitmapFromVector(R.drawable.ic_qibla_user_arrow));
        } catch (Exception e) {
            userMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }
        googleMap.addMarker(userMarker);

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

        updateInfoPanel(user);
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
