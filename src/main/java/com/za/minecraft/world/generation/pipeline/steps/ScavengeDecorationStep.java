package com.za.minecraft.world.generation.pipeline.steps;

import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.generation.ScavengeSettings;
import com.za.minecraft.world.generation.pipeline.GenerationStep;
import com.za.minecraft.entities.ResourceEntity;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.ItemRegistry;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Шаг генерации, рассыпающий 3D ресурсы (сущности) по поверхности мира.
 */
public class ScavengeDecorationStep implements GenerationStep {
    private final Random random;

    public ScavengeDecorationStep(long seed) {
        this.random = new Random(seed + 500);
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
                if (surfaceY != -1) {
                    Block ground = world.getBlock(worldX, surfaceY, worldZ);
                    BlockDefinition groundDef = BlockRegistry.getBlock(ground.getType());
                    
                    if (groundDef.canSupportScavenge()) {
                        float roll = random.nextFloat();
                        
                        for (ScavengeSettings.Entry entry : ScavengeSettings.getEntries()) {
                            if (roll < entry.chance()) {
                                // Создаем 3D сущность предмета вместо блока
                                var item = ItemRegistry.getItem(entry.blockId()); // В настройках теперь ID предметов
                                if (item != null) {
                                    Vector3f pos = new Vector3f(worldX + 0.5f, surfaceY + 1.05f, worldZ + 0.5f);
                                    float rot = random.nextFloat() * (float)Math.PI * 2;
                                    world.spawnEntity(new ResourceEntity(pos, new ItemStack(item), rot));
                                }
                                break;
                            }
                            roll -= entry.chance();
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(World world, int x, int z) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y > 0; y--) {
            Block b = world.getBlock(x, y, z);
            if (!b.isAir()) {
                BlockDefinition def = BlockRegistry.getBlock(b.getType());
                if (def.isSolid()) {
                    return y;
                }
            }
        }
        return -1;
    }
}
