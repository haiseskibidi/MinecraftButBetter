package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.engine.graphics.ui.GUIConfig;
import com.za.zenith.engine.graphics.ui.GUIRegistry;

public class GUIDataLoader extends AbstractJsonLoader<GUIConfig> {

    public GUIDataLoader() {
        super("gui");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            GUIConfig config = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(obj, GUIConfig.class);
            config.setSourcePath(sourcePath);
            GUIRegistry.register(Identifier.of(config.identifier), config);
        } catch (Exception e) {
            Logger.error("Failed to parse GUI " + sourcePath + ": " + e.getMessage());
        }
    }
}