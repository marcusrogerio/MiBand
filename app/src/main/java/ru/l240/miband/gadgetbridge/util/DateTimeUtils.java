package ru.l240.miband.gadgetbridge.util;

import android.text.format.DateUtils;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import ru.l240.miband.MiApplication;


public class DateTimeUtils {
    public static String formatDateTime(Date date) {
        return DateUtils.formatDateTime(MiApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
    }

    public static String formatDate(Date date) {
        return DateUtils.formatDateTime(MiApplication.getContext(), date.getTime(), DateUtils.FORMAT_SHOW_DATE);
//        long dateMillis = date.getTime();
//        if (isToday(dateMillis)) {
//            return "Today";
//        }
//        if (isYesterday(dateMillis)) {
//            return "Yesterday";
//        }
//        DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }


    public static String formatDateRange(Date from, Date to) {
        return DateUtils.formatDateRange(MiApplication.getContext(), from.getTime(), to.getTime(), DateUtils.FORMAT_SHOW_DATE);
    }

    public static Date shiftByDays(Date date, int offset) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(GregorianCalendar.DAY_OF_YEAR, offset);
        Date newDate = cal.getTime();
        return newDate;
    }

    public static Date parseTimeStamp(int timestamp) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000L); // make sure it's converted to long
        return cal.getTime();
    }
}
