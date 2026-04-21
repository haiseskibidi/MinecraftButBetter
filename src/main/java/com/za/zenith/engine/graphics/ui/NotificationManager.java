package com.za.zenith.engine.graphics.ui;

import com.za.zenith.utils.I18n;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lead Developer Mandate: Centralized Notification System (v1.0).
 * Handles item pickup stacks and alert banners with blueprint integration.
 */
public class NotificationManager {
    private static NotificationManager instance;

    private final List<PickupNote> pickupNotes = new ArrayList<>();
    private AlertNote activeAlert = null;

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    /**
     * Pushes a new item pickup notification. Merges with existing if possible.
     */
    public void pushPickup(ItemStack stack) {
        if (stack == null || stack.getCount() <= 0) return;

        for (PickupNote note : pickupNotes) {
            if (note.item == stack.getItem()) {
                note.count += stack.getCount();
                note.timer = 4.0f; // Reset timer
                return;
            }
        }

        pickupNotes.add(new PickupNote(stack.getItem(), stack.getCount()));
    }

    /**
     * Pushes a high-priority alert banner.
     */
    public void pushAlert(String message, Identifier blueprint, float duration) {
        activeAlert = new AlertNote(message, blueprint, duration);
    }

    public void update(float dt) {
        // Update pickups
        Iterator<PickupNote> it = pickupNotes.iterator();
        while (it.hasNext()) {
            PickupNote note = it.next();
            note.timer -= dt;
            if (note.timer <= 0) {
                it.remove();
            }
        }

        // Update alert
        if (activeAlert != null) {
            activeAlert.timer -= dt;
            if (activeAlert.timer <= 0) {
                activeAlert = null;
            }
        }
    }

    public void render(UIRenderer renderer, int sw, int sh) {
        renderPickups(renderer, sw, sh);
        renderAlert(renderer, sw, sh);
    }

    private void renderPickups(UIRenderer renderer, int sw, int sh) {
        int x = sw - 20;
        int y = sh - 100; // Above hotbar
        int spacing = 20;

        for (int i = 0; i < pickupNotes.size(); i++) {
            PickupNote note = pickupNotes.get(i);
            float alpha = Math.min(1.0f, note.timer);
            
            String pluralName = getPluralName(note.item, note.count);
            String text = "+" + note.count + " " + pluralName;
            
            int textWidth = renderer.getFontRenderer().getStringWidth(text, 14);
            renderer.getFontRenderer().drawString(text, x - textWidth, y - i * spacing, 14, sw, sh, 1.0f, 1.0f, 1.0f, alpha);
        }
    }

    private void renderAlert(UIRenderer renderer, int sw, int sh) {
        if (activeAlert == null) return;

        float alpha = Math.min(1.0f, activeAlert.timer * 2.0f);
        int y = 60;
        
        // Render blueprint icon
        if (activeAlert.blueprint != null) {
            renderer.getBlueprintRenderer().render(activeAlert.blueprint, sw / 2 - 80, y - 10, 30, sw, sh, new float[]{1.0f});
        }

        int textWidth = renderer.getFontRenderer().getStringWidth(activeAlert.message, 18);
        renderer.getFontRenderer().drawString(activeAlert.message, (sw - textWidth) / 2, y, 18, sw, sh, 1.0f, 0.2f, 0.2f, alpha);
    }

    /**
     * Logic for Russian plural forms (1, 2, 5).
     */
    private String getPluralName(Item item, int count) {
        String baseKey = item.getIdentifier().toString().replace(":", ".");
        int form = getRussianPluralForm(count);
        String key = "item." + baseKey + "." + form;
        
        String translated = I18n.get(key);
        // Fallback to base name if specific plural form is missing
        return translated.equals(key) ? item.getName() : translated;
    }

    private int getRussianPluralForm(int n) {
        n = Math.abs(n) % 100;
        int n1 = n % 10;
        if (n > 10 && n < 20) return 5;
        if (n1 > 1 && n1 < 5) return 2;
        if (n1 == 1) return 1;
        return 5;
    }

    private static class PickupNote {
        Item item;
        int count;
        float timer = 4.0f;

        PickupNote(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    private static class AlertNote {
        String message;
        Identifier blueprint;
        float timer;

        AlertNote(String message, Identifier blueprint, float timer) {
            this.message = message;
            this.blueprint = blueprint;
            this.timer = timer;
        }
    }
}
