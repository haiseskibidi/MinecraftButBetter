package com.za.zenith.engine.resources.loaders.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.stats.RarityDefinition;
import com.za.zenith.world.items.stats.RarityRegistry;
import org.joml.Vector3f;

public class RarityDataLoader extends AbstractJsonLoader<RarityDefinition> {
    public RarityDataLoader() {
        super("registry/rarities");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        JsonObject obj = root.getAsJsonObject();
        Identifier id = Identifier.of(obj.get("identifier").getAsString());
        JsonArray col = obj.getAsJsonArray("color");
        Vector3f color = new Vector3f(col.get(0).getAsFloat(), col.get(1).getAsFloat(), col.get(2).getAsFloat());
        
        RarityRegistry.register(new RarityDefinition(
            id,
            obj.get("translationKey").getAsString(),
            color,
            obj.has("colorCode") ? obj.get("colorCode").getAsString() : "$f",
            obj.get("affixSlots").getAsInt(),
            obj.get("weight").getAsInt()
        ));
    }
}