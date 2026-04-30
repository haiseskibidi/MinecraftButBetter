package com.za.zenith.engine.resources.loaders.settings;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractSingleFileLoader;
import com.za.zenith.engine.resources.AssetManager;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.WorldSettings;

public class WorldSettingsLoader extends AbstractSingleFileLoader<WorldSettings> {
    public WorldSettingsLoader() {
        super("registry/world.json");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        WorldSettings settings = AssetManager.getGson().fromJson(root, WorldSettings.class);
        if (settings != null) {
            settings.setSourcePath(sourcePath);
            WorldSettings.setInstance(settings);
            Logger.info("Loaded world settings");
        }
    }
}