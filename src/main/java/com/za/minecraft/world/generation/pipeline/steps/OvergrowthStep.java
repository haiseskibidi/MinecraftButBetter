package com.za.minecraft.world.generation.pipeline.steps;

import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.blocks.WoodTypeRegistry;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.generation.SimplexNoise;
import com.za.minecraft.world.generation.pipeline.GenerationStep;

import java.util.Random;

public class OvergrowthStep implements GenerationStep {
    private final SimplexNoise vegetationNoise;
    private final Random random;
    private static final int CITY_LEVEL = 60;

    public OvergrowthStep(long seed) {
        this.vegetationNoise = new SimplexNoise(seed + 300);
        this.random = new Random(seed);
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
                
                if (surfaceY >= CITY_LEVEL) {
                    Block groundBlock = world.getBlock(worldX, surfaceY, worldZ);
                    double vNoise = (vegetationNoise.noise(worldX * 0.05, worldZ * 0.05) + 1.0) / 2.0;

                    if (vNoise > 0.6) {
                        if (groundBlock.getType() == Blocks.ASPHALT.getId() || groundBlock.getType() == Blocks.COBBLESTONE.getId()) {
                            if (random.nextFloat() < 0.3f) {
                                world.setBlock(worldX, surfaceY, worldZ, new Block(Blocks.GRASS_BLOCK.getId()));
                                world.setBlock(worldX, surfaceY - 1, worldZ, new Block(Blocks.DIRT.getId()));
                            }
                        }
                        else if (groundBlock.getType() == Blocks.RUSTY_METAL.getId()) {
                            if (random.nextFloat() < 0.15f) {
                                // Random leaf type
                                int woodIdx = random.nextInt(WoodTypeRegistry.size());
                                com.za.minecraft.utils.Identifier logId = WoodTypeRegistry.getLogId(woodIdx);
                                com.za.minecraft.utils.Identifier leafId = com.za.minecraft.utils.Identifier.of(logId.getNamespace(), logId.getPath().replace("_log", "_leaves"));
                                BlockDefinition leafDef = BlockRegistry.getRegistry().get(leafId);
                                if (leafDef != null) {
                                    world.setBlock(worldX, surfaceY + 1, worldZ, new Block(leafDef.getId()));
                                } else {
                                    world.setBlock(worldX, surfaceY + 1, worldZ, new Block(Blocks.OAK_LEAVES.getId()));
                                }
                            }
                        }

                        if (groundBlock.getType() == Blocks.DIRT.getId() || groundBlock.getType() == Blocks.GRASS_BLOCK.getId()) {
                            if (random.nextFloat() < 0.05f) {
                                generateMutatedTree(world, worldX, surfaceY + 1, worldZ);
                            } else if (random.nextFloat() < 0.4f) {
                                if (random.nextFloat() < 0.2f) {
                                    // Высокая трава (DOUBLE_PLANT) - ставим низ и верх
                                    if (world.getBlock(worldX, surfaceY + 2, worldZ).isAir()) {
                                        world.setBlock(worldX, surfaceY + 1, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)0));
                                        world.setBlock(worldX, surfaceY + 2, worldZ, new Block(Blocks.TALL_GRASS.getId(), (byte)1));
                                    }
                                } else {
                                    world.setBlock(worldX, surfaceY + 1, worldZ, new Block(Blocks.SHORT_GRASS.getId()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(World world, int x, int z) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= CITY_LEVEL; y--) {
            Block b = world.getBlock(x, y, z);
            if (!b.isAir() && !b.isTransparent()) {
                return y;
            }
        }
        return -1;
    }

    private void generateMutatedTree(World world, int x, int y, int z) {
        int woodIdx = random.nextInt(com.za.minecraft.world.blocks.WoodTypeRegistry.size());
        com.za.minecraft.utils.Identifier logId = com.za.minecraft.world.blocks.WoodTypeRegistry.getLogId(woodIdx);
        com.za.minecraft.utils.Identifier leafId = com.za.minecraft.utils.Identifier.of(logId.getNamespace(), logId.getPath().replace("_log", "_leaves"));
        
        com.za.minecraft.world.blocks.BlockDefinition logDef = com.za.minecraft.world.blocks.BlockRegistry.getRegistry().get(logId);
        com.za.minecraft.world.blocks.BlockDefinition leafDef = com.za.minecraft.world.blocks.BlockRegistry.getRegistry().get(leafId);
        
        int logIdVal = logDef != null ? logDef.getId() : Blocks.OAK_LOG.getId();
        int leafIdVal = leafDef != null ? leafDef.getId() : Blocks.OAK_LEAVES.getId();

        int height = 3 + random.nextInt(4);
        // Trunk
        for (int dy = 0; dy < height; dy++) {
            world.setBlock(x, y + dy, z, new Block(logIdVal, Block.BIT_NATURAL));
        }

        // Leaves
        int crownY = y + height - 2;
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 3) {
                        if (random.nextFloat() > 0.3f) {
                            com.za.minecraft.world.BlockPos pos = new com.za.minecraft.world.BlockPos(x + dx, crownY + dy, z + dz);
                            Block existing = world.getBlock(pos);
                            if (existing.isAir()) {
                                world.setBlock(pos, new Block(leafIdVal, Block.BIT_NATURAL));
                            }
                        }
                    }
                }
            }
        }
    }
}
