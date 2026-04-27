package com.za.zenith.world.generation;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.SplineInterpolator;
import com.za.zenith.world.generation.zones.ZoneManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BiomeGenerator {
    private final ZoneManager zoneManager;
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise continentalnessNoise;
    private final SimplexNoise erosionNoise;
    private final SimplexNoise weirdnessNoise;
    private final SimplexNoise warpNoiseX;
    private final SimplexNoise warpNoiseZ;
    private final SimplexNoise ditherNoise;

    public BiomeGenerator(long seed) {
        this.zoneManager = new ZoneManager(seed);
        this.temperatureNoise = new SimplexNoise(seed + 7000);
        this.humidityNoise = new SimplexNoise(seed + 8000);
        this.continentalnessNoise = new SimplexNoise(seed + 9000);
        this.erosionNoise = new SimplexNoise(seed + 10000);
        this.weirdnessNoise = new SimplexNoise(seed + 11000);
        this.warpNoiseX = new SimplexNoise(seed + 12000);
        this.warpNoiseZ = new SimplexNoise(seed + 13000);
        this.ditherNoise = new SimplexNoise(seed + 16000);
    }

    public BiomeDefinition getBiome(int x, int z) {
        float[] p = getClimateParams(x, z);
        double dither = ditherNoise.noise(x * 0.1, z * 0.1) * 0.02;
        return getBiomeFromParams((float)(p[0]+dither), (float)(p[1]-dither), p[2], p[3], p[4], x, z);
    }

    public BiomeDefinition getBiomeFromParams(float temp, float hum, float cont, float eros, float weird, int x, int z) {
        Collection<BiomeDefinition> biomes = zoneManager.getAllowedBiomes(x, z);
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
                
                // Все параметры важны для правильного маппинга ландшафта
                double dist = (dt * dt) + (dh * dh) + (dc * dc) + (de * de) + (dw * dw) + cp.offset;
                
                if (dist < minDistance) {
                    minDistance = dist;
                    bestBiome = b;
                }
            }
        }
        return bestBiome;
    }

    private float normalizeNoise(double noise) {
        // Умножаем на 1.5, чтобы шум чаще достигал краев (0.0 и 1.0), 
        // иначе по гауссовому распределению он всегда сидит около 0.5 (Plains)
        double amplified = noise * 1.5;
        return (float) Math.max(0.0, Math.min(1.0, (amplified + 1.0) / 2.0));
    }

    public float[] getClimateParams(int x, int z) {
        double scale = 0.002;
        double warpScale = 0.005;
        double warpAmp = 20.0;
        
        double dx = warpNoiseX.octaveNoise(x * warpScale, z * warpScale, 3, 0.5, 1.0) * warpAmp;
        double dz = warpNoiseZ.octaveNoise(x * warpScale + 100, z * warpScale + 100, 3, 0.5, 1.0) * warpAmp;

        double sampleX = (x + dx) * scale;
        double sampleZ = (z + dz) * scale;

        return new float[] {
            normalizeNoise(temperatureNoise.octaveNoise(sampleX, sampleZ, 4, 0.5, 1.0)),
            normalizeNoise(humidityNoise.octaveNoise(sampleX + 1000, sampleZ + 1000, 4, 0.5, 1.0)),
            normalizeNoise(continentalnessNoise.octaveNoise(sampleX * 0.4 + 2000, sampleZ * 0.4 + 2000, 5, 0.5, 1.0)),
            normalizeNoise(erosionNoise.octaveNoise(sampleX * 0.6 + 3000, sampleZ * 0.6 + 3000, 4, 0.5, 1.0)),
            normalizeNoise(weirdnessNoise.octaveNoise(sampleX * 2.0 + 4000, sampleZ * 2.0 + 4000, 5, 0.5, 1.0) * 0.8)
        };
    }
}
