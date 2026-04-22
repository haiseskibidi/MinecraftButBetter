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

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;

                int surfaceY = findSurfaceY(world, worldX, worldZ);
                if (surfaceY > 0) {
                    BiomeDefinition biome = biomeGenerator.getBiome(worldX, worldZ);
                    if (biome == null) continue;

                    double vNoise = (vegetationNoise.octaveNoise(worldX, worldZ, 2, 0.5, 0.05) + 1.0) / 2.0;

                    if (vNoise > 0.4) {
                        Block groundBlock = world.getBlock(worldX, surfaceY, worldZ);

                        // Flowers, grass, etc.
                        if (groundBlock.getType() == Blocks.DIRT.getId() || groundBlock.getType() == Blocks.GRASS_BLOCK.getId()) {
                            if (random.nextFloat() < 0.3f) {
                                if (random.nextFloat() < 0.1f) {
                                    if (world.getBlock(worldX, surfaceY + 2, worldZ).isAir()) {
                                        world.setBlock(worldX, surfaceY + 1, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)0));
                                        world.setBlock(worldX, surfaceY + 2, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)1));
                                    }
                                } else {
                                    world.setBlock(worldX, surfaceY + 1, worldZ, new Block(Blocks.SHORT_GRASS.getId()));
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

    private int findSurfaceY(World world, int x, int z) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 20; y--) {
            Block b = world.getBlock(x, y, z);
            if (!b.isAir() && !b.isTransparent() && b.getType() != Blocks.SHORT_GRASS.getId() && b.getType() != Blocks.TALL_GRASS.getId()) {
                return y;
            }
        }
        return -1;
    }
}


