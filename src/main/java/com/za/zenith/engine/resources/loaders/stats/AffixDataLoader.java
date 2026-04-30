package com.za.zenith.engine.resources.loaders.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.stats.AffixDefinition;
import com.za.zenith.world.items.stats.AffixRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AffixDataLoader extends AbstractJsonLoader<AffixDefinition> {
    public AffixDataLoader() {
        super("registry/affixes");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        JsonObject obj = root.getAsJsonObject();
        Identifier id = Identifier.of(obj.get("identifier").getAsString());
        Identifier rarityId = Identifier.of(obj.get("rarityId").getAsString());
        AffixDefinition.Type type = AffixDefinition.Type.valueOf(obj.get("type").getAsString().toUpperCase());
        
        Map<Identifier, Float> stats = new HashMap<>();
        JsonObject statsObj = obj.getAsJsonObject("stats");
        for (String key : statsObj.keySet()) {
            stats.put(Identifier.of(key), statsObj.get(key).getAsFloat());
        }
        
        List<String> applicableTo = new ArrayList<>();
        JsonArray appArr = obj.getAsJsonArray("applicableTo");
        for (JsonElement e : appArr) {
            applicableTo.add(e.getAsString());
        }
        
        AffixRegistry.register(new AffixDefinition(
            id,
            obj.get("translationKey").getAsString(),
            rarityId,
            type,
            stats,
            applicableTo
        ));
    }
}