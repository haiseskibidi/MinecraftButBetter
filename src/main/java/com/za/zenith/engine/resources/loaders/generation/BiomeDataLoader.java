package com.za.zenith.engine.resources.loaders.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.generation.BiomeDefinition;
import com.za.zenith.world.generation.BiomeRegistry;

public class BiomeDataLoader extends AbstractJsonLoader<BiomeDefinition> {

    public BiomeDataLoader() {
        super("generation/biomes");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            BiomeDefinition biome = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(obj, BiomeDefinition.class);
            biome.setId(Identifier.of(obj.get("identifier").getAsString()));
            biome.setSourcePath(sourcePath);
            BiomeRegistry.register(biome);
        } catch (Exception e) {
            Logger.error("Failed to parse biome " + sourcePath + ": " + e.getMessage());
        }
    }
}