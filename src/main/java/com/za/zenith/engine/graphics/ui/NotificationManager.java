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
     * Pushes a new item pickup notification.
     */
    public void pushPickup(ItemStack stack) {
        if (stack == null || stack.getCount() <= 0) return;

        Identifier stackRarity = stack.getRarity();
        List<Identifier> stackAffixes = new ArrayList<>(stack.getActiveAffixes());

        // Traverse backwards to find the most recent notification for this item type AND rarity AND affixes
        for (int i = pickupNotes.size() - 1; i >= 0; i--) {
            PickupNote note = pickupNotes.get(i);
            if (note.item.getId() == stack.getItem().getId() && note.rarity.equals(stackRarity) && note.affixes.equals(stackAffixes)) {
                // If the item was picked up recently, stack it with the existing notification
                if (note.timeSinceLastAdd < 1.0f) {
                    note.count += stack.getCount();
                    note.timer = 4.0f; // Reset display timer
                    note.timeSinceLastAdd = 0.0f; // Reset accumulation window
                    return;
                }
                break;
            }
        }

        pickupNotes.add(new PickupNote(stack.getItem(), stack.getCount(), stackRarity, stackAffixes));
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
            note.timeSinceLastAdd += dt;
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
        com.za.zenith.utils.Identifier hudId = com.za.zenith.utils.Identifier.of("zenith:hud");
        GUIConfig config = GUIRegistry.get(hudId);
        if (config == null || config.hudElements == null) return;

        renderPickups(renderer, sw, sh, config.hudElements.get("pickups"));
        renderAlert(renderer, sw, sh, config.hudElements.get("alerts"));
    }

    private void renderPickups(UIRenderer renderer, int sw, int sh, GUIConfig.HUDElementConfig cfg) {
        if (cfg == null || !cfg.visible || pickupNotes.isEmpty()) return;

        int fontSize = cfg.fontSize;
        int spacing = cfg.spacing; 
        
        int[] basePos = com.za.zenith.engine.graphics.ui.renderers.HUDRenderer.calculateElementPos(cfg, sw, sh, 100, fontSize);
        int x = basePos[0];
        int y = basePos[1];

        renderer.setupUIProjection(sw, sh);

        for (int i = 0; i < pickupNotes.size(); i++) {
            PickupNote note = pickupNotes.get(i);
            float alpha = Math.min(1.0f, note.timer);
            
            // Create a temporary stack to use getFullDisplayName()
            ItemStack tempStack = new ItemStack(note.item, note.count);
            tempStack.setRarity(note.rarity);
            for (Identifier affix : note.affixes) {
                tempStack.addAffix(affix);
            }
            
            // Format: rarityColor + bold + "+Count " + FullName (which already includes color+bold)
            com.za.zenith.world.items.stats.RarityDefinition rarityDef = com.za.zenith.world.items.stats.RarityRegistry.get(note.rarity);
            String formatPrefix = (rarityDef != null ? rarityDef.colorCode() : "$f") + "$l";
            
            String text = formatPrefix + "+" + note.count + " " + tempStack.getFullDisplayName();
            
            int currentFontSize = fontSize;
            int textWidth = renderer.getFontRenderer().getStringWidth(text, currentFontSize);
            
            int maxWidth = cfg.maxWidth > 0 ? cfg.maxWidth : 250;
            while (textWidth > maxWidth && currentFontSize > cfg.minFontSize) {
                currentFontSize--;
                textWidth = renderer.getFontRenderer().getStringWidth(text, currentFontSize);
            }
            
            int renderX = x;
            if (cfg.alignX.equals("right")) renderX = x - textWidth;
            else if (cfg.alignX.equals("center")) renderX = x - textWidth / 2;

            int renderY = y;
            if (cfg.anchor.contains("top") || cfg.anchor.equals("center") || cfg.anchor.equals("left")) {
                renderY = y + i * spacing;
            } else {
                renderY = y - i * spacing;
            }

            renderer.getFontRenderer().drawString(text, renderX, renderY, currentFontSize, sw, sh, 1.0f, 1.0f, 1.0f, alpha);
        }
    }

    private void renderAlert(UIRenderer renderer, int sw, int sh, GUIConfig.HUDElementConfig cfg) {
        if (activeAlert == null || cfg == null || !cfg.visible) return;

        float alpha = Math.min(1.0f, activeAlert.timer * 2.0f);
        int fontSize = cfg.fontSize;
        
        int textWidth = renderer.getFontRenderer().getStringWidth(activeAlert.message, fontSize);
        int[] pos = com.za.zenith.engine.graphics.ui.renderers.HUDRenderer.calculateElementPos(cfg, sw, sh, textWidth, fontSize);
        
        if (activeAlert.blueprint != null) {
            int iconSize = (int)(fontSize * 1.5f);
            renderer.getBlueprintRenderer().render(activeAlert.blueprint, pos[0] - iconSize - 10, pos[1] - (iconSize - fontSize) / 2, iconSize, sw, sh, new float[]{1.0f});
        }

        renderer.setupUIProjection(sw, sh);

        float[] color = cfg.color != null ? cfg.color : new float[]{1.0f, 0.2f, 0.2f, 1.0f};
        renderer.getFontRenderer().drawString(activeAlert.message, pos[0], pos[1], fontSize, sw, sh, color[0], color[1], color[2], alpha * color[3]);
    }

    private static class PickupNote {
        Item item;
        int count;
        Identifier rarity;
        List<Identifier> affixes;
        float timer = 4.0f;
        float timeSinceLastAdd = 0.0f;

        PickupNote(Item item, int count, Identifier rarity, List<Identifier> affixes) {
            this.item = item;
            this.count = count;
            this.rarity = rarity;
            this.affixes = affixes;
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
