package com.za.minecraft.world.generation;

import com.za.minecraft.world.blocks.BlockType;

public class BiomeGenerator {
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new SimplexNoise(seed + 7000);
        this.humidityNoise = new SimplexNoise(seed + 8000);
    }
    
    public Biome getBiome(int x, int z) {
        double scale = 0.001;
        
        double temperature = temperatureNoise.noise(x * scale, z * scale);
        double humidity = humidityNoise.noise(x * scale + 1000, z * scale + 1000);
        
        if (temperature > 0.3) {
            return Biome.DESERT;
        } else if (temperature > 0) {
            return Biome.PLAINS;
        } else if (humidity > 0) {
            return Biome.FOREST;
        } else {
            return Biome.PLAINS;
        }
    }
    
    public enum Biome {
        PLAINS(64, 85, BlockType.GRASS, 0.01),
        FOREST(66, 88, BlockType.GRASS, 0.15),
        DESERT(62, 75, BlockType.SAND, 0.001),
        TAIGA(64, 90, BlockType.GRASS, 0.10),
        JUNGLE(65, 92, BlockType.GRASS, 0.25),
        TUNDRA(63, 80, BlockType.GRASS, 0.005),
        SWAMP(60, 65, BlockType.GRASS, 0.08);
        
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
