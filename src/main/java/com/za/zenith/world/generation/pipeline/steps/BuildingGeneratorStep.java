package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.chunks.Chunk;

import com.za.zenith.world.generation.pipeline.GenerationStep;
import com.za.zenith.world.generation.structures.PrefabManager;

import java.util.Random;

public class BuildingGeneratorStep implements GenerationStep {
    private final Random random;
    private static final int CITY_LEVEL = 60;

    public BuildingGeneratorStep(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();
        
        long chunkSeed = (long)chunkX * 341873128712L + (long)chunkZ * 132897987541L;
        random.setSeed(chunkSeed);
        
        int bX = chunkX * Chunk.CHUNK_SIZE + 4 + random.nextInt(8);
        int bZ = chunkZ * Chunk.CHUNK_SIZE + 4 + random.nextInt(8);
        
        boolean isRoadX = Math.abs(bX % 30) < 4;
        boolean isRoadZ = Math.abs(bZ % 30) < 4;
        
        if (!isRoadX && !isRoadZ) {
            if (random.nextFloat() > 0.4f) {
                int buildingType = random.nextInt(4); // Увеличили пул вариантов
                if (buildingType == 0) {
                    PrefabManager.SMALL_STORE.build(world, bX, CITY_LEVEL, bZ);
                } else if (buildingType == 1) {
                    PrefabManager.RUINED_HOUSE_1.build(world, bX, CITY_LEVEL, bZ);
                } else if (buildingType == 2) {
                    PrefabManager.APARTMENT_BUILDING.build(world, bX, CITY_LEVEL, bZ);
                } else {
                    buildPrefabSkyscraper(world, bX, CITY_LEVEL, bZ);
                }
            }
        }
    }
    
    private void buildPrefabSkyscraper(World world, int x, int startY, int z) {
        int floors = 3 + random.nextInt(6); // 3-8 этажей
        int currentY = startY;
        
        for (int i = 0; i < floors; i++) {
            PrefabManager.SKYSCRAPER_FLOOR.build(world, x, currentY, z);
            currentY += PrefabManager.SKYSCRAPER_FLOOR.getHeight();
        }
        
        PrefabManager.SKYSCRAPER_ROOF.build(world, x, currentY, z);
    }
}


