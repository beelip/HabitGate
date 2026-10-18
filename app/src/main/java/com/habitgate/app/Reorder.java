package com.habitgate.app;

import android.content.Context;
import android.view.DragEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** 行のドラッグ&ドロップ並べ替えの共通処理。 */
public final class Reorder {
    public interface OrderSaver { void save(List<Long> orderedIds); }
    private Reorder() {}

    /** 行にドロップ受け入れを配線する。idsList は表示順の id リスト（呼び出し側が保持）。 */
    public static void wireRow(View row, String tagPrefix, long id, Context context,
                               List<Long> idsList, OrderSaver saver, Runnable refresh) {
        row.setTag(tagPrefix + ":" + id);
        row.setOnDragListener((v, event) -> onDrag(v, event, tagPrefix, context, idsList, saver, refresh));
    }

    private static boolean onDrag(View v, DragEvent event, String tagPrefix, Context context,
                                  List<Long> idsList, OrderSaver saver, Runnable refresh) {
        Object local = event.getLocalState();
        String dragged = local instanceof String ? (String) local : "";
        String target = v.getTag() instanceof String ? (String) v.getTag() : "";
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return dragged.startsWith(tagPrefix + ":") && target.startsWith(tagPrefix + ":");
            case DragEvent.ACTION_DRAG_ENTERED:
                v.setAlpha(0.4f);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
                v.setAlpha(1f);
                return true;
            case DragEvent.ACTION_DROP:
                v.setAlpha(1f);
                handleDrop(dragged, target, context, idsList, saver, refresh);
                return true;
            default:
                return true;
        }
    }

    private static void handleDrop(String dragged, String target, Context context,
                                   List<Long> idsList, OrderSaver saver, Runnable refresh) {
        if (dragged.isEmpty() || target.isEmpty() || dragged.equals(target)) return;
        long draggedId;
        long targetId;
        try {
            draggedId = Long.parseLong(dragged.substring(dragged.indexOf(':') + 1));
            targetId = Long.parseLong(target.substring(target.indexOf(':') + 1));
        } catch (NumberFormatException e) {
            return;
        }
        int from = idsList.indexOf(draggedId);
        int to = idsList.indexOf(targetId);
        if (from < 0 || to < 0) return;
        List<Long> next = new ArrayList<>(idsList);
        next.remove(Long.valueOf(draggedId));
        int insertAt = next.indexOf(targetId);
        if (from < to) insertAt += 1;
        if (insertAt < 0) insertAt = next.size();
        if (insertAt > next.size()) insertAt = next.size();
        next.add(insertAt, draggedId);
        saver.save(next);
        AutoSync.run(context);
        refresh.run();
    }
}
