package com.za.zenith.world.blocks.component;

import com.google.gson.*;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import java.lang.reflect.Type;

/**
 * Адаптер для десериализации компонентов блоков из JSON.
 */
public class BlockComponentAdapter implements JsonDeserializer<BlockComponent> {
    @Override
    public BlockComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        if (!obj.has("type")) {
            throw new JsonParseException("Block component missing 'type' field");
        }

        Identifier typeId = Identifier.of(obj.get("type").getAsString());
        Class<? extends BlockComponent> clazz = BlockComponentRegistry.getComponentClass(typeId);

        if (clazz == null) {
            Logger.warn("Unknown block component type: " + typeId);
            return null;
        }

        return context.deserialize(json, clazz);
    }
}
