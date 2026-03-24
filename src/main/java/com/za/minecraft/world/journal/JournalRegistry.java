package com.za.minecraft.world.journal;

import com.za.minecraft.utils.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalRegistry {
    private static final Map<Identifier, JournalCategory> CATEGORIES = new HashMap<>();
    private static final Map<Identifier, JournalEntry> ENTRIES = new HashMap<>();
    private static final List<Identifier> CATEGORY_ORDER = new ArrayList<>();
    
    private static Identifier lastSelectedCategoryId;
    private static Identifier lastSelectedEntryId;
    private static float lastScrollY = 0;

    public static void registerCategory(JournalCategory category) {
        CATEGORIES.put(category.id(), category);
        if (!CATEGORY_ORDER.contains(category.id())) {
            CATEGORY_ORDER.add(category.id());
        }
    }

    public static void registerEntry(JournalEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    public static JournalCategory getCategory(Identifier id) {
        return CATEGORIES.get(id);
    }

    public static JournalEntry getEntry(Identifier id) {
        return ENTRIES.get(id);
    }

    public static List<JournalCategory> getAllCategories() {
        List<JournalCategory> list = new ArrayList<>();
        for (Identifier id : CATEGORY_ORDER) {
            list.add(CATEGORIES.get(id));
        }
        return list;
    }

    public static void clear() {
        CATEGORIES.clear();
        ENTRIES.clear();
        CATEGORY_ORDER.clear();
    }

    public static Identifier getLastSelectedCategoryId() {
        return lastSelectedCategoryId;
    }

    public static void setLastSelectedCategoryId(Identifier id) {
        lastSelectedCategoryId = id;
    }

    public static Identifier getLastSelectedEntryId() {
        return lastSelectedEntryId;
    }

    public static void setLastSelectedEntryId(Identifier id) {
        lastSelectedEntryId = id;
    }

    public static float getLastScrollY() {
        return lastScrollY;
    }

    public static void setLastScrollY(float y) {
        lastScrollY = y;
    }
}
