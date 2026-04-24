package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.NumericalRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class BlockRegistry {
    private static final NumericalRegistry<BlockDefinition> REGISTRY = new NumericalRegistry<>();

    public static void finalizeRegistration() {
        for (BlockDefinition def : REGISTRY.values()) {
            def.computeFlags();
        }
    }

    public static void registerBlock(BlockDefinition def) {
        REGISTRY.register(def.getIdentifier(), def.getId(), def);
    }

    public static BlockDefinition getBlock(int id) {
        BlockDefinition def = REGISTRY.get(id);
        if (def == null) {
            def = REGISTRY.get(Identifier.of("zenith:air"));
        }
        return def;
    }
    
    public static BlockDefinition getBlock(Identifier id) {
        BlockDefinition def = REGISTRY.get(id);
        if (def == null) {
            def = REGISTRY.get(Identifier.of("zenith:air"));
        }
        return def;
    }

    public static BlockTextures getTextures(int id) {
        BlockDefinition def = getBlock(id);
        return def != null ? def.getTextures() : null;
    }

    public static Set<String> allTextureKeys() {
        Set<String> keys = new HashSet<>();
        for (BlockDefinition def : REGISTRY.values()) {
            BlockTextures t = def.getTextures();
            if (t != null) {
                keys.add(t.getTop());
                keys.add(t.getBottom());
                keys.add(t.getNorth());
                keys.add(t.getSouth());
                keys.add(t.getEast());
                keys.add(t.getWest());
            }
        }
        return keys;
    }

    public static Map<Integer, BlockDefinition> getRegisteredBlocks() {
        Map<Integer, BlockDefinition> map = new HashMap<>();
        for (Identifier id : REGISTRY.getIds()) {
            map.put(REGISTRY.getId(id), REGISTRY.get(id));
        }
        return map;
    }
    
    public static NumericalRegistry<BlockDefinition> getRegistry() {
        return REGISTRY;
    }
}


