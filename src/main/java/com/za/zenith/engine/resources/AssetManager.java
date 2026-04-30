package com.za.zenith.engine.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.za.zenith.utils.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssetManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(com.za.zenith.world.blocks.component.BlockComponent.class, new com.za.zenith.world.blocks.component.BlockComponentAdapter())
        .create();
        
    private static final Map<String, String> snapshots = new HashMap<>();
    private static final List<IResourceLoader> loaders = new ArrayList<>();

    public static Gson getGson() {
        return GSON;
    }

    public static String getSnapshot(String path) {
        return snapshots.get(path);
    }
    
    public static Set<String> getLoadedPaths() {
        return snapshots.keySet();
    }

    public static void registerLoader(IResourceLoader loader) {
        loaders.add(loader);
    }

    public static String readAndSnapshot(String path) {
        try (InputStream is = AssetManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            String rawJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            snapshots.put(path, rawJson);
            return rawJson;
        } catch (Exception e) {
            Logger.error("Failed to read and snapshot " + path + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Сохраняет JSON на диск и инициирует перезагрузку ресурса.
     */
    public static void saveAndReload(String path, JsonElement element) {
        String rawJson = GSON.toJson(element);
        
        // 1. Сохраняем на диск (для IDE/репозитория)
        File file = new File(new File(System.getProperty("user.dir"), "src/main/resources"), path);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(rawJson);
            Logger.info("Saved resource to disk: " + path);
        } catch (Exception e) {
            Logger.error("Failed to save resource " + path + ": " + e.getMessage());
        }

        // 2. Обновляем снимок в памяти
        snapshots.put(path, rawJson);

        // 3. Инициируем Hot Reload
        boolean handled = false;
        for (IResourceLoader loader : loaders) {
            if (loader.reload(path)) {
                handled = true;
                break;
            }
        }
        
        if (handled) {
            Logger.info("Hot-reloaded resource: " + path);
        } else {
            Logger.warn("No loader handled reload for: " + path + ". Changes might not be visible until restart.");
        }
    }
}