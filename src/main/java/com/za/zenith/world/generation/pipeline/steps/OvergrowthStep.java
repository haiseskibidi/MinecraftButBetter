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
    private final SimplexNoise groveNoise; // Для формирования групп деревьев
    private final SimplexNoise flowerNoise; 
    private final Random random;
    private final BiomeGenerator biomeGenerator;

    public OvergrowthStep(long seed) {
        this.groveNoise = new SimplexNoise(seed + 300);
        this.flowerNoise = new SimplexNoise(seed + 600);
        this.random = new Random(seed);
        this.biomeGenerator = new BiomeGenerator(seed);
    }

    @Override
    public void generateTerrain(Chunk chunk) {
    }

    @Override
    public void generateStructures(World world, Chunk chunk) {
        int startX = chunk.getPosition().x() * Chunk.CHUNK_SIZE;
        int startZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE;

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                int surfaceY = findSurfaceY(chunk, x, z);
                if (surfaceY <= 0) continue;

                BiomeDefinition biome = biomeGenerator.getBiome(worldX, worldZ);
                if (biome == null) continue;

                // 1. Зонирование (Где растут деревья, а где цветы)
                double grove = (groveNoise.octaveNoise(worldX, worldZ, 2, 0.5, 0.01) + 1.0) / 2.0;
                double flowers = (flowerNoise.octaveNoise(worldX, worldZ, 2, 0.5, 0.05) + 1.0) / 2.0;

                Block ground = chunk.getBlock(x, surfaceY, z);
                boolean isGrass = ground.getType() == Blocks.GRASS_BLOCK.getId();

                if (isGrass) {
                    // 2. Цветочные поляны и подлесок
                    if (grove < 0.4) {
                        if (flowers > 0.6 && random.nextFloat() < 0.2f) {
                            world.setBlockDuringGen(worldX, surfaceY + 1, worldZ, new Block(Blocks.SHORT_GRASS.getId()));
                        }
                    }

                    // 3. Кластерная генерация деревьев (Grove System)
                    // Чем выше grove, тем гуще лес. Если grove < 0.4 - это поляна.
                    float treeDensity = (float) (biome.getTreeDensity() * (grove > 0.4 ? 4.0 : 0.0));
                    
                    if (random.nextFloat() < treeDensity) {
                        // Плотность внутри рощи
                        if (random.nextFloat() < 0.3f) { // Урежаем, чтобы не было "каши"
                            if (isSpaceClear(world, worldX, surfaceY + 1, worldZ)) {
                                List<BiomeDefinition.FeatureEntry> features = biome.getFeatures();
                                if (features != null && !features.isEmpty()) {
                                    Identifier selected = selectWeightedFeature(features);
                                    if (selected != null) {
                                        StructureTemplate template = StructureRegistry.get(selected);
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
    }

    private boolean isSpaceClear(World world, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (world.getBlock(x + dx, y, z + dz).getType() != Blocks.AIR.getId()) return false;
            }
        }
        return true;
    }

    private Identifier selectWeightedFeature(List<BiomeDefinition.FeatureEntry> features) {
        int totalWeight = features.stream().mapToInt(BiomeDefinition.FeatureEntry::getWeight).sum();
        if (totalWeight <= 0) return null;
        int roll = random.nextInt(totalWeight);
        for (BiomeDefinition.FeatureEntry f : features) {
            roll -= f.getWeight();
            if (roll < 0) return f.getId();
        }
        return null;
    }

    private int findSurfaceY(Chunk chunk, int localX, int localZ) {
        // Поддержка островов: ищем поверхность сверху вниз
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 40; y--) {
            Block b = chunk.getBlock(localX, y, localZ);
            if (!b.isAir() && !b.isTransparent()) return y;
        }
        return -1;
    }
}
