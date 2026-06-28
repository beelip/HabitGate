package com.habitgate.app;

public final class Models {
    private Models() {}

    public static class Task {
        public final long id;
        public final String title;
        public final String plannedDate;

        public Task(long id, String title, String plannedDate) {
            this.id = id;
            this.title = title;
            this.plannedDate = plannedDate;
        }
    }

    public static class ReduceItem {
        public final long id;
        public final String title;

        public ReduceItem(long id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    public static class Record {
        public final long id;
        public final String category;
        public final String title;
        public final int durationMinutes;
        public final String actualDate;
        public final long createdAt;
        public final boolean synced;

        public Record(long id, String category, String title, int durationMinutes, String actualDate, long createdAt, boolean synced) {
            this.id = id;
            this.category = category;
            this.title = title;
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
}
