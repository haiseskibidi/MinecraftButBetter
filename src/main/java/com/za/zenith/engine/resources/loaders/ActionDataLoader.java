package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.actions.ActionDefinition;
import com.za.zenith.world.actions.ActionRegistry;

public class ActionDataLoader extends AbstractJsonLoader<ActionDefinition> {

    public ActionDataLoader() {
        super("actions");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            // Path looks like "zenith/actions/walk.json"
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            String namespace = sourcePath.substring(0, sourcePath.indexOf('/'));
            
            ActionDefinition def = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(el, ActionDefinition.class);
            def.setSourcePath(sourcePath);
            ActionRegistry.register(Identifier.of(namespace + ":" + name), def);
        } catch (Exception e) {
            Logger.error("Failed to parse action " + sourcePath + ": " + e.getMessage());
        }
    }
}