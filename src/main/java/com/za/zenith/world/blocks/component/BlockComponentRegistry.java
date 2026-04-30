package com.za.zenith.world.blocks.component;

import com.za.zenith.utils.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Реестр компонентов блоков.
 */
public class BlockComponentRegistry {
    private static final Map<Identifier, Class<? extends BlockComponent>> REGISTRY = new HashMap<>();

    public static void register(Identifier id, Class<? extends BlockComponent> clazz) {
        REGISTRY.put(id, clazz);
    }

    public static Class<? extends BlockComponent> getComponentClass(Identifier id) {
        return REGISTRY.get(id);
    }
    
    public static void init() {
        register(Identifier.of("zenith:container"), ContainerComponent.class);
        register(Identifier.of("zenith:crafting_surface"), CraftingSurfaceComponent.class);
        register(Identifier.of("zenith:carvable"), CarvableComponent.class);
    }
}
