package com.za.zenith.engine.graphics.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.utils.I18n;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.component.ToolComponent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Lead Developer Mandate: Data-Driven Trigger System (v1.0).
 * Configurable via registry/triggers.json.
 */
public class NotificationTriggers {
    private static NotificationTriggers instance;

    private float durabilityThreshold = 0.15f;
    private float durabilityCooldown = 60.0f;
    private float durabilityAlertDuration = 5.0f;
    private float inventoryAlertDuration = 3.0f;
    private float lastDurabilityWarning = -100.0f;

    private NotificationTriggers() {
        loadConfig();
    }

    public static NotificationTriggers getInstance() {
        if (instance == null) {
            instance = new NotificationTriggers();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/zenith/registry/triggers.json"), StandardCharsets.UTF_8)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            
            if (root.has("durability")) {
                JsonObject dur = root.getAsJsonObject("durability");
                durabilityThreshold = dur.get("threshold").getAsFloat();
                durabilityCooldown = dur.get("cooldown").getAsFloat();
                durabilityAlertDuration = dur.get("alert_duration").getAsFloat();
            }
            
            if (root.has("inventory")) {
                JsonObject inv = root.getAsJsonObject("inventory");
                inventoryAlertDuration = inv.get("alert_duration").getAsFloat();
            }
            
            Logger.info("Notification Triggers loaded from JSON.");
        } catch (Exception e) {
            Logger.error("Failed to load triggers.json: %s", e.getMessage());
        }
    }

    /**
     * Checks the currently held item's durability.
     */
    public void checkDurability(ItemStack stack) {
        if (stack == null || stack.getCount() <= 0) return;
        
        ToolComponent tool = stack.getItem().getComponent(ToolComponent.class);
        if (tool == null) return;

        float currentTime = (float) GameLoop.getInstance().getTimer().getTotalTime();
        if (currentTime - lastDurabilityWarning < durabilityCooldown) return;

        float ratio = (float) stack.getDurability() / tool.maxDurability();
        if (ratio <= durabilityThreshold && ratio > 0) {
            String message = I18n.format("ui.warning.low_durability", (int)(ratio * 100));
            NotificationManager.getInstance().pushAlert(message, Identifier.of("zenith:warning_sign"), durabilityAlertDuration);
            lastDurabilityWarning = currentTime;
        }
    }

    /**
     * Called when an item was tried to be picked up but inventory was full.
     */
    public void onInventoryFull() {
        String message = I18n.get("ui.warning.inventory_full");
        NotificationManager.getInstance().pushAlert(message, Identifier.of("zenith:warning_sign"), inventoryAlertDuration);
    }
}
