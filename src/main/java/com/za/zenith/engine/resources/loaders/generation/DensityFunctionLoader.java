package com.za.zenith.engine.resources.loaders.generation;

import com.google.gson.JsonElement;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.generation.density.DensityFunctionRegistry;

public class DensityFunctionLoader extends AbstractJsonLoader<JsonElement> {

    public DensityFunctionLoader() {
        super("generation/density_functions");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            // Path: zenith/generation/density_functions/overworld.json
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            String namespace = sourcePath.substring(0, sourcePath.indexOf('/'));
            
            Identifier id = Identifier.of(namespace + ":" + name);
            DensityFunctionRegistry.register(id, el);
        } catch (Exception e) {
            Logger.error("Failed to parse density function " + sourcePath + ": " + e.getMessage());
        }
    }
}