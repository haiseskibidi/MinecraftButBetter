package com.za.zenith.engine.graphics.ui;

import com.za.zenith.utils.LiveReloadable;
import com.za.zenith.utils.Logger;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Global manager for editor undo/redo history.
 * Persistent across Inspector sessions.
 */
public class EditorHistoryManager {
    public static class ChangeRecord {
        public final LiveReloadable root; // The top-level reloadable object
        public final Object target;      // The actual object containing the field
        public final Field field;
        public final Object oldValue;
        public final Object newValue;

        public ChangeRecord(LiveReloadable root, Object target, Field field, Object oldValue, Object newValue) {
            this.root = root;
            this.target = target;
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    private static final Deque<ChangeRecord> undoStack = new ArrayDeque<>();
    private static final Deque<ChangeRecord> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 100;

    public static void pushChange(LiveReloadable root, Object target, Field field, Object oldValue, Object newValue) {
        if (undoStack.size() >= MAX_HISTORY) undoStack.removeLast();
        undoStack.push(new ChangeRecord(root, target, field, oldValue, newValue));
        redoStack.clear(); // Clear redo on new change
        Logger.info("Editor: Pushed change to %s.%s", target.getClass().getSimpleName(), field.getName());
    }

    public static boolean undo() {
        if (undoStack.isEmpty()) return false;
        ChangeRecord record = undoStack.pop();
        try {
            record.field.setAccessible(true);
            record.field.set(record.target, record.oldValue);
            redoStack.push(record);
            
            if (record.root != null) {
                record.root.onLiveReload();
                // We should probably save here too if we want full persistence
            }
            Logger.info("Editor: Undo %s", record.field.getName());
            return true;
        } catch (Exception e) {
            Logger.error("Undo failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean redo() {
        if (redoStack.isEmpty()) return false;
        ChangeRecord record = redoStack.pop();
        try {
            record.field.setAccessible(true);
            record.field.set(record.target, record.newValue);
            undoStack.push(record);
            
            if (record.root != null) record.root.onLiveReload();
            Logger.info("Editor: Redo %s", record.field.getName());
            return true;
        } catch (Exception e) {
            Logger.error("Redo failed: " + e.getMessage());
            return false;
        }
    }

    public static ChangeRecord getLastChange() {
        return undoStack.peek();
    }

    public static void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
