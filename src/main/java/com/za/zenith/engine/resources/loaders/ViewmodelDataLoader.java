package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.engine.graphics.model.ViewmodelDefinition;
import com.za.zenith.engine.graphics.model.ModelRegistry;

public class ViewmodelDataLoader extends AbstractJsonLoader<ViewmodelDefinition> {

    public ViewmodelDataLoader() {
        super("models/viewmodel");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            String namespace = sourcePath.substring(0, sourcePath.indexOf('/'));
            
            ViewmodelDefinition def = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(el, ViewmodelDefinition.class);
            // ViewmodelDefinition might not implement LiveReloadable yet, check if needed
            ModelRegistry.registerViewmodel(Identifier.of(namespace, name), def);
        } catch (Exception e) {
            Logger.error("Failed to parse viewmodel " + sourcePath + ": " + e.getMessage());
        }
    }
}