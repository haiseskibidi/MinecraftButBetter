package com.za.zenith.world.generation;

public class BiomeGenerator {
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise detailNoise;
    
    public BiomeGenerator(long seed) {
        this.temperatureNoise = new SimplexNoise(seed + 7000);
        this.humidityNoise = new SimplexNoise(seed + 8000);
        this.detailNoise = new SimplexNoise(seed + 15000);
    }
    
    public Biome getBiome(int x, int z) {
        double scale = 0.0005;
        
        double fuzzX = detailNoise.noise(x * 0.01, z * 0.01) * 12.0;
        double fuzzZ = detailNoise.noise(x * 0.01 + 500, z * 0.01 + 500) * 12.0;
        
        double temp = (temperatureNoise.noise((x + fuzzX) * scale, (z + fuzzZ) * scale) + 1.0) / 2.0;
        double hum = (humidityNoise.noise((x + fuzzX) * scale + 1000, (z + fuzzZ) * scale + 1000) + 1.0) / 2.0;
        
        if (temp < 0.3) {
            if (hum < 0.3) return Biome.TUNDRA;
            return Biome.TAIGA;
        } else if (temp < 0.6) {
            if (hum < 0.3) return Biome.PLAINS;
            if (hum < 0.7) return Biome.FOREST;
            return Biome.SWAMP;
        } else {
            if (hum < 0.2) return Biome.DESERT;
            if (hum < 0.5) return Biome.SAVANNA;
            return Biome.JUNGLE;
        }
    }
    
    public enum Biome {
        PLAINS(64, 10, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.05, 1.2),
        FOREST(68, 20, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.40, 0.8),
        DESERT(62, 8, com.za.zenith.world.blocks.Blocks.SAND.getId(), 0.00, 1.5),
        TAIGA(72, 40, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.30, 0.6),
        JUNGLE(65, 30, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.70, 0.5),
        TUNDRA(63, 15, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.02, 1.0),
        SWAMP(60, 5, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.25, 1.8),
        SAVANNA(66, 12, com.za.zenith.world.blocks.Blocks.GRASS_BLOCK.getId(), 0.10, 1.3);
        
        private final int baseHeight;
        private final int heightVariation;
        private final int surfaceBlock;
        private final double treeDensity;
        private final double erosionFactor;

        Biome(int baseHeight, int heightVariation, int surfaceBlock, double treeDensity, double erosionFactor) {
            this.baseHeight = baseHeight;
            this.heightVariation = heightVariation;
            this.surfaceBlock = surfaceBlock;
            this.treeDensity = treeDensity;
            this.erosionFactor = erosionFactor;
        }

        public int getBaseHeight() { return baseHeight; }
        public int getHeightVariation() { return heightVariation; }
        public int getSurfaceBlock() { return surfaceBlock; }
        public double getTreeDensity() { return treeDensity; }
        public double getErosionFactor() { return erosionFactor; }
    }
}
