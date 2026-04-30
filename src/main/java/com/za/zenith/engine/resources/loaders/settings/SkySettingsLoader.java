package com.za.zenith.engine.resources.loaders.settings;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractSingleFileLoader;
import com.za.zenith.engine.resources.AssetManager;
import com.za.zenith.utils.Logger;
import com.za.zenith.engine.graphics.SkySettings;

public class SkySettingsLoader extends AbstractSingleFileLoader<SkySettings> {
    public SkySettingsLoader() {
        super("registry/celestial.json");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        SkySettings settings = AssetManager.getGson().fromJson(root, SkySettings.class);
        if (settings != null) {
            settings.setSourcePath(sourcePath);
            SkySettings.setInstance(settings);
            Logger.info("Loaded sky settings");
        }
    }
}