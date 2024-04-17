package com.quran.quranaudio.quiz.extension

import android.content.Context
import android.text.format.DateFormat
import com.blankj.utilcode.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

fun getTodayDate(): String {
    return TimeUtils.getNowString(TimeUtils.getSafeDateFormat("yyyyMMdd"))
}

fun getLocalDateStr(context: Context, date: String): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, date.substring(0, 4).toInt())
    calendar.set(Calendar.MONTH, date.substring(4, 6).toInt() - 1)
    calendar.set(Calendar.DAY_OF_MONTH, date.substring(6, 8).toInt())
    return DateFormat.getMediumDateFormat(context).format(calendar.time)
}

fun Long.isToday(dest: Long): Boolean {
    val srcDate = Date(this)
    val destDate = Date(dest)
    val nowCalendar: Calendar = Calendar.getInstance()
    nowCalendar.time = srcDate

    val dateCalendar: Calendar = Calendar.getInstance()
    dateCalendar.time = destDate
    return nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) && nowCalendar.get(
        Calendar.MONTH
    ) == dateCalendar.get(Calendar.MONTH) && nowCalendar.get(Calendar.DATE) == dateCalendar.get(
        Calendar.DATE
    )
}


fun Long.isNextDay(data: Long): Boolean {
    val srcDate = Date(this)
    val toadyDate = Date(data)
    val nowCalendar: Calendar = Calendar.getInstance()
    nowCalendar.setTime(srcDate)

    val dateCalendar: Calendar = Calendar.getInstance()
    dateCalendar.setTime(toadyDate)
    dateCalendar.add(Calendar.DATE, -1)
    return nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) && nowCalendar.get(
        Calendar.MONTH
    ) == dateCalendar.get(Calendar.MONTH) && nowCalendar.get(Calendar.DATE) == dateCalendar.get(
        Calendar.DATE
    )
}

fun Long.isNextXDay(data: Long, nextDay: Int): Boolean {
    val srcDate = Date(this)
    val toadyDate = Date(data)
    val nowCalendar: Calendar = Calendar.getInstance()
    nowCalendar.setTime(srcDate)

    val dateCalendar: Calendar = Calendar.getInstance()
    dateCalendar.setTime(toadyDate)
    dateCalendar.add(Calendar.DATE, -nextDay)

    return nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) && nowCalendar.get(
        Calendar.MONTH
    ) == dateCalendar.get(Calendar.MONTH) && nowCalendar.get(Calendar.DATE) == dateCalendar.get(
        Calendar.DATE
    )
}

fun Long.isNext3Day(data: Long): Boolean {
    val srcDate = Date(this)
    val toadyDate = Date(data)
    val nowCalendar: Calendar = Calendar.getInstance()
    nowCalendar.setTime(srcDate)

    val dateCalendar: Calendar = Calendar.getInstance()
    dateCalendar.setTime(toadyDate)
    dateCalendar.add(Calendar.DATE, -3)
    return nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) && nowCalendar.get(
        Calendar.MONTH
    ) == dateCalendar.get(Calendar.MONTH) && nowCalendar.get(Calendar.DATE) == dateCalendar.get(
        Calendar.DATE
    )
}


fun getWeekDate(bDate: String): ArrayList<String> {
    val dates = ArrayList<String>()
    val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
    TimeUtils.string2Date(bDate, formatter)?.let {
        val cal = Calendar.getInstance()
        cal.time = it
        println(cal[Calendar.DAY_OF_WEEK])
        while (cal[Calendar.DAY_OF_WEEK] != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_WEEK, -1)
        }
        val s1 = formatter.format(cal.time)
        dates.add(s1)
        for (i in 0..5) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val s2 = formatter.format(cal.time)
            dates.add(s2)
        }
    }
    return dates
}


fun milliSecondsToTimer(milliseconds: Long): String {
    if (milliseconds > 86400000) {
        return "00:00"
    }
    var finalTimerString = ""
    val minutesString: String
    val secondsString: String
    val hours = (milliseconds / (1000 * 60 * 60)).toInt()
    val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
    val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
    secondsString = if (seconds < 10) {
        "0$seconds"
    } else {
        "" + seconds
    }
    minutesString = if (minutes < 10) {
        "0$minutes"
    } else {
        "" + minutes
    }
    if (hours > 0) {
        finalTimerString = "$hours:"
    }
    finalTimerString = "$finalTimerString$minutesString:$secondsString"
    return finalTimerString
}

fun Long.daysBetween(data: Long): Int {
    val srcDate = Date(this)
    val toadyDate = Date(data)
    val nowCalendar: Calendar = Calendar.getInstance()
    nowCalendar.setTime(srcDate)
    val time1 = nowCalendar.timeInMillis

    val dateCalendar: Calendar = Calendar.getInstance()
    dateCalendar.setTime(toadyDate)
    val time2 = dateCalendar.timeInMillis

    return ((time1-time2)/(1000*3600*24)).toInt()
}