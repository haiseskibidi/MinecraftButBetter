package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.generation.ScavengeSettings;

public class ScavengeDataLoader extends AbstractJsonLoader<JsonArray> {

    public ScavengeDataLoader() {
        super("registry/scavenge.json");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonArray root = el.getAsJsonArray();
            for (JsonElement e : root) {
                JsonObject obj = e.getAsJsonObject();
                Identifier blockId = Identifier.of(obj.get("block").getAsString());
                float chance = obj.get("chance").getAsFloat();
                ScavengeSettings.register(blockId, chance);
            }
        } catch (Exception e) {
            Logger.error("Failed to parse scavenge settings " + sourcePath + ": " + e.getMessage());
        }
    }
}