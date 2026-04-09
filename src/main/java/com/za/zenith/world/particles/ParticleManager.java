package com.za.zenith.world.particles;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Глобальный менеджер системы частиц.
 */
public class ParticleManager {
    private static ParticleManager instance;
    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> pendingAdd = new ArrayList<>();

    public static ParticleManager getInstance() {
        if (instance == null) instance = new ParticleManager();
        return instance;
    }

    public void update(float deltaTime, World world) {
        // Добавление новых частиц
        if (!pendingAdd.isEmpty()) {
            particles.addAll(pendingAdd);
            pendingAdd.clear();
        }

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(deltaTime);
            
            // Упрощенная коллизия с миром
            if (p instanceof ShardParticle shard && !p.isRemoved()) {
                Vector3f pos = p.getPosition();
                BlockPos bPos = new BlockPos((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
                Block b = world.getBlock(bPos);
                if (!b.isAir() && BlockRegistry.getBlock(b.getType()).isSolid()) {
                    shard.setGrounded(true);
                }
            }

            if (p.isRemoved()) {
                it.remove();
            }
        }
        
        // Лимит частиц для FPS
        if (particles.size() > 2000) {
            for (int i = 0; i < 500; i++) particles.remove(0);
        }
    }

    /**
     * Создает эффект разрушения блока (Shatter).
     */
    public void spawnShatter(BlockPos pos, Block block) {
        BlockDefinition def = BlockRegistry.getBlock(block.getType());
        if (def == null || block.getType() == 0) return;

        com.za.zenith.world.physics.VoxelShape shape = block.getShape();
        if (shape == null || shape.getBoxes().isEmpty()) return;

        // ГРАМОТНОЕ ОПРЕДЕЛЕНИЕ МАТЕРИАЛА
        int baseMatType = ShardParticle.MAT_GENERIC;
        String blockIdStr = def.getIdentifier().toString();
        
        // Только реальные растения - легкие. Грязь с травой - тяжелая.
        if (blockIdStr.contains("leaves") || blockIdStr.equals("zenith:short_grass") || blockIdStr.contains("plant")) {
            baseMatType = ShardParticle.MAT_LEAVES;
        } else if (blockIdStr.contains("log") || blockIdStr.contains("plank") || blockIdStr.contains("wood")) {
            baseMatType = ShardParticle.MAT_WOOD;
        }

        // Calculate total bounds
        float minX = 1, minY = 1, minZ = 1;
        float maxX = 0, maxY = 0, maxZ = 0;
        for (com.za.zenith.world.physics.AABB box : shape.getBoxes()) {
            minX = Math.min(minX, box.getMin().x);
            minY = Math.min(minY, box.getMin().y);
            minZ = Math.min(minZ, box.getMin().z);
            maxX = Math.max(maxX, box.getMax().x);
            maxY = Math.max(maxY, box.getMax().y);
            maxZ = Math.max(maxZ, box.getMax().z);
        }

        float sizeX = maxX - minX;
        float sizeY = maxY - minY;
        float sizeZ = maxZ - minZ;
        float volume = sizeX * sizeY * sizeZ;

        int grid = (volume < 0.15f) ? 1 : 2; 
        
        float stepX = sizeX / grid;
        float stepY = sizeY / grid;
        float stepZ = sizeZ / grid;
        
        float offsetX = stepX / 2.0f;
        float offsetY = stepY / 2.0f;
        float offsetZ = stepZ / 2.0f;

        float baseScale = Math.max(sizeX, Math.max(sizeY, sizeZ));
        com.za.zenith.engine.graphics.DynamicTextureAtlas atlas = com.za.zenith.engine.core.GameLoop.getInstance().getRenderer().getAtlas();

        for (int x = 0; x < grid; x++) {
            for (int y = 0; y < grid; y++) {
                for (int z = 0; z < grid; z++) {
                    float localX = minX + x * stepX + offsetX;
                    float localY = minY + y * stepY + offsetY;
                    float localZ = minZ + z * stepZ + offsetZ;

                    boolean inside = false;
                    for (com.za.zenith.world.physics.AABB box : shape.getBoxes()) {
                        if (localX >= box.getMin().x && localX <= box.getMax().x &&
                            localY >= box.getMin().y && localY <= box.getMax().y &&
                            localZ >= box.getMin().z && localZ <= box.getMax().z) {
                            inside = true;
                            break;
                        }
                    }
                    if (!inside) continue;

                    // ГИБРИДНАЯ ЛОГИКА ДЛЯ ТРАВЫ/ГРЯЗИ
                    int shardMat = baseMatType;
                    String texKey;
                    boolean shardTinted = def.isTinted();

                    if (blockIdStr.equals("zenith:grass_block")) {
                        if (y == grid - 1) { // Верхний слой - трава
                            texKey = def.getTextures().getTop();
                            shardTinted = true;
                        } else { // Нижние слои - чистая земля
                            texKey = def.getTextures().getBottom();
                            shardTinted = false;
                        }
                    } else {
                        // Обычная логика: inner или side
                        texKey = def.getInnerTextureIndex() != -1 ? def.getTextures().getInner() : def.getTextures().getSouth();
                    }

                    int texLayer = (int)atlas.getLayer(texKey);

                    float jitter = 0.1f * stepX;
                    Vector3f pPos = new Vector3f(
                        pos.x() + localX + (float)(Math.random() - 0.5) * jitter,
                        pos.y() + localY + (float)(Math.random() - 0.5) * jitter,
                        pos.z() + localZ + (float)(Math.random() - 0.5) * jitter
                    );

                    Vector3f vel = new Vector3f(pPos).sub(pos.x() + minX + sizeX/2f, pos.y() + minY + sizeY/2f, pos.z() + minZ + sizeZ/2f);
                    float force = (shardMat == ShardParticle.MAT_LEAVES) ? 0.5f : 1.2f;
                    vel.normalize().mul(force + (float)Math.random() * force);
                    vel.y += (shardMat == ShardParticle.MAT_LEAVES) ? 0.3f : 1.0f;

                    float life = 1.0f + (float)Math.random() * 1.5f;
                    
                    ShardParticle shard = new ShardParticle(pPos, vel, life, block.getType(), shardMat, texLayer, shardTinted, x, y, z, grid, baseScale);
                    pendingAdd.add(shard);
                }
            }
        }
    }

    public List<Particle> getActiveParticles() {
        return particles;
    }
}
