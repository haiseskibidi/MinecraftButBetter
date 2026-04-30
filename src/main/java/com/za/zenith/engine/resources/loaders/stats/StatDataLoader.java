package com.za.zenith.engine.resources.loaders.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.stats.StatDefinition;
import com.za.zenith.world.items.stats.StatRegistry;

public class StatDataLoader extends AbstractJsonLoader<StatDefinition> {
    public StatDataLoader() {
        super("registry/stats");
    }

    @Override
    protected void parseAndRegister(JsonElement root, String sourcePath) {
        JsonObject obj = root.getAsJsonObject();
        Identifier id = Identifier.of(obj.get("identifier").getAsString());
        StatRegistry.register(new StatDefinition(
            id,
            obj.get("translationKey").getAsString(),
            obj.get("defaultValue").getAsFloat(),
            obj.get("minValue").getAsFloat(),
            obj.get("maxValue").getAsFloat(),
            StatDefinition.DisplayType.valueOf(obj.get("displayType").getAsString().toUpperCase()),
            StatDefinition.Category.valueOf(obj.get("category").getAsString().toUpperCase())
        ));
    }
}