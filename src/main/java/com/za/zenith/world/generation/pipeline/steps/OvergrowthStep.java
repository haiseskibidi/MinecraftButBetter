package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.generation.BiomeDefinition;
import com.za.zenith.world.generation.BiomeGenerator;
import com.za.zenith.world.generation.SimplexNoise;
import com.za.zenith.world.generation.pipeline.GenerationStep;
import com.za.zenith.world.generation.structures.StructureRegistry;
import com.za.zenith.world.generation.structures.StructureTemplate;

import java.util.List;
import java.util.Random;

public class OvergrowthStep implements GenerationStep {
    private final SimplexNoise vegetationNoise;
    private final Random random;
    private final BiomeGenerator biomeGenerator;

    public OvergrowthStep(long seed) {
        this.vegetationNoise = new SimplexNoise(seed + 300);
        this.random = new Random(seed);
        this.biomeGenerator = new BiomeGenerator(seed);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();
        
        int startX = chunkX * Chunk.CHUNK_SIZE;
        int startZ = chunkZ * Chunk.CHUNK_SIZE;

        // Precompute FBM noise at the 4 corners of the chunk
        double v00 = (vegetationNoise.octaveNoise(startX, startZ, 2, 0.5, 0.05) + 1.0) / 2.0;
        double v10 = (vegetationNoise.octaveNoise(startX + 16, startZ, 2, 0.5, 0.05) + 1.0) / 2.0;
        double v01 = (vegetationNoise.octaveNoise(startX, startZ + 16, 2, 0.5, 0.05) + 1.0) / 2.0;
        double v11 = (vegetationNoise.octaveNoise(startX + 16, startZ + 16, 2, 0.5, 0.05) + 1.0) / 2.0;

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                int surfaceY = findSurfaceY(chunk, x, z);
                if (surfaceY > 0) {
                    BiomeDefinition biome = biomeGenerator.getBiome(worldX, worldZ);
                    if (biome == null) continue;

                    double tx = x / 16.0;
                    double tz = z / 16.0;
                    double vNoise = v00 * (1 - tx) * (1 - tz) + v10 * tx * (1 - tz) + v01 * (1 - tx) * tz + v11 * tx * tz;

                    if (vNoise > 0.4) {
                        Block groundBlock = chunk.getBlock(x, surfaceY, z);

                        // Flowers, grass, etc.
                        if (groundBlock.getType() == Blocks.DIRT.getId() || groundBlock.getType() == Blocks.GRASS_BLOCK.getId()) {
                            if (random.nextFloat() < 0.3f) {
                                if (random.nextFloat() < 0.1f) {
                                    if (chunk.getBlock(x, surfaceY + 2, z).isAir()) {
                                        world.setBlockDuringGen(worldX, surfaceY + 1, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)0));
                                        world.setBlockDuringGen(worldX, surfaceY + 2, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)1));
                                    }
                                } else {
                                    world.setBlockDuringGen(worldX, surfaceY + 1, worldZ, new Block(Blocks.SHORT_GRASS.getId()));
                                }
                            }
                        }

                        // Generate Trees / Features based on Biome
                        if (random.nextFloat() < biome.getTreeDensity() * 0.05f) { // Adjusted density multiplier
                            List<BiomeDefinition.FeatureEntry> features = biome.getFeatures();
                            if (features != null && !features.isEmpty()) {
                                int totalWeight = features.stream().mapToInt(BiomeDefinition.FeatureEntry::getWeight).sum();
                                int roll = random.nextInt(totalWeight);
                                Identifier selectedFeature = null;
                                for (BiomeDefinition.FeatureEntry f : features) {
                                    roll -= f.getWeight();
                                    if (roll < 0) {
                                        selectedFeature = f.getId();
                                        break;
                                    }
                                }

                                if (selectedFeature != null) {
                                    StructureTemplate template = StructureRegistry.get(selectedFeature);
                                    if (template != null) {
                                        template.build(world, worldX - template.getWidth() / 2, surfaceY + 1, worldZ - template.getDepth() / 2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(Chunk chunk, int localX, int localZ) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 20; y--) {
            Block b = chunk.getBlock(localX, y, localZ);
            if (!b.isAir() && !b.isTransparent() && b.getType() != Blocks.SHORT_GRASS.getId() && b.getType() != Blocks.TALL_GRASS.getId()) {
                return y;
            }
        }
        return -1;
    }
}


