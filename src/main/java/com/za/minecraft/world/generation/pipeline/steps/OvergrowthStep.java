package com.za.minecraft.world.generation.pipeline.steps;

import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
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
        // Мы добавляем растительность поверх существующего мира,
        // поэтому делаем это в Structure pass, чтобы видеть соседние блоки.
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;

                // Находим верхний твердый блок
                int surfaceY = findSurfaceY(world, worldX, worldZ);
                
                if (surfaceY >= CITY_LEVEL) {
                    Block groundBlock = world.getBlock(worldX, surfaceY, worldZ);
                    
                    // Шум для определения густоты зарослей
                    double vNoise = (vegetationNoise.noise(worldX * 0.05, worldZ * 0.05) + 1.0) / 2.0;

                    if (vNoise > 0.6) {
                        // Если это асфальт или булыжник (разбитая дорога/фундамент)
                        if (groundBlock.getType() == BlockType.ASPHALT || groundBlock.getType() == BlockType.COBBLESTONE) {
                            if (random.nextFloat() < 0.3f) {
                                // Заменяем сам асфальт на траву, чтобы она не "висела" сверху
                                world.setBlock(worldX, surfaceY, worldZ, BlockType.GRASS);
                                world.setBlock(worldX, surfaceY - 1, worldZ, BlockType.DIRT);
                            }
                        }
                        // Если это крыша здания (ржавый металл или стекло)
                        else if (groundBlock.getType() == BlockType.RUSTY_METAL) {
                            if (random.nextFloat() < 0.15f) {
                                // Мох на крышах
                                world.setBlock(worldX, surfaceY + 1, worldZ, BlockType.LEAVES);
                            }
                        }

                        // Спавн "деревьев" (мутировавших зарослей) на заросших участках
                        if (groundBlock.getType() == BlockType.DIRT || groundBlock.getType() == BlockType.GRASS) {
                            if (random.nextFloat() < 0.05f) {
                                generateMutatedTree(world, worldX, surfaceY + 1, worldZ);
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
            if (!b.isAir() && !b.isTransparent()) { // Игнорируем стекло и листья при поиске поверхности
                return y;
            }
        }
        return -1;
    }

    private void generateMutatedTree(World world, int x, int y, int z) {
        int height = 3 + random.nextInt(4);
        
        // Ствол
        for (int dy = 0; dy < height; dy++) {
            world.setBlock(x, y + dy, z, BlockType.WOOD);
        }
        
        // Крона (более хаотичная)
        int crownY = y + height - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 3) {
                        if (random.nextFloat() > 0.3f) {
                            Block existing = world.getBlock(x + dx, crownY + dy, z + dz);
                            if (existing.isAir()) {
                                world.setBlock(x + dx, crownY + dy, z + dz, BlockType.LEAVES);
                            }
                        }
                    }
                }
            }
        }
    }
}
