package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.generation.BiomeDefinition;
import com.za.zenith.world.generation.BiomeGenerator;
import com.za.zenith.world.generation.SimplexNoise;
import com.za.zenith.world.generation.pipeline.GenerationStep;

public class TerrainStep implements GenerationStep {
    private final SimplexNoise continentalnessNoise;
    private final SimplexNoise erosionNoise;
    private final SimplexNoise peaksNoise;
    private final BiomeGenerator biomeGenerator;
    
    private static final int MIN_HEIGHT = 20;

    public TerrainStep(long seed) {
        this.continentalnessNoise = new SimplexNoise(seed + 100);
        this.erosionNoise = new SimplexNoise(seed + 200);
        this.peaksNoise = new SimplexNoise(seed + 300);
        this.biomeGenerator = new BiomeGenerator(seed);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;

                BiomeDefinition biome = biomeGenerator.getBiome(worldX, worldZ);
                if (biome == null) continue;

                // FBM for more natural terrain
                double continentalness = (continentalnessNoise.octaveNoise(worldX, worldZ, 4, 0.5, 0.005) + 1.0) / 2.0;
                double erosion = (erosionNoise.octaveNoise(worldX, worldZ, 3, 0.5, 0.01) + 1.0) / 2.0;
                double peaks = (peaksNoise.octaveNoise(worldX, worldZ, 5, 0.6, 0.02) + 1.0) / 2.0;

                // Complex terrain calculation
                double baseTerrain = (continentalness * 30.0) - (erosion * 15.0 * biome.getErosionFactor());
                double peaksOffset = peaks * biome.getHeightVariation() * (1.0 - erosion); // Peaks only exist where erosion is low
                
                int surfaceY = biome.getBaseHeight() + (int) baseTerrain + (int) peaksOffset;
                
                if (surfaceY < MIN_HEIGHT) surfaceY = MIN_HEIGHT;
                if (surfaceY >= Chunk.CHUNK_HEIGHT - 10) surfaceY = Chunk.CHUNK_HEIGHT - 10;

                int surfaceBlockId = BlockRegistry.getRegistry().getId(biome.getSurfaceBlock());
                int undergroundBlockId = BlockRegistry.getRegistry().getId(biome.getUndergroundBlock());

                if (surfaceBlockId == -1) surfaceBlockId = Blocks.GRASS_BLOCK.getId();
                if (undergroundBlockId == -1) undergroundBlockId = Blocks.DIRT.getId();

                for (int y = 0; y <= surfaceY; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, new Block(Blocks.BEDROCK.getId()));
                    } else if (y < surfaceY - 4) {
                        chunk.setBlock(x, y, z, new Block(Blocks.STONE.getId()));
                    } else if (y < surfaceY) {
                        chunk.setBlock(x, y, z, new Block(undergroundBlockId));
                    } else {
                        chunk.setBlock(x, y, z, new Block(surfaceBlockId));
                    }
                }
            }
        }
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
    }
}
