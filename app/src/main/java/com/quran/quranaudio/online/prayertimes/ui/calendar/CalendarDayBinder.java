package com.quran.quranaudio.online.prayertimes.ui.calendar;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.prayertimes.common.HijriHoliday;
import com.quran.quranaudio.online.prayertimes.timings.aladhan.AladhanDate;
import com.quran.quranaudio.online.prayertimes.timings.aladhan.AladhanDateType;
import com.quran.quranaudio.online.R;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class CalendarDayBinder implements DayBinder<DayViewContainer> {

    @Override
    public void bind(@NonNull DayViewContainer dayViewContainer, @NonNull CalendarDay calendarDay) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

        dayViewContainer.setCalendarDay(calendarDay);
        TextView dayTextView = dayViewContainer.getDayTextView();
        TextView hijriDayTextView = dayViewContainer.getHijriDayTextView();
        ImageView hijriCalendarDateMonthTextView = dayViewContainer.getHijriCalendarDateMonthTextView();
        ImageView hijriHolidayDateMonthTextView = dayViewContainer.getHijriHolidayDate();
        hijriCalendarDateMonthTextView.setVisibility(View.INVISIBLE);
        hijriHolidayDateMonthTextView.setVisibility(View.INVISIBLE);
        View calendarDayLayout = dayViewContainer.getCalendarDayLayout();

        LocalDate selectedDate = CalendarActivity.getSelectedDate();
        List<AladhanDate> aladhanDates = CalendarActivity.getHijriDates();

        LocalDate today = LocalDate.now();
        if (calendarDay.getOwner() == DayOwner.THIS_MONTH) {
            if (calendarDay.getDate().equals(today)) {
                updateHijriDate(calendarDay, hijriDayTextView, hijriCalendarDateMonthTextView, hijriHolidayDateMonthTextView, aladhanDates);
                calendarDayLayout.setBackgroundResource(R.drawable.calendar_today_bg);
                dayTextView.setVisibility(View.VISIBLE);
            } else if (calendarDay.getDate().equals(selectedDate)) {
                updateHijriDate(calendarDay, hijriDayTextView, hijriCalendarDateMonthTextView, hijriHolidayDateMonthTextView, aladhanDates);
                calendarDayLayout.setBackgroundResource(R.drawable.calendar_selected_bg);
                dayTextView.setVisibility(View.VISIBLE);
            } else {
                updateHijriDate(calendarDay, hijriDayTextView, hijriCalendarDateMonthTextView, hijriHolidayDateMonthTextView, aladhanDates);
                calendarDayLayout.setBackground(null);
                dayTextView.setVisibility(View.VISIBLE);
            }
        } else {
            dayTextView.setVisibility(View.INVISIBLE);
            hijriDayTextView.setVisibility(View.INVISIBLE);
            hijriHolidayDateMonthTextView.setVisibility(View.INVISIBLE);
            calendarDayLayout.setBackground(null);
        }
        dayTextView.setText(numberFormat.format(calendarDay.getDay()));
    }

    private void updateHijriDate(@NonNull CalendarDay calendarDay, TextView hijriDayTextView, ImageView hijriCalendarDateMonthTextView, ImageView hijriHolidayDateMonthTextView, List<AladhanDate> aladhanDates) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());

        if (aladhanDates != null) {
            String gregorianMonth = aladhanDates.get(0).getGregorian().getMonth().getNumber();

            if (Integer.valueOf(gregorianMonth).equals(calendarDay.getDate().getMonthValue())) {
                AladhanDateType hijriDate = aladhanDates.get(calendarDay.getDay() - 1).getHijri();
                String hijriDayNumber = hijriDate.getDay();
                hijriDayTextView.setText(numberFormat.format(Integer.parseInt(hijriDayNumber)));
                hijriDayTextView.setVisibility(View.VISIBLE);

                if (hijriDayNumber.equals("01")) {
                    hijriCalendarDateMonthTextView.setVisibility(View.VISIBLE);
                }

                HijriHoliday holiday = HijriHoliday.getHoliday(Integer.parseInt(hijriDate.getDay()), Integer.parseInt(hijriDate.getMonth().getNumber()));

                if (holiday != null) {
                    hijriHolidayDateMonthTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @NonNull
    @Override
    public DayViewContainer create(@NonNull View view) {
        return new DayViewContainer(view);
    }
}
