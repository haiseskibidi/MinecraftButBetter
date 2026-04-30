package com.za.zenith.engine.resources;

import com.google.gson.JsonElement;
import com.za.zenith.utils.Logger;

public abstract class AbstractSingleFileLoader<T> implements IResourceLoader {
    protected final String filePath;
    
    public AbstractSingleFileLoader(String filePath) {
        this.filePath = filePath;
        AssetManager.registerLoader(this);
    }

    @Override
    public boolean reload(String path) {
        if (path.equals("zenith/" + filePath)) {
            String rawJson = AssetManager.getSnapshot(path);
            if (rawJson != null) {
                JsonElement element = AssetManager.getGson().fromJson(rawJson, JsonElement.class);
                parseAndRegister(element, path);
                return true;
            }
        }
        return false;
    }

    @Override
    public void load(String namespace) {
        String fullPath = namespace + "/" + filePath;
        try {
            String rawJson = AssetManager.readAndSnapshot(fullPath);
            if (rawJson != null) {
                JsonElement element = AssetManager.getGson().fromJson(rawJson, JsonElement.class);
                parseAndRegister(element, fullPath);
            }
        } catch (Exception e) {
            Logger.error("Failed to parse resource " + fullPath + ": " + e.getMessage());
        }
    }

    protected abstract void parseAndRegister(JsonElement root, String sourcePath);
}