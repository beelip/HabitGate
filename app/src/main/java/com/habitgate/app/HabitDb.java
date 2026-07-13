package com.habitgate.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HabitDb extends SQLiteOpenHelper {
    // データ保持のため、旧版と同じ DB 名を維持する。
    private static final String DB_NAME = "friction_habit.db";
    private static final int DB_VERSION = 5;
    public static final String CATEGORY_DO = "DO";
    public static final String CATEGORY_REDUCE = "REDUCE";

    public HabitDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE do_tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "planned_date TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1," +
                "completed INTEGER NOT NULL DEFAULT 0," +
                "completed_date TEXT NOT NULL DEFAULT ''," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "priority INTEGER NOT NULL DEFAULT 0," +
                "due_at INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE TABLE reduce_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "app_package TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1," +
                "gauge_max_minutes INTEGER NOT NULL DEFAULT 480," +
                "sort_order INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE TABLE records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "duration_minutes INTEGER NOT NULL," +
                "actual_date TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "synced INTEGER NOT NULL DEFAULT 0" +
                ")");
        createCyclesTable(db);
        insertCycle(db, DateTools.today(), System.currentTimeMillis());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            if (!hasColumn(db, "do_tasks", "note")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN note TEXT NOT NULL DEFAULT ''");
            }
            if (!hasColumn(db, "reduce_items", "note")) {
                db.execSQL("ALTER TABLE reduce_items ADD COLUMN note TEXT NOT NULL DEFAULT ''");
            }
            if (!hasColumn(db, "records", "note")) {
                db.execSQL("ALTER TABLE records ADD COLUMN note TEXT NOT NULL DEFAULT ''");
            }
            createCyclesTable(db);
            ensureActiveCycle(db);
        }
        if (oldVersion < 3) {
            if (!hasColumn(db, "reduce_items", "app_package")) {
                db.execSQL("ALTER TABLE reduce_items ADD COLUMN app_package TEXT NOT NULL DEFAULT ''");
            }
        }
        if (oldVersion < 4) {
            if (!hasColumn(db, "do_tasks", "completed")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN completed INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, "do_tasks", "completed_date")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN completed_date TEXT NOT NULL DEFAULT ''");
            }
            if (!hasColumn(db, "reduce_items", "gauge_max_minutes")) {
                db.execSQL("ALTER TABLE reduce_items ADD COLUMN gauge_max_minutes INTEGER NOT NULL DEFAULT 480");
            }
        }
        if (oldVersion < 5) {
            if (!hasColumn(db, "do_tasks", "sort_order")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, "do_tasks", "priority")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, "do_tasks", "due_at")) {
                db.execSQL("ALTER TABLE do_tasks ADD COLUMN due_at INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, "reduce_items", "sort_order")) {
                db.execSQL("ALTER TABLE reduce_items ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
            }
            db.execSQL("UPDATE do_tasks SET sort_order = id WHERE sort_order = 0");
            db.execSQL("UPDATE reduce_items SET sort_order = id WHERE sort_order = 0");
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                if (column.equals(c.getString(1))) return true;
            }
        }
        return false;
    }

    private void createCyclesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cycles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cycle_date TEXT NOT NULL," +
                "start_at INTEGER NOT NULL," +
                "end_at INTEGER NOT NULL DEFAULT 0," +
                "closed INTEGER NOT NULL DEFAULT 0," +
                "synced INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    private long insertCycle(SQLiteDatabase db, String cycleDate, long startAt) {
        ContentValues v = new ContentValues();
        v.put("cycle_date", cycleDate);
        v.put("start_at", startAt);
        v.put("end_at", 0);
        v.put("closed", 0);
        v.put("synced", 0);
        return db.insert("cycles", null, v);
    }

    private void ensureActiveCycle(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT id FROM cycles WHERE closed=0 ORDER BY id DESC LIMIT 1", null)) {
            if (c.moveToFirst()) return;
        }
        insertCycle(db, DateTools.today(), System.currentTimeMillis());
    }

    public Models.Cycle getCurrentCycle() {
        SQLiteDatabase db = getWritableDatabase();
        ensureActiveCycle(db);
        try (Cursor c = db.rawQuery(
                "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles WHERE closed=0 ORDER BY id DESC LIMIT 1", null)) {
            if (c.moveToFirst()) return cycleFromCursor(c);
        }
        long id = insertCycle(db, DateTools.today(), System.currentTimeMillis());
        try (Cursor c = db.rawQuery(
                "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles WHERE id=?", new String[]{String.valueOf(id)})) {
            c.moveToFirst();
            return cycleFromCursor(c);
        }
    }

    public Models.Cycle endCurrentCycleAndStartNext() {
        SQLiteDatabase db = getWritableDatabase();
        Models.Cycle current = getCurrentCycle();
        String nextDate = DateTools.nextDay(current.cycleDate);
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            ContentValues carry = new ContentValues();
            carry.put("planned_date", nextDate);
            db.update("do_tasks", carry, "active=1 AND planned_date<=?", new String[]{current.cycleDate});

            ContentValues close = new ContentValues();
            close.put("end_at", now);
            close.put("closed", 1);
            close.put("synced", 0);
            db.update("cycles", close, "id=?", new String[]{String.valueOf(current.id)});

            insertCycle(db, nextDate, now);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return getCurrentCycle();
    }

    public long addDoTask(String title, String plannedDate) {
        return addDoTask(title, "", plannedDate);
    }

    public long addDoTask(String title, String note, String plannedDate) {
        return addDoTask(title, note, plannedDate, 0, 0);
    }

    public long addDoTask(String title, String note, String plannedDate, int priority, long dueAt) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("note", clean(note));
        v.put("planned_date", plannedDate);
        v.put("priority", priority);
        v.put("due_at", dueAt);
        v.put("created_at", System.currentTimeMillis());
        v.put("active", 1);
        long id = db.insert("do_tasks", null, v);
        if (id >= 0) {
            db.execSQL("UPDATE do_tasks SET sort_order = id WHERE id = ?", new Object[]{id});
        }
        return id;
    }

    public long addReduceItem(String title) {
        return addReduceItem(title, "");
    }

    public long addReduceItem(String title, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("note", clean(note));
        v.put("created_at", System.currentTimeMillis());
        v.put("active", 1);
        long id = db.insert("reduce_items", null, v);
        if (id >= 0) {
            db.execSQL("UPDATE reduce_items SET sort_order = id WHERE id = ?", new Object[]{id});
        }
        return id;
    }

    public void deleteDoTask(long id) {
        ContentValues v = new ContentValues();
        v.put("active", 0);
        getWritableDatabase().update("do_tasks", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteReduceItem(long id) {
        ContentValues v = new ContentValues();
        v.put("active", 0);
        getWritableDatabase().update("reduce_items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Models.Task> getActiveDoTasks() {
        return getActiveDoTasks("manual");
    }

    public List<Models.Task> getActiveDoTasks(String sortMode) {
        String orderBy;
        if ("priority".equals(sortMode)) {
            orderBy = "priority DESC, sort_order ASC, id ASC";
        } else if ("due".equals(sortMode)) {
            orderBy = "CASE WHEN due_at=0 THEN 1 ELSE 0 END ASC, due_at ASC, sort_order ASC, id ASC";
        } else if ("planned".equals(sortMode)) {
            orderBy = "planned_date ASC, sort_order ASC, id ASC";
        } else if ("created".equals(sortMode)) {
            orderBy = "created_at ASC, id ASC";
        } else {
            orderBy = "sort_order ASC, id ASC";
        }
        ArrayList<Models.Task> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,planned_date,note,priority,due_at FROM do_tasks WHERE active=1 ORDER BY " + orderBy, null)) {
            while (c.moveToNext()) {
                list.add(new Models.Task(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5)));
            }
        }
        return list;
    }

    public List<Models.Task> getDueDoTasks(String actualDate) {
        ArrayList<Models.Task> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,planned_date,note,priority,due_at FROM do_tasks WHERE active=1 AND planned_date<=? ORDER BY planned_date ASC, id ASC",
                new String[]{actualDate})) {
            while (c.moveToNext()) {
                list.add(new Models.Task(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5)));
            }
        }
        return list;
    }

    public List<Models.ReduceItem> getActiveReduceItems() {
        ArrayList<Models.ReduceItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,note,app_package,gauge_max_minutes FROM reduce_items WHERE active=1 ORDER BY sort_order ASC, id ASC", null)) {
            while (c.moveToNext()) {
                list.add(new Models.ReduceItem(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4)));
            }
        }
        return list;
    }

    public Models.ReduceItem getReduceItem(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,note,app_package,gauge_max_minutes FROM reduce_items WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) {
                return new Models.ReduceItem(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4));
            }
        }
        return null;
    }

    public void setReduceItemAppPackage(long id, String appPackage) {
        ContentValues v = new ContentValues();
        v.put("app_package", appPackage == null ? "" : appPackage.trim());
        getWritableDatabase().update("reduce_items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void setReduceItemGaugeMax(long id, int minutes) {
        ContentValues v = new ContentValues();
        v.put("gauge_max_minutes", Math.max(1, minutes));
        getWritableDatabase().update("reduce_items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public boolean hasRecordOn(String category, String title, String actualDate) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM records WHERE category=? AND title=? AND actual_date=?",
                new String[]{category, title, actualDate})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public boolean hasAutoRecordOn(String category, String title, String actualDate) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM records WHERE category=? AND title=? AND actual_date=? AND note LIKE '自動計測%'",
                new String[]{category, title, actualDate})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public long addRecord(String category, String title, int durationMinutes, String actualDate) {
        return addRecord(category, title, "", durationMinutes, actualDate);
    }

    public long addRecord(String category, String title, String note, int durationMinutes, String actualDate) {
        ContentValues v = new ContentValues();
        v.put("category", category);
        v.put("title", title);
        v.put("note", clean(note));
        v.put("duration_minutes", Math.max(0, durationMinutes));
        v.put("actual_date", actualDate);
        v.put("created_at", System.currentTimeMillis());
        v.put("synced", 0);
        return getWritableDatabase().insert("records", null, v);
    }

    public void completeDoTask(long id, String completedDate) {
        ContentValues v = new ContentValues();
        v.put("active", 0);
        v.put("completed", 1);
        v.put("completed_date", completedDate);
        getWritableDatabase().update("do_tasks", v, "id=?", new String[]{String.valueOf(id)});
    }

    public Models.Task getDoTask(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,planned_date,note,priority,due_at FROM do_tasks WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) {
                return new Models.Task(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5));
            }
        }
        return null;
    }

    public void updateDoTask(long id, String title, String note, String plannedDate, int priority, long dueAt) {
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("note", clean(note));
        v.put("planned_date", plannedDate);
        v.put("priority", priority);
        v.put("due_at", dueAt);
        getWritableDatabase().update("do_tasks", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void updateReduceItem(long id, String title, String note) {
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("note", clean(note));
        getWritableDatabase().update("reduce_items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void saveDoTaskOrder(List<Long> orderedIds) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < orderedIds.size(); i++) {
                ContentValues v = new ContentValues();
                v.put("sort_order", i);
                db.update("do_tasks", v, "id=?", new String[]{String.valueOf(orderedIds.get(i))});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveReduceItemOrder(List<Long> orderedIds) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < orderedIds.size(); i++) {
                ContentValues v = new ContentValues();
                v.put("sort_order", i);
                db.update("reduce_items", v, "id=?", new String[]{String.valueOf(orderedIds.get(i))});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Models.CompletedTask> getCompletedTaskTotals(String from, String to) {
        ArrayList<Models.CompletedTask> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id,title,completed_date FROM do_tasks " +
                        "WHERE completed=1 AND completed_date>=? AND completed_date<=? " +
                        "ORDER BY completed_date DESC, id DESC",
                new String[]{from, to})) {
            while (c.moveToNext()) {
                String title = c.getString(1);
                String completedDate = c.getString(2);
                String previousCompletedDate = "";
                try (Cursor pc = db.rawQuery(
                        "SELECT MAX(completed_date) FROM do_tasks WHERE completed=1 AND title=? AND completed_date<?",
                        new String[]{title, completedDate})) {
                    if (pc.moveToFirst() && !pc.isNull(0)) previousCompletedDate = pc.getString(0);
                }
                int minutes;
                if (previousCompletedDate.isEmpty()) {
                    try (Cursor rc = db.rawQuery(
                            "SELECT IFNULL(SUM(duration_minutes),0) FROM records WHERE category='DO' AND title=? AND actual_date<=?",
                            new String[]{title, completedDate})) {
                        minutes = rc.moveToFirst() ? rc.getInt(0) : 0;
                    }
                } else {
                    try (Cursor rc = db.rawQuery(
                            "SELECT IFNULL(SUM(duration_minutes),0) FROM records WHERE category='DO' AND title=? AND actual_date<=? AND actual_date>?",
                            new String[]{title, completedDate, previousCompletedDate})) {
                        minutes = rc.moveToFirst() ? rc.getInt(0) : 0;
                    }
                }
                list.add(new Models.CompletedTask(title, completedDate, minutes));
            }
        }
        return list;
    }

    public int getReduceMinutesOn(String title, String actualDate) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(duration_minutes),0) FROM records WHERE category='REDUCE' AND title=? AND actual_date=?",
                new String[]{title, actualDate})) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    public int getReduceAutoMinutesOn(String title, String actualDate) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(duration_minutes),0) FROM records WHERE category='REDUCE' AND title=? AND actual_date=? AND note LIKE '自動計測%'",
                new String[]{title, actualDate})) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    public void carryOverDoTask(long id, String nextDate) {
        ContentValues v = new ContentValues();
        v.put("planned_date", nextDate);
        getWritableDatabase().update("do_tasks", v, "id=? AND active=1", new String[]{String.valueOf(id)});
    }

    public Models.Record getRecord(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,note,duration_minutes,actual_date,created_at,synced FROM records WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return recordFromCursor(c);
        }
        return null;
    }

    public void updateRecord(long id, int durationMinutes, String note, String actualDate) {
        ContentValues v = new ContentValues();
        v.put("duration_minutes", Math.max(0, durationMinutes));
        v.put("note", clean(note));
        v.put("actual_date", actualDate);
        v.put("synced", 0);
        getWritableDatabase().update("records", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteRecord(long id) {
        getWritableDatabase().delete("records", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Models.Record> getRecords(String fromDateInclusive, String toDateInclusive) {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,note,duration_minutes,actual_date,created_at,synced FROM records " +
                        "WHERE actual_date>=? AND actual_date<=? ORDER BY actual_date DESC, id DESC",
                new String[]{fromDateInclusive, toDateInclusive})) {
            while (c.moveToNext()) list.add(recordFromCursor(c));
        }
        return list;
    }

    public List<Models.Record> getAllRecords() {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,note,duration_minutes,actual_date,created_at,synced FROM records ORDER BY actual_date DESC, id DESC", null)) {
            while (c.moveToNext()) list.add(recordFromCursor(c));
        }
        return list;
    }

    public List<Models.Record> getUnsyncedRecords() {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,note,duration_minutes,actual_date,created_at,synced FROM records WHERE synced=0 ORDER BY id ASC", null)) {
            while (c.moveToNext()) list.add(recordFromCursor(c));
        }
        return list;
    }

    public void markSynced(List<Long> ids) {
        if (ids.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("synced", 1);
            for (Long id : ids) db.update("records", v, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Models.Cycle> getCycles(String fromDateInclusive, String toDateInclusive) {
        ArrayList<Models.Cycle> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles " +
                        "WHERE cycle_date>=? AND cycle_date<=? ORDER BY cycle_date DESC, id DESC",
                new String[]{fromDateInclusive, toDateInclusive})) {
            while (c.moveToNext()) list.add(cycleFromCursor(c));
        }
        return list;
    }

    public List<Models.Cycle> getAllCycles() {
        ArrayList<Models.Cycle> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles ORDER BY cycle_date DESC, id DESC", null)) {
            while (c.moveToNext()) list.add(cycleFromCursor(c));
        }
        return list;
    }

    public List<Models.Cycle> getUnsyncedClosedCycles() {
        ArrayList<Models.Cycle> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles WHERE synced=0 AND closed=1 ORDER BY id ASC", null)) {
            while (c.moveToNext()) list.add(cycleFromCursor(c));
        }
        return list;
    }

    public void markCyclesSynced(List<Long> ids) {
        if (ids.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("synced", 1);
            for (Long id : ids) db.update("cycles", v, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Models.DayTotal> getDayTotals(String fromDateInclusive, String toDateInclusive) {
        ArrayList<Models.DayTotal> totals = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT actual_date," +
                        "SUM(CASE WHEN category='DO' THEN duration_minutes ELSE 0 END) AS do_minutes," +
                        "SUM(CASE WHEN category='REDUCE' THEN duration_minutes ELSE 0 END) AS reduce_minutes " +
                        "FROM records WHERE actual_date>=? AND actual_date<=? GROUP BY actual_date ORDER BY actual_date ASC",
                new String[]{fromDateInclusive, toDateInclusive})) {
            while (c.moveToNext()) totals.add(new Models.DayTotal(c.getString(0), c.getInt(1), c.getInt(2)));
        }
        return totals;
    }

    public JSONArray recordsToJson(List<Models.Record> records) throws Exception {
        JSONArray arr = new JSONArray();
        for (Models.Record r : records) {
            JSONObject o = new JSONObject();
            o.put("local_id", r.id);
            o.put("category", r.category);
            o.put("title", r.title);
            o.put("note", r.note);
            o.put("duration_minutes", r.durationMinutes);
            o.put("actual_date", r.actualDate);
            o.put("created_at", r.createdAt);
            arr.put(o);
        }
        return arr;
    }

    public JSONArray cyclesToJson(List<Models.Cycle> cycles) throws Exception {
        JSONArray arr = new JSONArray();
        for (Models.Cycle c : cycles) {
            JSONObject o = new JSONObject();
            o.put("local_id", c.id);
            o.put("cycle_date", c.cycleDate);
            o.put("start_at", c.startAt);
            o.put("end_at", c.endAt);
            o.put("closed", c.closed);
            arr.put(o);
        }
        return arr;
    }

    private Models.Record recordFromCursor(Cursor c) {
        return new Models.Record(
                c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4),
                c.getString(5), c.getLong(6), c.getInt(7) == 1);
    }

    private Models.Cycle cycleFromCursor(Cursor c) {
        return new Models.Cycle(
                c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getInt(4) == 1, c.getInt(5) == 1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
