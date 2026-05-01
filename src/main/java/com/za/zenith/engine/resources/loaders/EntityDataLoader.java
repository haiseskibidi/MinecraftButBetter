package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.entities.EntityDefinition;
import com.za.zenith.entities.EntityRegistry;
import org.joml.Vector3f;

public class EntityDataLoader extends AbstractJsonLoader<EntityDefinition> {

    public EntityDataLoader() {
        super("entities");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            String modelType = obj.get("modelType").getAsString();
            String texture = obj.get("texture").getAsString();
            
            Vector3f visualScale = new Vector3f(1.0f, 1.0f, 1.0f);
            if (obj.has("visualScale")) {
                if (obj.get("visualScale").isJsonArray()) {
                    JsonArray vs = obj.getAsJsonArray("visualScale");
                    visualScale.set(vs.get(0).getAsFloat(), vs.get(1).getAsFloat(), vs.get(2).getAsFloat());
                } else {
                    float s = obj.get("visualScale").getAsFloat();
                    visualScale.set(s, s, s);
                }
            }
            
            Vector3f hitbox = new Vector3f(0.5f, 0.5f, 0.5f);
            if (obj.has("hitbox")) {
                JsonArray h = obj.getAsJsonArray("hitbox");
                hitbox.set(h.get(0).getAsFloat(), h.get(1).getAsFloat(), h.get(2).getAsFloat());
            }

            EntityDefinition def = new EntityDefinition(id, modelType, texture, visualScale, hitbox);
            def.setSourcePath(sourcePath);
            EntityRegistry.register(def);
        } catch (Exception e) {
            Logger.error("Failed to parse entity definition " + sourcePath + ": " + e.getMessage());
        }
    }
}