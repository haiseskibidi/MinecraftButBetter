package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.world.generation.pipeline.GenerationStep;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.generation.SimplexNoise;

public class CityLayoutStep implements GenerationStep {
    private final SimplexNoise cityNoise;
    private final SimplexNoise roadNoise;
    
    private static final int CITY_LEVEL = 60; // Высота земли
    private static final int BASEMENT_LEVEL = 40; // Фундамент/Подземка
    
    public CityLayoutStep(long seed) {
        this.cityNoise = new SimplexNoise(seed + 100);
        this.roadNoise = new SimplexNoise(seed + 200);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                // Простая сетка улиц (Манхэттенское расстояние)
                boolean isRoadX = Math.abs(worldX % 30) < 4;
                boolean isRoadZ = Math.abs(worldZ % 30) < 4;
                boolean isRoad = isRoadX || isRoadZ;
                
                // Шум для разрушений дороги
                double destruction = roadNoise.noise(worldX * 0.1, worldZ * 0.1);
                
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.BEDROCK.getId()));
                    } else if (y < BASEMENT_LEVEL) {
                        chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.STONE.getId()));
                    } else if (y < CITY_LEVEL) {
                        // Фундамент зданий
                        chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.DIRT.getId())); 
                    } else if (y == CITY_LEVEL) {
                        if (isRoad) {
                            if (destruction > 0.4) {
                                chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.DIRT.getId())); // Разбитый асфальт
                            } else {
                                chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.ASPHALT.getId()));
                            }
                        } else {
                            chunk.setBlock(x, y, z, new Block(com.za.zenith.world.blocks.Blocks.COBBLESTONE.getId())); // Основа зданий
                        }
                    }
                    // Воздух выше CITY_LEVEL ставится по умолчанию в Chunk
                }
            }
        }
    }
}
