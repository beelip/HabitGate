package com.habitgate.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTools {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTools() {}

    public static String today() {
        return LocalDate.now().format(DATE);
    }

    public static String tomorrow() {
        return LocalDate.now().plusDays(1).format(DATE);
    }

    public static String nextDay(String isoDate) {
        return LocalDate.parse(isoDate, DATE).plusDays(1).format(DATE);
    }

    public static String startOfWeek() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY).format(DATE);
    }

    public static String startOfMonth() {
        return LocalDate.now().withDayOfMonth(1).format(DATE);
    }

    public static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h + "時間" + String.format("%02d", m) + "分";
    }

    public static int parseMinutes(String hours, String minutes) {
        int h = parseNonNegative(hours);
        int m = parseNonNegative(minutes);
        return h * 60 + Math.min(m, 59);
    }

    public static int parseNonNegative(String value) {
        if (value == null) return 0;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    public static int[] parseTime(String hhmm, int fallbackHour, int fallbackMinute) {
        try {
            LocalTime t = LocalTime.parse(hhmm);
            return new int[]{t.getHour(), t.getMinute()};
        } catch (Exception e) {
            return new int[]{fallbackHour, fallbackMinute};
        }
    }
}
