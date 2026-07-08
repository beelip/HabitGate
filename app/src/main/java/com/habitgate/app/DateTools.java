package com.habitgate.app;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTools {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String[] WEEKDAYS_JA = {"月", "火", "水", "木", "金", "土", "日"};

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

    public static LocalDate parseOrToday(String isoDate) {
        try {
            return LocalDate.parse(isoDate, DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public static String startOfWeek() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY).format(DATE);
    }

    public static String startOfMonth() {
        return LocalDate.now().withDayOfMonth(1).format(DATE);
    }

    public static String startOfWeekOf(String isoDate) {
        return parseOrToday(isoDate).with(DayOfWeek.MONDAY).format(DATE);
    }

    public static String endOfWeekOf(String isoDate) {
        return parseOrToday(isoDate).with(DayOfWeek.SUNDAY).format(DATE);
    }

    public static String startOfMonthOf(String isoDate) {
        return parseOrToday(isoDate).withDayOfMonth(1).format(DATE);
    }

    public static String endOfMonthOf(String isoDate) {
        LocalDate date = parseOrToday(isoDate);
        return date.withDayOfMonth(date.lengthOfMonth()).format(DATE);
    }

    public static String addDaysTo(String isoDate, int days) {
        return parseOrToday(isoDate).plusDays(days).format(DATE);
    }

    public static boolean sameWeek(String a, String b) {
        return startOfWeekOf(a).equals(startOfWeekOf(b));
    }

    public static boolean sameMonth(String a, String b) {
        String na = parseOrToday(a).format(DATE);
        String nb = parseOrToday(b).format(DATE);
        return na.substring(0, 7).equals(nb.substring(0, 7));
    }

    public static String maxDate(String left, String right) {
        if (left == null || left.isEmpty()) return right;
        if (right == null || right.isEmpty()) return left;
        return left.compareTo(right) >= 0 ? left : right;
    }

    public static String formatDisplayDate(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) return "";
        try {
            return LocalDate.parse(isoDate, DATE).format(DISPLAY_DATE);
        } catch (Exception e) {
            return isoDate.replace('-', '/');
        }
    }

    public static String formatShortDateWithWeekday(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(isoDate, DATE);
            String weekday = WEEKDAYS_JA[date.getDayOfWeek().getValue() - 1];
            return String.format("%02d/%02d(%s)", date.getMonthValue(), date.getDayOfMonth(), weekday);
        } catch (Exception e) {
            return isoDate.replace('-', '/');
        }
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

    public static String formatDateTime(long epochMillis) {
        if (epochMillis <= 0) return "";
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DATE_TIME);
    }

    public static long dayStartMillis(String isoDate) {
        return parseOrToday(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long dayEndMillis(String isoDate) {
        return parseOrToday(isoDate).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
