package com.habitgate.app;

public final class Models {
    private Models() {}

    public static class Task {
        public final long id;
        public final String title;
        public final String plannedDate;
        public final String note;

        public Task(long id, String title, String plannedDate, String note) {
            this.id = id;
            this.title = title;
            this.plannedDate = plannedDate;
            this.note = note == null ? "" : note;
        }
    }

    public static class ReduceItem {
        public final long id;
        public final String title;
        public final String note;
        // 使用時間を自動計測する連携アプリのパッケージ名。未連携なら空文字。
        public final String appPackage;
        // 使用時間バーの上限（分）。既定は 480 分（8時間）。
        public final int gaugeMaxMinutes;

        public ReduceItem(long id, String title, String note, String appPackage, int gaugeMaxMinutes) {
            this.id = id;
            this.title = title;
            this.note = note == null ? "" : note;
            this.appPackage = appPackage == null ? "" : appPackage;
            this.gaugeMaxMinutes = gaugeMaxMinutes > 0 ? gaugeMaxMinutes : 480;
        }

        public boolean hasLinkedApp() {
            return !appPackage.isEmpty();
        }
    }

    public static class Record {
        public final long id;
        public final String category;
        public final String title;
        public final String note;
        public final int durationMinutes;
        public final String actualDate;
        public final long createdAt;
        public final boolean synced;

        public Record(long id, String category, String title, String note, int durationMinutes, String actualDate, long createdAt, boolean synced) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.note = note == null ? "" : note;
            this.durationMinutes = durationMinutes;
            this.actualDate = actualDate;
            this.createdAt = createdAt;
            this.synced = synced;
        }
    }

    public static class DayTotal {
        public final String date;
        public final int doMinutes;
        public final int reduceMinutes;

        public DayTotal(String date, int doMinutes, int reduceMinutes) {
            this.date = date;
            this.doMinutes = doMinutes;
            this.reduceMinutes = reduceMinutes;
        }
    }

    public static class CompletedTask {
        public final String title;
        public final String completedDate;
        public final int minutes;

        public CompletedTask(String title, String completedDate, int minutes) {
            this.title = title;
            this.completedDate = completedDate;
            this.minutes = minutes;
        }
    }

    public static class Cycle {
        public final long id;
        public final String cycleDate;
        public final long startAt;
        public final long endAt;
        public final boolean closed;
        public final boolean synced;

        public Cycle(long id, String cycleDate, long startAt, long endAt, boolean closed, boolean synced) {
            this.id = id;
            this.cycleDate = cycleDate;
            this.startAt = startAt;
            this.endAt = endAt;
            this.closed = closed;
            this.synced = synced;
        }
    }
}
