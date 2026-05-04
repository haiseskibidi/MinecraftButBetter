package com.za.zenith.world.generation.caves;

import java.util.ArrayList;
import java.util.List;

public class SubterraneanBiomeManager {
    private final List<ICaveDecorator> globalDecorators = new ArrayList<>();

    public SubterraneanBiomeManager() {
        // Register default decorators
        globalDecorators.add(new WoodenBeamDecorator());
    }

    public List<ICaveDecorator> getDecoratorsForChunk(int chunkX, int chunkZ) {
        // In the future, this can return biome-specific decorators
        return globalDecorators;
    }
}