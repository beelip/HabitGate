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
    private static final String DB_NAME = "friction_habit.db";
    private static final int DB_VERSION = 1;
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
                "planned_date TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1" +
                ")");
        db.execSQL("CREATE TABLE reduce_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1" +
                ")");
        db.execSQL("CREATE TABLE records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "duration_minutes INTEGER NOT NULL," +
                "actual_date TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "synced INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 only. Future migrations should preserve records.
    }

    public long addDoTask(String title, String plannedDate) {
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("planned_date", plannedDate);
        v.put("created_at", System.currentTimeMillis());
        v.put("active", 1);
        return getWritableDatabase().insert("do_tasks", null, v);
    }

    public long addReduceItem(String title) {
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("created_at", System.currentTimeMillis());
        v.put("active", 1);
        return getWritableDatabase().insert("reduce_items", null, v);
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
        ArrayList<Models.Task> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,planned_date FROM do_tasks WHERE active=1 ORDER BY planned_date ASC, id ASC", null)) {
            while (c.moveToNext()) {
                list.add(new Models.Task(c.getLong(0), c.getString(1), c.getString(2)));
            }
        }
        return list;
    }

    public List<Models.Task> getDueDoTasks(String actualDate) {
        ArrayList<Models.Task> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,planned_date FROM do_tasks WHERE active=1 AND planned_date<=? ORDER BY planned_date ASC, id ASC",
                new String[]{actualDate})) {
            while (c.moveToNext()) {
                list.add(new Models.Task(c.getLong(0), c.getString(1), c.getString(2)));
            }
        }
        return list;
    }

    public List<Models.ReduceItem> getActiveReduceItems() {
        ArrayList<Models.ReduceItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title FROM reduce_items WHERE active=1 ORDER BY id ASC", null)) {
            while (c.moveToNext()) {
                list.add(new Models.ReduceItem(c.getLong(0), c.getString(1)));
            }
        }
        return list;
    }

    public long addRecord(String category, String title, int durationMinutes, String actualDate) {
        ContentValues v = new ContentValues();
        v.put("category", category);
        v.put("title", title);
        v.put("duration_minutes", Math.max(0, durationMinutes));
        v.put("actual_date", actualDate);
        v.put("created_at", System.currentTimeMillis());
        v.put("synced", 0);
        return getWritableDatabase().insert("records", null, v);
    }

    public void completeDoTask(long id) {
        ContentValues v = new ContentValues();
        v.put("active", 0);
        getWritableDatabase().update("do_tasks", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void carryOverDoTask(long id, String nextDate) {
        ContentValues v = new ContentValues();
        v.put("planned_date", nextDate);
        getWritableDatabase().update("do_tasks", v, "id=? AND active=1", new String[]{String.valueOf(id)});
    }

    public List<Models.Record> getRecords(String fromDateInclusive, String toDateInclusive) {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,duration_minutes,actual_date,created_at,synced FROM records " +
                        "WHERE actual_date>=? AND actual_date<=? ORDER BY actual_date DESC, id DESC",
                new String[]{fromDateInclusive, toDateInclusive})) {
            while (c.moveToNext()) {
                list.add(new Models.Record(
                        c.getLong(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getLong(5), c.getInt(6) == 1));
            }
        }
        return list;
    }

    public List<Models.Record> getAllRecords() {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,duration_minutes,actual_date,created_at,synced FROM records ORDER BY actual_date DESC, id DESC", null)) {
            while (c.moveToNext()) {
                list.add(new Models.Record(
                        c.getLong(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getLong(5), c.getInt(6) == 1));
            }
        }
        return list;
    }

    public List<Models.Record> getUnsyncedRecords() {
        ArrayList<Models.Record> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,category,title,duration_minutes,actual_date,created_at,synced FROM records WHERE synced=0 ORDER BY id ASC", null)) {
            while (c.moveToNext()) {
                list.add(new Models.Record(
                        c.getLong(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getLong(5), false));
            }
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
            for (Long id : ids) {
                db.update("records", v, "id=?", new String[]{String.valueOf(id)});
            }
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
            while (c.moveToNext()) {
                totals.add(new Models.DayTotal(c.getString(0), c.getInt(1), c.getInt(2)));
            }
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
            o.put("duration_minutes", r.durationMinutes);
            o.put("actual_date", r.actualDate);
            o.put("created_at", r.createdAt);
            arr.put(o);
        }
        return arr;
    }
}
