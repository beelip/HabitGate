package com.habitgate.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvBackupManager {
    public static final String BACKUP_FILE_NAME = "habit_gate_data.csv";
    private static final String KEY_BACKUP_TREE_URI = "backup_tree_uri";
    private static final String KEY_BACKUP_FILE_URI = "backup_file_uri";

    private CsvBackupManager() {}

    public static void saveBackupDirectory(Context context, Uri treeUri, int flags) {
        int persistFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (persistFlags != 0) {
            context.getContentResolver().takePersistableUriPermission(treeUri, persistFlags);
        }
        prefs(context).edit()
                .putString(KEY_BACKUP_TREE_URI, treeUri.toString())
                .remove(KEY_BACKUP_FILE_URI)
                .apply();
    }

    public static boolean hasBackupDirectory(Context context) {
        return !backupTreeUri(context).isEmpty();
    }

    public static String backupDirectoryLabel(Context context) {
        String raw = backupTreeUri(context);
        if (raw.isEmpty()) return "未設定";
        Uri uri = Uri.parse(raw);
        String id = DocumentsContract.getTreeDocumentId(uri);
        if (id == null || id.isEmpty()) return raw;
        int sep = id.lastIndexOf(':');
        if (sep >= 0 && sep + 1 < id.length()) return id.substring(sep + 1);
        return id;
    }

    public static String backupTreeUri(Context context) {
        return prefs(context).getString(KEY_BACKUP_TREE_URI, "");
    }

    public static String buildBackupCsv(Context context) {
        HabitDb helper = new HabitDb(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("# habit_gate_csv_v1\n");

        appendSection(sb, "do_tasks",
                "id,title,note,planned_date,created_at,active",
                query(db, "SELECT id,title,note,planned_date,created_at,active FROM do_tasks ORDER BY id ASC"));

        appendSection(sb, "reduce_items",
                "id,title,note,created_at,active",
                query(db, "SELECT id,title,note,created_at,active FROM reduce_items ORDER BY id ASC"));

        appendSection(sb, "records",
                "id,category,title,note,duration_minutes,actual_date,created_at,synced",
                query(db, "SELECT id,category,title,note,duration_minutes,actual_date,created_at,synced FROM records ORDER BY id ASC"));

        appendSection(sb, "cycles",
                "id,cycle_date,start_at,end_at,closed,synced",
                query(db, "SELECT id,cycle_date,start_at,end_at,closed,synced FROM cycles ORDER BY id ASC"));

        return sb.toString();
    }

    public static Uri writeBackupToConfiguredDirectory(Context context) throws Exception {
        String tree = backupTreeUri(context);
        if (tree.isEmpty()) throw new IllegalStateException("CSV更新先フォルダが未設定です");
        ContentResolver resolver = context.getContentResolver();
        Uri fileUri = storedFileUri(context);
        String csv = buildBackupCsv(context);
        if (fileUri != null && tryWrite(resolver, fileUri, csv)) return fileUri;

        Uri treeUri = Uri.parse(tree);
        Uri existing = findExistingBackupFile(resolver, treeUri);
        if (existing != null) {
            write(resolver, existing, csv);
            prefs(context).edit().putString(KEY_BACKUP_FILE_URI, existing.toString()).apply();
            return existing;
        }

        Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        Uri created = DocumentsContract.createDocument(resolver, dirUri, "text/csv", BACKUP_FILE_NAME);
        if (created == null) throw new IllegalStateException("CSVファイルを作成できませんでした");
        write(resolver, created, csv);
        prefs(context).edit().putString(KEY_BACKUP_FILE_URI, created.toString()).apply();
        return created;
    }


    private static Uri findExistingBackupFile(ContentResolver resolver, Uri treeUri) {
        try {
            String treeId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId);
            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            };
            try (Cursor c = resolver.query(children, projection, null, null, null)) {
                if (c == null) return null;
                while (c.moveToNext()) {
                    String documentId = c.getString(0);
                    String displayName = c.getString(1);
                    if (BACKUP_FILE_NAME.equals(displayName)) {
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    public static int importFromCsvUri(Context context, Uri csvUri) throws Exception {
        String csv = readAll(context.getContentResolver(), csvUri);
        Map<String, CsvTable> tables = parseTables(csv);
        if (!tables.containsKey("records") && !tables.containsKey("do_tasks") && !tables.containsKey("reduce_items") && !tables.containsKey("cycles")) {
            throw new IllegalArgumentException("HabitGate のバックアップCSVとして認識できませんでした");
        }
        HabitDb helper = new HabitDb(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        int rows = 0;
        try {
            db.delete("do_tasks", null, null);
            db.delete("reduce_items", null, null);
            db.delete("records", null, null);
            db.delete("cycles", null, null);
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('do_tasks','reduce_items','records','cycles')");

            rows += insertDoTasks(db, tables.get("do_tasks"));
            rows += insertReduceItems(db, tables.get("reduce_items"));
            rows += insertRecords(db, tables.get("records"));
            rows += insertCycles(db, tables.get("cycles"));
            ensureActiveCycle(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    private static SharedPreferences prefs(Context context) {
        return ReminderScheduler.prefs(context);
    }

    private static Uri storedFileUri(Context context) {
        String raw = prefs(context).getString(KEY_BACKUP_FILE_URI, "");
        return raw == null || raw.isEmpty() ? null : Uri.parse(raw);
    }

    private static boolean tryWrite(ContentResolver resolver, Uri uri, String csv) {
        try {
            write(resolver, uri, csv);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void write(ContentResolver resolver, Uri uri, String csv) throws Exception {
        try (OutputStream os = resolver.openOutputStream(uri, "wt")) {
            if (os == null) throw new IllegalStateException("CSV出力ストリームを開けませんでした");
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.write(csv.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readAll(ContentResolver resolver, Uri uri) throws Exception {
        try (InputStream is = resolver.openInputStream(uri); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) throw new IllegalStateException("CSV入力ストリームを開けませんでした");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) >= 0) bos.write(buffer, 0, n);
            String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            return text;
        }
    }

    private static List<List<String>> query(SQLiteDatabase db, String sql) {
        ArrayList<List<String>> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 0; i < c.getColumnCount(); i++) row.add(c.isNull(i) ? "" : c.getString(i));
                rows.add(row);
            }
        }
        return rows;
    }

    private static void appendSection(StringBuilder sb, String section, String header, List<List<String>> rows) {
        sb.append('\n').append("# ").append(section).append('\n');
        sb.append(header).append('\n');
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(csv(row.get(i)));
            }
            sb.append('\n');
        }
    }

    private static String csv(String raw) {
        if (raw == null) return "";
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private static Map<String, CsvTable> parseTables(String csv) {
        Map<String, CsvTable> tables = new LinkedHashMap<>();
        CsvTable current = null;
        for (List<String> record : parseCsvRecords(csv)) {
            if (record.isEmpty()) continue;
            boolean allEmpty = true;
            for (String cell : record) {
                if (!cell.trim().isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) continue;

            String first = record.get(0).trim();
            if (first.startsWith("#")) {
                String name = first.substring(1).trim();
                if (name.equals("habit_gate_csv_v1")) {
                    current = null;
                    continue;
                }
                current = new CsvTable(name);
                tables.put(name, current);
                continue;
            }
            if (current == null) continue;
            if (current.headers.isEmpty()) {
                current.headers.addAll(record);
            } else {
                current.rows.add(toMap(current.headers, record));
            }
        }
        return tables;
    }

    private static List<List<String>> parseCsvRecords(String text) {
        ArrayList<List<String>> records = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else {
                if (ch == '"') {
                    quoted = true;
                } else if (ch == ',') {
                    row.add(cell.toString());
                    cell.setLength(0);
                } else if (ch == '\n' || ch == '\r') {
                    row.add(cell.toString());
                    cell.setLength(0);
                    records.add(row);
                    row = new ArrayList<>();
                    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                } else {
                    cell.append(ch);
                }
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            records.add(row);
        }
        return records;
    }

    private static Map<String, String> toMap(List<String> headers, List<String> cells) {
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i), i < cells.size() ? cells.get(i) : "");
        }
        return map;
    }

    private static int insertDoTasks(SQLiteDatabase db, CsvTable table) {
        if (table == null) return 0;
        int count = 0;
        for (Map<String, String> row : table.rows) {
            ContentValues v = new ContentValues();
            putId(v, row);
            v.put("title", str(row, "title"));
            v.put("note", str(row, "note"));
            v.put("planned_date", str(row, "planned_date", DateTools.today()));
            v.put("created_at", lng(row, "created_at", System.currentTimeMillis()));
            v.put("active", integer(row, "active", 1));
            db.insert("do_tasks", null, v);
            count++;
        }
        return count;
    }

    private static int insertReduceItems(SQLiteDatabase db, CsvTable table) {
        if (table == null) return 0;
        int count = 0;
        for (Map<String, String> row : table.rows) {
            ContentValues v = new ContentValues();
            putId(v, row);
            v.put("title", str(row, "title"));
            v.put("note", str(row, "note"));
            v.put("created_at", lng(row, "created_at", System.currentTimeMillis()));
            v.put("active", integer(row, "active", 1));
            db.insert("reduce_items", null, v);
            count++;
        }
        return count;
    }

    private static int insertRecords(SQLiteDatabase db, CsvTable table) {
        if (table == null) return 0;
        int count = 0;
        for (Map<String, String> row : table.rows) {
            ContentValues v = new ContentValues();
            putId(v, row);
            v.put("category", str(row, "category", HabitDb.CATEGORY_DO));
            v.put("title", str(row, "title"));
            v.put("note", str(row, "note"));
            v.put("duration_minutes", integer(row, "duration_minutes", 0));
            v.put("actual_date", str(row, "actual_date", DateTools.today()));
            v.put("created_at", lng(row, "created_at", System.currentTimeMillis()));
            v.put("synced", integer(row, "synced", 0));
            db.insert("records", null, v);
            count++;
        }
        return count;
    }

    private static int insertCycles(SQLiteDatabase db, CsvTable table) {
        if (table == null) return 0;
        int count = 0;
        for (Map<String, String> row : table.rows) {
            ContentValues v = new ContentValues();
            putId(v, row);
            v.put("cycle_date", str(row, "cycle_date", DateTools.today()));
            v.put("start_at", lng(row, "start_at", System.currentTimeMillis()));
            v.put("end_at", lng(row, "end_at", 0));
            v.put("closed", integer(row, "closed", 0));
            v.put("synced", integer(row, "synced", 0));
            db.insert("cycles", null, v);
            count++;
        }
        return count;
    }

    private static void ensureActiveCycle(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT id FROM cycles WHERE closed=0 ORDER BY id DESC LIMIT 1", null)) {
            if (c.moveToFirst()) return;
        }
        ContentValues v = new ContentValues();
        v.put("cycle_date", DateTools.today());
        v.put("start_at", System.currentTimeMillis());
        v.put("end_at", 0);
        v.put("closed", 0);
        v.put("synced", 0);
        db.insert("cycles", null, v);
    }

    private static void putId(ContentValues v, Map<String, String> row) {
        long id = lng(row, "id", 0);
        if (id > 0) v.put("id", id);
    }

    private static String str(Map<String, String> row, String key) {
        return str(row, key, "");
    }

    private static String str(Map<String, String> row, String key, String fallback) {
        String value = row.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int integer(Map<String, String> row, String key, int fallback) {
        try {
            String value = row.get(key);
            return value == null || value.isEmpty() ? fallback : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long lng(Map<String, String> row, String key, long fallback) {
        try {
            String value = row.get(key);
            return value == null || value.isEmpty() ? fallback : Long.parseLong(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static class CsvTable {
        final String name;
        final List<String> headers = new ArrayList<>();
        final List<Map<String, String>> rows = new ArrayList<>();

        CsvTable(String name) {
            this.name = name;
        }
    }
}
