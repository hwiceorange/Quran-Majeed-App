package com.quran.quranaudio.online.prayertimes.location.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.quran.quranaudio.online.prayertimes.exceptions.LocationException;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.utils.UserPreferencesUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;

import static android.content.Context.MODE_PRIVATE;


/**
 * 定位获取。
 *
 * ⚠️ 历史严重 Bug（本次修复）：旧实现只用 {@link android.location.LocationManager#getLastKnownLocation}，
 * 它返回的是各 provider 的"上一次缓存定位"——在没有任何 App 主动请求定位时，这个值可能是
 * 几天甚至几周前的旧城市（用户报告：授权后启动仍显示几周前的位置）。NETWORK provider 的
 * last-known 尤其陈旧，而旧代码还优先取它。结果：祈祷时间按错误城市计算。
 *
 * 现改为优先用 FusedLocationProviderClient.getCurrentLocation() **主动拉取当前定位**（Task 式，
 * 任意线程可调、无需 Looper），并分层兜底：当前定位 → fused 融合的 last-location → 旧
 * LocationManager last-known → SharedPreferences 上次成功定位。Play Services 不可用的设备
 * （如部分 Transsion 机型）自动降级到旧路径，行为不回退。
 *
 * 精度选 BALANCED_POWER_ACCURACY：城市级足够算祈祷时间，省电省流量、室内(WiFi/基站)也能出，
 * 契合 T3 目标机型与网络环境。
 */
@Singleton
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    // Fused Task 的回调默认在主线程执行；下游 flatMap 会做阻塞式 Geocoder 反查地址，
    // 若在主线程执行可能 ANR。用后台线程执行回调，保证发射及下游都在工作线程。
    private static final java.util.concurrent.Executor CALLBACK_EXECUTOR =
            java.util.concurrent.Executors.newCachedThreadPool();

    private final Context context;

    @Inject
    public LocationHelper(Context context) {
        this.context = context;
    }

    public Single<Location> getLocation() {
        final SharedPreferences sharedPreferences =
                context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        final double lastKnownLatitude =
                UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LATITUDE, 0);
        final double lastKnownLongitude =
                UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LONGITUDE, 0);

        return Single.create(emitter -> {
            try {
                FusedLocationProviderClient fused =
                        LocationServices.getFusedLocationProviderClient(context);
                CancellationTokenSource cts = new CancellationTokenSource();

                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                        .addOnSuccessListener(CALLBACK_EXECUTOR, current -> {
                            if (current != null) {
                                Log.i(TAG, "Fresh location from FusedLocationProvider: "
                                        + current.getLatitude() + ", " + current.getLongitude());
                                emitSuccess(emitter, current);
                            } else {
                                // 当前定位暂拿不到（无信号/刚开机）→ 退到 fused 融合的 last-location
                                fallbackToFusedLastLocation(fused, emitter, lastKnownLatitude, lastKnownLongitude);
                            }
                        })
                        .addOnFailureListener(CALLBACK_EXECUTOR, e -> {
                            Log.w(TAG, "getCurrentLocation failed, falling back", e);
                            fallbackToFusedLastLocation(fused, emitter, lastKnownLatitude, lastKnownLongitude);
                        });
            } catch (Throwable t) {
                // Play Services 不可用等极端情况：降级到旧 LocationManager / 缓存
                Log.w(TAG, "Fused provider unavailable, using legacy fallback", t);
                emitLegacyFallback(emitter, lastKnownLatitude, lastKnownLongitude);
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void fallbackToFusedLastLocation(FusedLocationProviderClient fused,
                                             SingleEmitter<Location> emitter,
                                             double lastLat, double lastLng) {
        try {
            fused.getLastLocation()
                    .addOnSuccessListener(CALLBACK_EXECUTOR, last -> {
                        if (last != null) {
                            Log.i(TAG, "Using fused last-location fallback");
                            emitSuccess(emitter, last);
                        } else {
                            emitLegacyFallback(emitter, lastLat, lastLng);
                        }
                    })
                    .addOnFailureListener(CALLBACK_EXECUTOR, e -> emitLegacyFallback(emitter, lastLat, lastLng));
        } catch (Throwable t) {
            emitLegacyFallback(emitter, lastLat, lastLng);
        }
    }

    /**
     * 最后兜底：旧 LocationManager last-known（仍可能陈旧，但作为最末选项），
     * 再退到 SharedPreferences 上次成功定位；都没有才报错。
     */
    private void emitLegacyFallback(SingleEmitter<Location> emitter, double lastLat, double lastLng) {
        try {
            GPSTracker gpsTracker = new GPSTracker(context);
            if (gpsTracker.canGetLocation()) {
                Location legacy = gpsTracker.getLocation();
                if (legacy != null) {
                    Log.w(TAG, "Using legacy LocationManager last-known fallback");
                    emitSuccess(emitter, legacy);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // 继续退到缓存
        }

        if (lastLat != 0.0 && lastLng != 0.0) {
            Log.w(TAG, "Using cached last-known location from preferences");
            emitSuccess(emitter, buildLocation(lastLat, lastLng));
            return;
        }

        if (!emitter.isDisposed()) {
            emitter.onError(new LocationException(
                    context.getResources().getString(R.string.location_service_unavailable)));
        }
    }

    private void emitSuccess(SingleEmitter<Location> emitter, Location location) {
        if (!emitter.isDisposed()) {
            emitter.onSuccess(location);
        }
    }

    @NonNull
    private Location buildLocation(double latitude, double longitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
