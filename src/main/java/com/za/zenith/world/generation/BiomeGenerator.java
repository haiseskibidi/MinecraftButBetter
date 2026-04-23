package com.za.zenith.world.generation;

import com.za.zenith.utils.Identifier;

import java.util.Collection;

public class BiomeGenerator {
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise detailNoise;
    
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new SimplexNoise(seed + 7000);
        this.humidityNoise = new SimplexNoise(seed + 8000);
        this.detailNoise = new SimplexNoise(seed + 15000);
    }
    
    public BiomeDefinition getBiome(int x, int z) {
        double scale = 0.0005;
        
        double fuzzX = detailNoise.noise(x * 0.01, z * 0.01) * 12.0;
        double fuzzZ = detailNoise.noise(x * 0.01 + 500, z * 0.01 + 500) * 12.0;
        
        double temp = (temperatureNoise.noise((x + fuzzX) * scale, (z + fuzzZ) * scale) + 1.0) / 2.0;
        double hum = (humidityNoise.noise((x + fuzzX) * scale + 1000, (z + fuzzZ) * scale + 1000) + 1.0) / 2.0;
        
        Collection<BiomeDefinition> biomes = BiomeRegistry.getAll();
        if (biomes.isEmpty()) {
            return null; // Should not happen in normal gameplay
        }

        BiomeDefinition bestBiome = null;
        double minDistance = Double.MAX_VALUE;

        for (BiomeDefinition b : biomes) {
            double dTemp = b.getTemperature() - temp;
            double dHum = b.getHumidity() - hum;
            double dist = dTemp * dTemp + dHum * dHum;
            if (dist < minDistance) {
                minDistance = dist;
                bestBiome = b;
            }
        }
        
        return bestBiome;
    }

    public BlendedBiome getBlendedBiome(int x, int z, int radius) {
        BlendedBiome blended = new BlendedBiome();
        int count = 0;
        // Step of 4 to optimize sampling
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                BiomeDefinition b = getBiome(x + dx, z + dz);
                if (b != null) {
                    blended.baseHeight += b.getBaseHeight();
                    blended.heightVariation += b.getHeightVariation();
                    blended.erosionFactor += b.getErosionFactor();
                    // Just take the center block for surface info
                    if (dx == 0 && dz == 0) {
                        blended.mainBiome = b;
                    }
                    count++;
                }
            }
        }
        if (count > 0) {
            blended.baseHeight /= count;
            blended.heightVariation /= count;
            blended.erosionFactor /= count;
        }
        
        if (blended.mainBiome == null) blended.mainBiome = getBiome(x, z);
        
        return blended;
    }

    public static class BlendedBiome {
        public BiomeDefinition mainBiome;
        public double baseHeight;
        public double heightVariation;
        public double erosionFactor;
    }
}



