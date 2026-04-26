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

        int startX = chunkX * Chunk.CHUNK_SIZE;
        int startZ = chunkZ * Chunk.CHUNK_SIZE;

        // Sample blended biome data at the 4 corners of the chunk
        BiomeGenerator.BlendedBiome b00 = biomeGenerator.getBlendedBiome(startX, startZ, 16);
        BiomeGenerator.BlendedBiome b10 = biomeGenerator.getBlendedBiome(startX + 16, startZ, 16);
        BiomeGenerator.BlendedBiome b01 = biomeGenerator.getBlendedBiome(startX, startZ + 16, 16);
        BiomeGenerator.BlendedBiome b11 = biomeGenerator.getBlendedBiome(startX + 16, startZ + 16, 16);

        // Precompute FBM noises at the 4 corners
        double c00 = (continentalnessNoise.octaveNoise(startX, startZ, 4, 0.5, 0.005) + 1.0) / 2.0;
        double c10 = (continentalnessNoise.octaveNoise(startX + 16, startZ, 4, 0.5, 0.005) + 1.0) / 2.0;
        double c01 = (continentalnessNoise.octaveNoise(startX, startZ + 16, 4, 0.5, 0.005) + 1.0) / 2.0;
        double c11 = (continentalnessNoise.octaveNoise(startX + 16, startZ + 16, 4, 0.5, 0.005) + 1.0) / 2.0;

        double e00 = (erosionNoise.octaveNoise(startX, startZ, 3, 0.5, 0.01) + 1.0) / 2.0;
        double e10 = (erosionNoise.octaveNoise(startX + 16, startZ, 3, 0.5, 0.01) + 1.0) / 2.0;
        double e01 = (erosionNoise.octaveNoise(startX, startZ + 16, 3, 0.5, 0.01) + 1.0) / 2.0;
        double e11 = (erosionNoise.octaveNoise(startX + 16, startZ + 16, 3, 0.5, 0.01) + 1.0) / 2.0;

        double p00 = (peaksNoise.octaveNoise(startX, startZ, 5, 0.6, 0.02) + 1.0) / 2.0;
        double p10 = (peaksNoise.octaveNoise(startX + 16, startZ, 5, 0.6, 0.02) + 1.0) / 2.0;
        double p01 = (peaksNoise.octaveNoise(startX, startZ + 16, 5, 0.6, 0.02) + 1.0) / 2.0;
        double p11 = (peaksNoise.octaveNoise(startX + 16, startZ + 16, 5, 0.6, 0.02) + 1.0) / 2.0;

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Bilinear interpolation weights
                double tx = x / 16.0;
                double tz = z / 16.0;

                double baseHeight = b00.baseHeight * (1 - tx) * (1 - tz) + 
                                    b10.baseHeight * tx * (1 - tz) + 
                                    b01.baseHeight * (1 - tx) * tz + 
                                    b11.baseHeight * tx * tz;

                double erosionFactor = b00.erosionFactor * (1 - tx) * (1 - tz) + 
                                       b10.erosionFactor * tx * (1 - tz) + 
                                       b01.erosionFactor * (1 - tx) * tz + 
                                       b11.erosionFactor * tx * tz;

                double heightVariation = b00.heightVariation * (1 - tx) * (1 - tz) + 
                                         b10.heightVariation * tx * (1 - tz) + 
                                         b01.heightVariation * (1 - tx) * tz + 
                                         b11.heightVariation * tx * tz;

                BiomeDefinition exactBiome = biomeGenerator.getBiome(worldX, worldZ);
                if (exactBiome == null) continue;

                // Interpolated FBM values
                double continentalness = c00 * (1 - tx) * (1 - tz) + c10 * tx * (1 - tz) + c01 * (1 - tx) * tz + c11 * tx * tz;
                double erosion = e00 * (1 - tx) * (1 - tz) + e10 * tx * (1 - tz) + e01 * (1 - tx) * tz + e11 * tx * tz;
                double peaks = p00 * (1 - tx) * (1 - tz) + p10 * tx * (1 - tz) + p01 * (1 - tx) * tz + p11 * tx * tz;

                // Complex terrain calculation using interpolated values
                double baseTerrain = (continentalness * 30.0) - (erosion * 15.0 * erosionFactor);
                double peaksOffset = peaks * heightVariation * (1.0 - erosion); 
                
                int surfaceY = (int)baseHeight + (int) baseTerrain + (int) peaksOffset;
                
                if (surfaceY < MIN_HEIGHT) surfaceY = MIN_HEIGHT;
                if (surfaceY >= Chunk.CHUNK_HEIGHT - 10) surfaceY = Chunk.CHUNK_HEIGHT - 10;

                int surfaceBlockId = BlockRegistry.getRegistry().getId(exactBiome.getSurfaceBlock());
                int undergroundBlockId = BlockRegistry.getRegistry().getId(exactBiome.getUndergroundBlock());

                if (surfaceBlockId == -1) surfaceBlockId = Blocks.GRASS_BLOCK.getId();
                if (undergroundBlockId == -1) undergroundBlockId = Blocks.DIRT.getId();

                for (int y = 0; y <= surfaceY; y++) {
                    if (chunk.getBlockType(x, y, z) != 0) continue; // Skip if already has a block (e.g. from neighbor structure)

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
