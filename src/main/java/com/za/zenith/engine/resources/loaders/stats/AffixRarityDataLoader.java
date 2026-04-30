package com.za.zenith.engine.resources.loaders.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.stats.AffixRarityDefinition;
import com.za.zenith.world.items.stats.AffixRarityRegistry;
import org.joml.Vector3f;

public class AffixRarityDataLoader extends AbstractJsonLoader<AffixRarityDefinition> {
    public AffixRarityDataLoader() {
        super("registry/affix_rarities");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        JsonObject obj = root.getAsJsonObject();
        Identifier id = Identifier.of(obj.get("identifier").getAsString());
        JsonArray col = obj.getAsJsonArray("color");
        Vector3f color = new Vector3f(col.get(0).getAsFloat(), col.get(1).getAsFloat(), col.get(2).getAsFloat());
        
        AffixRarityRegistry.register(new AffixRarityDefinition(
            id,
            obj.get("translationKey").getAsString(),
            color,
            obj.get("weight").getAsInt()
        ));
    }
}