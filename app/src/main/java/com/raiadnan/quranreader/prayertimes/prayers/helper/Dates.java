package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.content.Context;
import com.raiadnan.quranreader.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Dates {
    public static String islamicMonthName(Context context, int i) {
        switch (i) {
            case 0:
                return context.getResources().getString(R.string.month1);
            case 1:
                return context.getResources().getString(R.string.month2);
            case 2:
                return context.getResources().getString(R.string.month3);
            case 3:
                return context.getResources().getString(R.string.month4);
            case 4:
                return context.getResources().getString(R.string.month5);
            case 5:
                return context.getResources().getString(R.string.month6);
            case 6:
                return context.getResources().getString(R.string.month7);
            case 7:
                return context.getResources().getString(R.string.month8);
            case 8:
                return context.getResources().getString(R.string.month9);
            case 9:
                return context.getResources().getString(R.string.month10);
            case 10:
                return context.getResources().getString(R.string.month11);
            default:
                return context.getResources().getString(R.string.month12);
        }
    }

    public static String islamicMonthNameE(Context context, int i) {
        switch (i) {
            case 0:
                return context.getResources().getString(R.string.month1e);
            case 1:
                return context.getResources().getString(R.string.month2e);
            case 2:
                return context.getResources().getString(R.string.month3e);
            case 3:
                return context.getResources().getString(R.string.month4e);
            case 4:
                return context.getResources().getString(R.string.month5e);
            case 5:
                return context.getResources().getString(R.string.month6e);
            case 6:
                return context.getResources().getString(R.string.month7e);
            case 7:
                return context.getResources().getString(R.string.month8e);
            case 8:
                return context.getResources().getString(R.string.month9e);
            case 9:
                return context.getResources().getString(R.string.month10e);
            case 10:
                return context.getResources().getString(R.string.month11e);
            default:
                return context.getResources().getString(R.string.month12e);
        }
    }

    public static String gregorianMonthName(Context context, int i) {
        switch (i) {
            case 0:
                return context.getResources().getString(R.string.month1g);
            case 1:
                return context.getResources().getString(R.string.month2g);
            case 2:
                return context.getResources().getString(R.string.month3g);
            case 3:
                return context.getResources().getString(R.string.month4g);
            case 4:
                return context.getResources().getString(R.string.month5g);
            case 5:
                return context.getResources().getString(R.string.month6g);
            case 6:
                return context.getResources().getString(R.string.month7g);
            case 7:
                return context.getResources().getString(R.string.month8g);
            case 8:
                return context.getResources().getString(R.string.month9g);
            case 9:
                return context.getResources().getString(R.string.month10g);
            case 10:
                return context.getResources().getString(R.string.month11g);
            default:
                return context.getResources().getString(R.string.month12g);
        }
    }

    public static String weekDayName(Context context, int i) {
        switch (i) {
            case 0:
                return context.getResources().getString(R.string.day1);
            case 1:
                return context.getResources().getString(R.string.day2);
            case 2:
                return context.getResources().getString(R.string.day3);
            case 3:
                return context.getResources().getString(R.string.day4);
            case 4:
                return context.getResources().getString(R.string.day5);
            case 5:
                return context.getResources().getString(R.string.day6);
            default:
                return context.getResources().getString(R.string.day7);
        }
    }

    public static String getCurrentWeekDay() {
        return new SimpleDateFormat("EEEE, MMMM , dd", Locale.getDefault()).format(Long.valueOf(new Date().getTime())).split(",")[0];
    }
}
