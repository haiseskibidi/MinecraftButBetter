package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

public class BlockTypeRegistry {
    public interface BlockFactory {
        BlockDefinition create(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent);
    }

    private static final Map<String, BlockFactory> FACTORIES = new HashMap<>();

    static {
        register("default", BlockDefinition::new);
        register("slab", SlabBlockDefinition::new);
        register("stairs", StairsBlockDefinition::new);
        register("cable", CableBlockDefinition::new);
        register("generator", GeneratorBlockDefinition::new);
        register("lamp", LampBlockDefinition::new);
        register("battery", BatteryBlockDefinition::new);
        register("stump", StumpBlockDefinition::new);
    }

    public static void register(String type, BlockFactory factory) {
        FACTORIES.put(type.toLowerCase(), factory);
    }

    public static BlockDefinition create(String type, int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        BlockFactory factory = FACTORIES.getOrDefault(type.toLowerCase(), FACTORIES.get("default"));
        return factory.create(id, identifier, translationKey, solid, transparent);
    }
}
