package com.za.zenith.world.generation;

import com.za.zenith.utils.Identifier;

import java.util.Collection;

public class BiomeGenerator {
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise continentalnessNoise;
    private final SimplexNoise erosionNoise;
    private final SimplexNoise weirdnessNoise;
    private final SimplexNoise detailNoise;
    
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new SimplexNoise(seed + 7000);
        this.humidityNoise = new SimplexNoise(seed + 8000);
        this.continentalnessNoise = new SimplexNoise(seed + 9000);
        this.erosionNoise = new SimplexNoise(seed + 10000);
        this.weirdnessNoise = new SimplexNoise(seed + 11000);
        this.detailNoise = new SimplexNoise(seed + 15000);
    }
    
    public BiomeDefinition getBiome(int x, int z) {
        double scale = 0.0005;
        
        double fuzzX = detailNoise.noise(x * 0.01, z * 0.01) * 12.0;
        double fuzzZ = detailNoise.noise(x * 0.01 + 500, z * 0.01 + 500) * 12.0;
        
        double sampleX = (x + fuzzX) * scale;
        double sampleZ = (z + fuzzZ) * scale;

        float temp = (float) ((temperatureNoise.noise(sampleX, sampleZ) + 1.0) / 2.0);
        float hum = (float) ((humidityNoise.noise(sampleX + 1000, sampleZ + 1000) + 1.0) / 2.0);
        float cont = (float) ((continentalnessNoise.noise(sampleX * 0.4 + 2000, sampleZ * 0.4 + 2000) + 1.0) / 2.0);
        float eros = (float) ((erosionNoise.noise(sampleX * 0.6 + 3000, sampleZ * 0.6 + 3000) + 1.0) / 2.0);
        float weird = (float) ((weirdnessNoise.noise(sampleX * 2.0 + 4000, sampleZ * 2.0 + 4000) + 1.0) / 2.0);
        
        Collection<BiomeDefinition> biomes = BiomeRegistry.getAll();
        if (biomes.isEmpty()) {
            return null;
        }

        BiomeDefinition bestBiome = null;
        double minDistance = Double.MAX_VALUE;

        for (BiomeDefinition b : biomes) {
            for (BiomeDefinition.ClimatePoint cp : b.getClimatePoints()) {
                double dt = cp.temperature - temp;
                double dh = cp.humidity - hum;
                double dc = cp.continentalness - cont;
                double de = cp.erosion - eros;
                double dw = cp.weirdness - weird;
                
                // Евклидово расстояние в 5D пространстве с учетом весов (Continentalness и Erosion важнее для ландшафта)
                double dist = dt * dt + dh * dh + (dc * dc * 1.5) + (de * de * 1.2) + (dw * dw * 0.5) + cp.offset;
                
                if (dist < minDistance) {
                    minDistance = dist;
                    bestBiome = b;
                }
            }
        }
        
        return bestBiome;
    }

    public BiomeDefinition getBiomeFromParams(float temp, float hum, float cont, float eros, float weird) {
        Collection<BiomeDefinition> biomes = BiomeRegistry.getAll();
        if (biomes.isEmpty()) return null;

        BiomeDefinition bestBiome = null;
        double minDistance = Double.MAX_VALUE;

        for (BiomeDefinition b : biomes) {
            for (BiomeDefinition.ClimatePoint cp : b.getClimatePoints()) {
                double dt = cp.temperature - temp;
                double dh = cp.humidity - hum;
                double dc = cp.continentalness - cont;
                double de = cp.erosion - eros;
                double dw = cp.weirdness - weird;
                
                double dist = dt * dt + dh * dh + (dc * dc * 1.5) + (de * de * 1.2) + (dw * dw * 0.5) + cp.offset;
                
                if (dist < minDistance) {
                    minDistance = dist;
                    bestBiome = b;
                }
            }
        }
        return bestBiome;
    }
    
    public float[] getClimateParams(int x, int z) {
        double scale = 0.0005;
        double fuzzX = detailNoise.noise(x * 0.01, z * 0.01) * 12.0;
        double fuzzZ = detailNoise.noise(x * 0.01 + 500, z * 0.01 + 500) * 12.0;
        
        double sampleX = (x + fuzzX) * scale;
        double sampleZ = (z + fuzzZ) * scale;

        return new float[] {
            (float) ((temperatureNoise.noise(sampleX, sampleZ) + 1.0) / 2.0),
            (float) ((humidityNoise.noise(sampleX + 1000, sampleZ + 1000) + 1.0) / 2.0),
            (float) ((continentalnessNoise.noise(sampleX * 0.4 + 2000, sampleZ * 0.4 + 2000) + 1.0) / 2.0),
            (float) ((erosionNoise.noise(sampleX * 0.6 + 3000, sampleZ * 0.6 + 3000) + 1.0) / 2.0),
            (float) ((weirdnessNoise.noise(sampleX * 2.0 + 4000, sampleZ * 2.0 + 4000) + 1.0) / 2.0)
        };
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



