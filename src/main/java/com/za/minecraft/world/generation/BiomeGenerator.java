package com.za.minecraft.world.generation;

import com.za.minecraft.world.blocks.BlockType;

public class BiomeGenerator {
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new NoiseGenerator(seed + 2000);
        this.humidityNoise = new NoiseGenerator(seed + 3000);
    }
    
    public Biome getBiome(int x, int z) {
        // Larger scale for more visible biome regions
        double temperature = temperatureNoise.octaveNoise(x * 0.002, z * 0.002, 2, 0.6, 1.0);
        double humidity = humidityNoise.octaveNoise(x * 0.003, z * 0.003, 2, 0.6, 1.0);
        
        // Normalize to 0-1 range
        temperature = (temperature + 1) / 2;
        humidity = (humidity + 1) / 2;
        
        // Determine biome based on temperature and humidity
        if (temperature < 0.2) {
            return Biome.TUNDRA;
        } else if (temperature < 0.5) {
            if (humidity < 0.3) {
                return Biome.TAIGA;
            } else {
                return Biome.FOREST;
            }
        } else if (temperature < 0.8) {
            if (humidity < 0.3) {
                return Biome.DESERT;
            } else if (humidity < 0.7) {
                return Biome.PLAINS;
            } else {
                return Biome.FOREST;
            }
        } else {
            if (humidity < 0.5) {
                return Biome.DESERT;
            } else {
                return Biome.JUNGLE;
            }
        }
    }
    
    public enum Biome {
        PLAINS(65, 75, BlockType.GRASS, 0.03),
        FOREST(70, 85, BlockType.GRASS, 0.12),
        DESERT(60, 70, BlockType.SAND, 0.001),
        TAIGA(68, 80, BlockType.GRASS, 0.08),
        JUNGLE(72, 90, BlockType.GRASS, 0.20),
        TUNDRA(62, 72, BlockType.GRASS, 0.005);
        
        private final int baseHeight;
        private final int maxHeight;
        private final BlockType surfaceBlock;
        private final double treeDensity;
        
        Biome(int baseHeight, int maxHeight, BlockType surfaceBlock, double treeDensity) {
            this.baseHeight = baseHeight;
            this.maxHeight = maxHeight;
            this.surfaceBlock = surfaceBlock;
            this.treeDensity = treeDensity;
        }
        
        public int getBaseHeight() { return baseHeight; }
        public int getMaxHeight() { return maxHeight; }
        public BlockType getSurfaceBlock() { return surfaceBlock; }
        public double getTreeDensity() { return treeDensity; }
    }
}
