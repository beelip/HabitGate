package com.habitgate.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 日本の国民の祝日をアルゴリズムで判定する（1980〜2099 の近似。2020 年以降の現行法準拠）。 */
public final class JapaneseHolidays {
    private static final Map<Integer, Set<LocalDate>> CACHE = new HashMap<>();

    private static volatile Set<String> dbDates;      // 同期済みの祝日 ISO 日付
    private static volatile Set<Integer> dbYears;     // dbDates に含まれる年の集合

    private JapaneseHolidays() {}

    /** 祝日同期後などにキャッシュを破棄する。 */
    public static void invalidateCache() {
        dbDates = null;
        dbYears = null;
    }

    /** DB に同期データがある年は DB を正とし、無い年はアルゴリズムで判定する。 */
    public static boolean isHoliday(HabitDb db, LocalDate date) {
        Set<String> dates = dbDates;
        Set<Integer> years = dbYears;
        if (dates == null || years == null) {
            dates = db.getHolidayDates();
            Set<Integer> ys = new HashSet<>();
            for (String d : dates) {
                if (d.length() >= 4) {
                    try {
                        ys.add(Integer.parseInt(d.substring(0, 4)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            years = ys;
            dbDates = dates;
            dbYears = ys;
        }
        if (years.contains(date.getYear())) return dates.contains(date.format(DateTools.DATE));
        return isHoliday(date);
    }

    public static boolean isHoliday(LocalDate date) {
        return holidaysOf(date.getYear()).contains(date);
    }

    private static synchronized Set<LocalDate> holidaysOf(int year) {
        Set<LocalDate> cached = CACHE.get(year);
        if (cached != null) return cached;
        Set<LocalDate> set = compute(year);
        CACHE.put(year, set);
        return set;
    }

    private static Set<LocalDate> compute(int y) {
        Set<LocalDate> base = new HashSet<>();
        // 固定日
        base.add(LocalDate.of(y, 1, 1));    // 元日
        base.add(LocalDate.of(y, 2, 11));   // 建国記念の日
        base.add(LocalDate.of(y, 2, 23));   // 天皇誕生日
        base.add(LocalDate.of(y, 4, 29));   // 昭和の日
        base.add(LocalDate.of(y, 5, 3));    // 憲法記念日
        base.add(LocalDate.of(y, 5, 4));    // みどりの日
        base.add(LocalDate.of(y, 5, 5));    // こどもの日
        base.add(LocalDate.of(y, 8, 11));   // 山の日
        base.add(LocalDate.of(y, 11, 3));   // 文化の日
        base.add(LocalDate.of(y, 11, 23));  // 勤労感謝の日
        // ハッピーマンデー
        base.add(nthMonday(y, 1, 2));       // 成人の日
        base.add(nthMonday(y, 7, 3));       // 海の日
        base.add(nthMonday(y, 9, 3));       // 敬老の日
        base.add(nthMonday(y, 10, 2));      // スポーツの日
        // 春分の日・秋分の日
        int spring = (int) Math.floor(20.8431 + 0.242194 * (y - 1980) - Math.floor((y - 1980) / 4.0));
        int autumn = (int) Math.floor(23.2488 + 0.242194 * (y - 1980) - Math.floor((y - 1980) / 4.0));
        base.add(LocalDate.of(y, 3, spring));
        base.add(LocalDate.of(y, 9, autumn));

        Set<LocalDate> result = new HashSet<>(base);

        // 振替休日: 日曜の祝日の翌日以降で最初の祝日でない日
        for (LocalDate d : base) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                LocalDate sub = d.plusDays(1);
                while (result.contains(sub)) sub = sub.plusDays(1);
                result.add(sub);
            }
        }

        // 国民の休日: 前日と翌日が祝日に挟まれた平日
        LocalDate d = LocalDate.of(y, 1, 2);
        LocalDate end = LocalDate.of(y, 12, 30);
        Set<LocalDate> extra = new HashSet<>();
        while (!d.isAfter(end)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY && !result.contains(d)
                    && result.contains(d.minusDays(1)) && result.contains(d.plusDays(1))) {
                extra.add(d);
            }
            d = d.plusDays(1);
        }
        result.addAll(extra);
        return result;
    }

    private static LocalDate nthMonday(int year, int month, int n) {
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, DayOfWeek.MONDAY));
    }
}
