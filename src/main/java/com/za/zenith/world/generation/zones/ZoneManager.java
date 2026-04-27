package com.za.zenith.world.generation.zones;

import com.za.zenith.world.generation.BiomeDefinition;
import com.za.zenith.world.generation.BiomeRegistry;
import com.za.zenith.world.generation.SimplexNoise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ZoneManager {
    private final SimplexNoise zoneNoise;
    
    private List<ZoneDefinition> cachedSortedZones = null;
    private final Map<ZoneDefinition, Collection<BiomeDefinition>> cachedAllowedBiomes = new HashMap<>();
    private Collection<BiomeDefinition> cachedAllBiomes = null;

    public ZoneManager(long seed) {
        this.zoneNoise = new SimplexNoise(seed + 20000);
    }

    private void ensureCache() {
        if (cachedSortedZones == null) {
            cachedSortedZones = new ArrayList<>(ZoneRegistry.getAll());
            // Сортируем для детерминированности, иначе порядок в Map случаен!
            cachedSortedZones.sort((a, b) -> a.getId().toString().compareTo(b.getId().toString()));
            
            for (ZoneDefinition zone : cachedSortedZones) {
                if (zone.getAllowedBiomes().isEmpty()) {
                    cachedAllowedBiomes.put(zone, BiomeRegistry.getAll());
                } else {
                    List<BiomeDefinition> biomes = zone.getAllowedBiomes().stream()
                            .map(BiomeRegistry::get)
                            .filter(b -> b != null)
                            .collect(Collectors.toList());
                    cachedAllowedBiomes.put(zone, biomes);
                }
            }
            
            cachedAllBiomes = BiomeRegistry.getAll();
        }
    }

    public ZoneDefinition getZone(int x, int z) {
        ensureCache();
        
        if (cachedSortedZones.isEmpty()) return null;
        
        double scale = 0.001; // Увеличил частоту зон (примерно раз в 1000 блоков)
        double noiseVal = zoneNoise.noise(x * scale, z * scale) * 2.0; // [-2, 2] чтобы чаще выходило за края
        double normalized = Math.max(0.0, Math.min(1.0, (noiseVal + 1.0) / 2.0));
        
        int index = (int) (normalized * cachedSortedZones.size());
        index = Math.max(0, Math.min(cachedSortedZones.size() - 1, index));
        
        return cachedSortedZones.get(index);
    }
    
    public Collection<BiomeDefinition> getAllowedBiomes(int x, int z) {
        ensureCache();
        
        ZoneDefinition zone = getZone(x, z);
        if (zone == null) {
            return cachedAllBiomes;
        }
        return cachedAllowedBiomes.getOrDefault(zone, cachedAllBiomes);
    }
}
