package com.za.zenith.engine.resources.loaders.settings;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractSingleFileLoader;
import com.za.zenith.engine.resources.AssetManager;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.generation.GenerationSettings;

public class GenerationSettingsLoader extends AbstractSingleFileLoader<GenerationSettings> {
    public GenerationSettingsLoader() {
        super("registry/generation.json");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        GenerationSettings settings = AssetManager.getGson().fromJson(root, GenerationSettings.class);
        if (settings != null) {
            settings.setSourcePath(sourcePath);
            GenerationSettings.setInstance(settings);
            Logger.info("Loaded generation settings");
        }
    }
}