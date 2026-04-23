package com.za.zenith.world.generation.pipeline.steps;

import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.generation.ScavengeSettings;
import com.za.zenith.world.generation.pipeline.GenerationStep;
import com.za.zenith.entities.ResourceEntity;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.ItemRegistry;
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

                float surfaceY = findSurfaceY(chunk, x, z);
                if (surfaceY > 0) {
                    Block ground = chunk.getBlock(x, (int)Math.floor(surfaceY - 0.01f), z);
                    BlockDefinition groundDef = BlockRegistry.getBlock(ground.getType());
                    
                    if (groundDef.canSupportScavenge()) {
                        float roll = random.nextFloat();
                        
                        for (ScavengeSettings.Entry entry : ScavengeSettings.getEntries()) {
                            if (roll < entry.chance()) {
                                // Создаем 3D сущность предмета вместо блока
                                var item = ItemRegistry.getItem(entry.blockId()); 
                                if (item != null) {
                                    Vector3f pos = new Vector3f(worldX + 0.5f, surfaceY, worldZ + 0.5f);
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

    private float findSurfaceY(Chunk chunk, int localX, int localZ) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y > 0; y--) {
            Block b = chunk.getBlock(localX, y, localZ);
            if (!b.isAir()) {
                BlockDefinition def = BlockRegistry.getBlock(b.getType());
                if (def.isSolid()) {
                    float topY = (float)y;
                    com.za.zenith.world.physics.VoxelShape shape = def.getShape(b.getMetadata());
                    if (shape != null && !shape.getBoxes().isEmpty()) {
                        for (com.za.zenith.world.physics.AABB box : shape.getBoxes()) {
                            topY = Math.max(topY, y + box.maxY());
                        }
                    } else {
                        topY += 1.0f;
                    }
                    return topY;
                }
            }
        }
        return -1;
    }
}


