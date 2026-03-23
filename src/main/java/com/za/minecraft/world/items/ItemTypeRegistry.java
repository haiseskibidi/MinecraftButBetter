package com.za.minecraft.world.items;

import com.za.minecraft.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ItemTypeRegistry {
    public interface ItemFactory {
        Item create(int id, Identifier identifier, String translationKey, String texturePath);
    }

    private static final Map<String, ItemFactory> FACTORIES = new HashMap<>();

    static {
        register("default", Item::new);
        register("tool", Item::new);
        register("food", Item::new);
        register("block", BlockItem::new);
    }

    public static void register(String type, ItemFactory factory) {
        FACTORIES.put(type.toLowerCase(), factory);
    }

    public static Item create(String type, int id, Identifier identifier, String translationKey, String texturePath) {
        ItemFactory factory = FACTORIES.getOrDefault(type.toLowerCase(), FACTORIES.get("default"));
        return factory.create(id, identifier, translationKey, texturePath);
    }
}
