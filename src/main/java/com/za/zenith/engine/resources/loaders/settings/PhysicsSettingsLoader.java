package com.za.zenith.engine.resources.loaders.settings;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractSingleFileLoader;
import com.za.zenith.engine.resources.AssetManager;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.physics.PhysicsSettings;

public class PhysicsSettingsLoader extends AbstractSingleFileLoader<PhysicsSettings> {
    public PhysicsSettingsLoader() {
        super("registry/physics.json");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        PhysicsSettings settings = AssetManager.getGson().fromJson(root, PhysicsSettings.class);
        if (settings != null) {
            settings.setSourcePath(sourcePath);
            PhysicsSettings.setInstance(settings);
            Logger.info("Loaded physics settings");
        }
    }
}