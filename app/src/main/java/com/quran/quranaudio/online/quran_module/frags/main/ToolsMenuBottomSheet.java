package com.quran.quranaudio.online.quran_module.frags.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.activities.SixKalmasActivity;
import com.quran.quranaudio.online.activities.ZakatCalculatorActivity;
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.hadith.HadithActivity;
import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;

/**
 * Bottom Sheet Dialog displaying tools menu
 * Provides quick access to: Hadith Books, Qibla Direction, Calendar, Six Kalmas, Zakat Calculator
 */
public class ToolsMenuBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ToolsMenuBottomSheet";
    
    // ⭐ Location permission tracking
    private static final String PREFS_NAME = "LocationPermissionPrefs";
    private static final String KEY_PERMISSION_REQUEST_COUNT = "permission_request_count";
    private static final int MAX_PERMISSION_REQUESTS = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_tools_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Close button
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        // Hadith Books
        LinearLayout toolHadithBooks = view.findViewById(R.id.tool_hadith_books);
        toolHadithBooks.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), HadithActivity.class);
                startActivity(intent);
                dismiss();
                Log.d(TAG, "Launched Hadith Books");
            } catch (Exception e) {
                Log.e(TAG, "Error launching Hadith Books", e);
            }
        });

        // Qibla Direction
        LinearLayout toolQiblaDirection = view.findViewById(R.id.tool_qibla_direction);
        toolQiblaDirection.setOnClickListener(v -> {
            try {
                // ⭐ 点击Qibla功能时检查位置权限
                if (checkLocationPermission()) {
                    // 有权限，直接打开Qibla页面
                    Intent intent = new Intent(requireActivity(), QiblaDirectionActivity.class);
                    startActivity(intent);
                    dismiss();
                    Log.d(TAG, "✅ Launched Qibla Direction with location permission");
                } else {
                    // 没有权限，检查是否还能弹出权限请求
                    int requestCount = getPermissionRequestCount();
                    if (requestCount < MAX_PERMISSION_REQUESTS) {
                        Log.d(TAG, "⚠️ No location permission, showing permission dialog for Qibla feature (count: " + (requestCount + 1) + "/" + MAX_PERMISSION_REQUESTS + ")");
                        Toast.makeText(requireContext(), 
                            "Location permission is required to use Qibla Direction", 
                            Toast.LENGTH_SHORT).show();
                        showPermissionWarningAndIncrementCount();
                        dismiss();
                    } else {
                        Log.d(TAG, "⚠️ Max permission requests reached, cannot show dialog");
                        Toast.makeText(requireContext(), 
                            "Please enable location permission in Settings to use Qibla Direction", 
                            Toast.LENGTH_LONG).show();
                        dismiss();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error launching Qibla Direction", e);
            }
        });

        // Calendar
        LinearLayout toolCalendar = view.findViewById(R.id.tool_calendar);
        toolCalendar.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), CalendarActivity.class);
                startActivity(intent);
                dismiss();
                Log.d(TAG, "Launched Islamic Calendar");
            } catch (Exception e) {
                Log.e(TAG, "Error launching Calendar", e);
            }
        });

        // Six Kalmas
        LinearLayout toolSixKalmas = view.findViewById(R.id.tool_six_kalmas);
        toolSixKalmas.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), SixKalmasActivity.class);
                startActivity(intent);
                dismiss();
                Log.d(TAG, "Launched Six Kalmas");
            } catch (Exception e) {
                Log.e(TAG, "Error launching Six Kalmas", e);
            }
        });

        // Zakat Calculator
        LinearLayout toolZakatCalculator = view.findViewById(R.id.tool_zakat_calculator);
        toolZakatCalculator.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), ZakatCalculatorActivity.class);
                startActivity(intent);
                dismiss();
                Log.d(TAG, "Launched Zakat Calculator");
            } catch (Exception e) {
                Log.e(TAG, "Error launching Zakat Calculator", e);
            }
        });

        Log.d(TAG, "Tools menu bottom sheet initialized");
    }

    @Override
    public int getTheme() {
        // Use existing Peace Bottom Sheet theme for consistent styling
        return R.style.PeaceBottomSheetTheme;
    }
    
    // ⭐ Location permission helper methods
    
    /**
     * Check if location permission is granted
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Get the number of times permission has been requested
     */
    private int getPermissionRequestCount() {
        if (getActivity() == null) return 0;
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        Log.d(TAG, "📊 Current permission request count: " + count);
        return count;
    }
    
    /**
     * Increment the permission request count
     */
    private void incrementPermissionRequestCount() {
        if (getActivity() == null) return;
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        int currentCount = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        int newCount = currentCount + 1;
        prefs.edit().putInt(KEY_PERMISSION_REQUEST_COUNT, newCount).apply();
        Log.d(TAG, "📈 Permission request count incremented: " + currentCount + " → " + newCount);
    }
    
    /**
     * Show permission warning dialog and increment count
     */
    private void showPermissionWarningAndIncrementCount() {
        incrementPermissionRequestCount();
        showPermissionWarning();
    }
    
    /**
     * Show permission warning dialog
     */
    private void showPermissionWarning() {
        if (getActivity() == null) return;
        
        new android.app.AlertDialog.Builder(getActivity())
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to show accurate prayer times and Qibla direction for your area.")
            .setPositiveButton("Grant Permission", (dialog, which) -> {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
                );
            })
            .setNegativeButton("Not Now", (dialog, which) -> {
                dialog.dismiss();
            })
            .create()
            .show();
    }
}

