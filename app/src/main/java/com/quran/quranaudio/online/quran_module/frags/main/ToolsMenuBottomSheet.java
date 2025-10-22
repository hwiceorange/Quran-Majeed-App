package com.quran.quranaudio.online.quran_module.frags.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
                Intent intent = new Intent(requireActivity(), QiblaDirectionActivity.class);
                startActivity(intent);
                dismiss();
                Log.d(TAG, "Launched Qibla Direction");
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
}

