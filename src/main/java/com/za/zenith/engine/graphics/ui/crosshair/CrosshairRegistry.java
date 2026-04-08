package com.za.zenith.engine.graphics.ui.crosshair;

import com.google.gson.Gson;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all data-driven crosshairs.
 */
public class CrosshairRegistry {
    private static final Map<Identifier, CrosshairDefinition> CROSSHAIRS = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static void register(Identifier id, CrosshairDefinition def) {
        CROSSHAIRS.put(id, def);
    }

    public static CrosshairDefinition get(Identifier id) {
        return CROSSHAIRS.get(id);
    }

    public static void load() {
        try {
            Logger.info("Loading crosshairs...");
            // Try different paths for compatibility
            var is = CrosshairRegistry.class.getResourceAsStream("/zenith/gui/crosshairs/.index");
            if (is == null) is = CrosshairRegistry.class.getResourceAsStream("zenith/gui/crosshairs/.index");
            
            if (is == null) {
                Logger.error("Crosshair index not found!");
                return;
            }

            String indexContent = new String(is.readAllBytes());
            String[] files = indexContent.split("\\r?\\n");
            Logger.info("Found " + files.length + " crosshairs in index");
            
            for (String file : files) {
                if (file.trim().isEmpty()) continue;
                String path = "/zenith/gui/crosshairs/" + file.trim();
                var fileIs = CrosshairRegistry.class.getResourceAsStream(path);
                if (fileIs == null) fileIs = CrosshairRegistry.class.getResourceAsStream(path.substring(1));
                
                if (fileIs != null) {
                    try (InputStreamReader reader = new InputStreamReader(fileIs)) {
                        CrosshairDefinition def = GSON.fromJson(reader, CrosshairDefinition.class);
                        register(Identifier.of(def.getIdentifier()), def);
                        Logger.info("Registered crosshair: " + def.getIdentifier());
                    }
                } else {
                    Logger.error("Crosshair file not found: " + path);
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load crosshairs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static Map<Identifier, CrosshairDefinition> getAll() {
        return CROSSHAIRS;
    }
}


