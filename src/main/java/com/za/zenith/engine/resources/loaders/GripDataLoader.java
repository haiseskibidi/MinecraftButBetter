package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.engine.graphics.model.GripDefinition;
import com.za.zenith.engine.graphics.model.GripRegistry;

public class GripDataLoader extends AbstractJsonLoader<GripDefinition> {

    public GripDataLoader() {
        super("grips");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            // Path: zenith/grips/shovel.json
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            String namespace = sourcePath.substring(0, sourcePath.indexOf('/'));
            
            GripDefinition def = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(el, GripDefinition.class);
            def.setSourcePath(sourcePath);
            GripRegistry.register(Identifier.of(namespace, name), def);
        } catch (Exception e) {
            Logger.error("Failed to parse grip " + sourcePath + ": " + e.getMessage());
        }
    }
}