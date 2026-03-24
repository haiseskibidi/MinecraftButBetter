package com.za.minecraft.world.journal;

import com.za.minecraft.utils.Identifier;
import java.util.List;

public record JournalElement(
    Type type,
    String value,
    String path,
    float scale,
    List<Identifier> items,
    String color,      // Hex color like "#FF0000"
    String alignment   // "left", "center", "right"
) {
    public enum Type {
        TEXT,
        IMAGE,
        ITEM_ROW,
        HEADER,
        SPACER,
        TIP,
        RECIPE
    }

    public static JournalElement text(String value) {
        return new JournalElement(Type.TEXT, value, null, 0, null, null, "left");
    }

    public static JournalElement image(String path, float scale) {
        return new JournalElement(Type.IMAGE, null, path, scale, null, null, "center");
    }

    public static JournalElement itemRow(List<Identifier> items) {
        return new JournalElement(Type.ITEM_ROW, null, null, 0, items, null, "left");
    }

    public static JournalElement header(String value) {
        return new JournalElement(Type.HEADER, value, null, 0, null, null, "left");
    }

    public static JournalElement tip(String value) {
        return new JournalElement(Type.TIP, value, null, 0, null, null, "left");
    }

    public static JournalElement spacer() {
        return new JournalElement(Type.SPACER, null, null, 0, null, null, "left");
    }
}
