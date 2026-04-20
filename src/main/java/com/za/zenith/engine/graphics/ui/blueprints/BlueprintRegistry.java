package com.za.zenith.engine.graphics.ui.blueprints;

import com.google.gson.Gson;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BlueprintRegistry {
    private static final Map<Identifier, GraphicBlueprint> BLUEPRINTS = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static void init() {
        // Load index and then each blueprint
        loadIndex();
    }

    private static void loadIndex() {
        String indexPath = "/zenith/gui/blueprints/.index";
        try (InputStream in = BlueprintRegistry.class.getResourceAsStream(indexPath)) {
            if (in == null) {
                Logger.warn("Blueprint index NOT FOUND at " + indexPath);
                return;
            }
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    loadBlueprint(line);
                    count++;
                }
            }
            Logger.info("BlueprintRegistry: Initialized " + count + " blueprints from index");
        } catch (Exception e) {
            Logger.error("Failed to load blueprints index: " + e.getMessage());
        }
    }

    private static void loadBlueprint(String filename) {
        String path = "/zenith/gui/blueprints/" + filename + ".json";
        try (InputStream in = BlueprintRegistry.class.getResourceAsStream(path)) {
            if (in != null) {
                GraphicBlueprint def = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), GraphicBlueprint.class);
                if (def.getIdentifier() == null) {
                    Logger.error("Blueprint " + filename + " has no identifier!");
                    return;
                }
                BLUEPRINTS.put(Identifier.of(def.getIdentifier()), def);
                Logger.info("Loaded graphic blueprint: " + def.getIdentifier() + " (" + filename + ")");
            } else {
                Logger.error("Blueprint file not found: " + path);
            }
        } catch (Exception e) {
            Logger.error("Failed to parse blueprint " + filename + ": " + e.getMessage());
        }
    }

    public static GraphicBlueprint get(Identifier id) {
        return BLUEPRINTS.get(id);
    }
}
