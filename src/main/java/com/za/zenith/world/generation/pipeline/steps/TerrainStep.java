package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.generation.BiomeDefinition;
import com.za.zenith.world.generation.BiomeGenerator;
import com.za.zenith.world.generation.BiomeRegistry;
import com.za.zenith.world.generation.density.DensityContextImpl;
import com.za.zenith.world.generation.density.NoiseRouter;
import com.za.zenith.world.generation.pipeline.GenerationStep;

public class TerrainStep implements GenerationStep {
    private final NoiseRouter noiseRouter;
    private final BiomeGenerator biomeGenerator;
    
    private static final int HORIZ_STEP = 4;
    private static final int VERT_STEP = 4;
    private static final int GRID_X = Chunk.CHUNK_SIZE / HORIZ_STEP + 1;
    private static final int GRID_Z = Chunk.CHUNK_SIZE / HORIZ_STEP + 1;
    private static final int GRID_Y = Chunk.CHUNK_HEIGHT / VERT_STEP + 1;

    public TerrainStep(long seed) {
        this.biomeGenerator = new BiomeGenerator(seed);
        this.noiseRouter = new NoiseRouter(seed, biomeGenerator);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
        int startX = chunk.getPosition().x() * Chunk.CHUNK_SIZE;
        int startZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE;

        // 1. Сэмплирование графа в сетку с использованием контекста
        double[][][] densityGrid = new double[GRID_X][GRID_Z][GRID_Y];
        for (int i = 0; i < GRID_X; i++) {
            for (int j = 0; j < GRID_Z; j++) {
                int worldX = startX + i * HORIZ_STEP;
                int worldZ = startZ + j * HORIZ_STEP;
                
                // Получаем климат один раз для колонны
                float[] climate = biomeGenerator.getClimateParams(worldX, worldZ);
                
                for (int k = 0; k < GRID_Y; k++) {
                    int worldY = k * VERT_STEP;
                    DensityContextImpl ctx = new DensityContextImpl(
                        worldX, worldY, worldZ,
                        climate[2], climate[3], climate[4], climate[0], climate[1]
                    );
                    densityGrid[i][j][k] = noiseRouter.getDensity(ctx);
                }
            }
        }

        // 2. Интерполяция и генерация блоков
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                
                BiomeDefinition biome = biomeGenerator.getBiome(worldX, worldZ);
                if (biome == null) biome = BiomeRegistry.getAll().iterator().next();

                int surfaceId = BlockRegistry.getRegistry().getId(biome.getSurfaceBlock());
                int undergroundId = BlockRegistry.getRegistry().getId(biome.getUndergroundBlock());
                float[] climate = biomeGenerator.getClimateParams(worldX, worldZ);
                float noiseVal = climate[0]; // just a dummy noise for rules

                int currentSurfaceY = -1;

                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    double density = sampleInterpolated(densityGrid, x, y, z);

                    if (density > 0.0) {
                        if (currentSurfaceY == -1) currentSurfaceY = y;
                        int depth = currentSurfaceY - y;

                        if (y == 0) {
                            chunk.setBlock(x, y, z, new Block(Blocks.BEDROCK.getId()));
                        } else {
                            boolean ruleMatched = false;
                            if (biome.getSurfaceRules() != null && !biome.getSurfaceRules().isEmpty()) {
                                for (com.za.zenith.world.generation.rules.SurfaceRule rule : biome.getSurfaceRules()) {
                                    if (rule.evaluate(worldX, y, worldZ, noiseVal, depth)) {
                                        BlockDefinition block = rule.getBlock();
                                        if (block != null) {
                                            chunk.setBlock(x, y, z, new Block(block.getId()));
                                            ruleMatched = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            if (!ruleMatched) {
                                if (y == currentSurfaceY) {
                                    chunk.setBlock(x, y, z, new Block(surfaceId));
                                } else if (y > currentSurfaceY - 4) {
                                    chunk.setBlock(x, y, z, new Block(undergroundId));
                                } else {
                                    chunk.setBlock(x, y, z, new Block(Blocks.STONE.getId()));
                                }
                            }
                        }
                    } else {
                        currentSurfaceY = -1;
                        if (y < 62) {
                            chunk.setBlock(x, y, z, new Block(Blocks.WATER.getId()));
                        }
                    }
                }
            }
        }
    }

    private double sampleInterpolated(double[][][] grid, int x, int y, int z) {
        int gx = x / HORIZ_STEP; int gz = z / HORIZ_STEP; int gy = y / VERT_STEP;
        double tx = (x % HORIZ_STEP) / (double) HORIZ_STEP;
        double tz = (z % HORIZ_STEP) / (double) HORIZ_STEP;
        double ty = (y % VERT_STEP) / (double) VERT_STEP;

        double d000 = grid[gx][gz][gy];
        double d100 = grid[gx+1][gz][gy];
        double d010 = grid[gx][gz+1][gy];
        double d110 = grid[gx+1][gz+1][gy];
        double d001 = grid[gx][gz][gy+1];
        double d101 = grid[gx+1][gz][gy+1];
        double d011 = grid[gx][gz+1][gy+1];
        double d111 = grid[gx+1][gz+1][gy+1];

        double dx00 = d000 * (1 - tx) + d100 * tx;
        double dx10 = d010 * (1 - tx) + d110 * tx;
        double dx01 = d001 * (1 - tx) + d101 * tx;
        double dx11 = d011 * (1 - tx) + d111 * tx;
        double dxy0 = dx00 * (1 - tz) + dx10 * tz;
        double dxy1 = dx01 * (1 - tz) + dx11 * tz;
        return dxy0 * (1 - ty) + dxy1 * ty;
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
    }
}
