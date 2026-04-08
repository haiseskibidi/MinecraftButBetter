package com.za.minecraft.world;

import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.ITickable;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.chunks.ChunkPos;
import com.za.minecraft.world.generation.TerrainGenerator;
import com.za.minecraft.world.items.ItemStack;

import com.za.minecraft.entities.Entity;
import com.za.minecraft.entities.Player;
import com.za.minecraft.entities.ScoutEntity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

public class World {
    private final Map<ChunkPos, Chunk> chunks;
    private final List<Entity> entities;
    private final Map<BlockPos, BlockEntity> blockEntities;
    private final List<ITickable> tickableBlockEntities;
    
    public static class BlockDamageInstance {
        private float damage;
        private final List<Vector3f> hitHistory;

        public BlockDamageInstance(float damage, List<Vector3f> hitHistory) {
            this.damage = damage;
            this.hitHistory = new ArrayList<>(hitHistory);
        }

        public float getDamage() { return damage; }
        public void setDamage(float damage) { this.damage = damage; }
        public List<Vector3f> getHitHistory() { return hitHistory; }
    }

    private final Map<BlockPos, BlockDamageInstance> blockDamageMap = new ConcurrentHashMap<>();

    public Map<BlockPos, BlockDamageInstance> getBlockDamageMap() {
        return blockDamageMap;
    }

    private Player player;
    private final TerrainGenerator terrainGenerator;
    private final long seed;
    
    public World() {
        this.chunks = new ConcurrentHashMap<>();
        this.entities = new CopyOnWriteArrayList<>();
        this.blockEntities = new ConcurrentHashMap<>();
        this.tickableBlockEntities = new CopyOnWriteArrayList<>();
        this.seed = System.currentTimeMillis(); // Random seed each time
        com.za.minecraft.utils.Logger.info("Generating new world with seed: %d", seed);
        this.terrainGenerator = new TerrainGenerator(seed);
        generateWorld();
    }
    
    public World(long seed) {
        this.chunks = new ConcurrentHashMap<>();
        this.entities = new CopyOnWriteArrayList<>();
        this.blockEntities = new ConcurrentHashMap<>();
        this.tickableBlockEntities = new CopyOnWriteArrayList<>();
        this.seed = seed;
        com.za.minecraft.utils.Logger.info("Generating new world with seed: %d", seed);
        this.terrainGenerator = new TerrainGenerator(seed);
        generateWorld();
    }
    
    private void generateWorld() {
        int renderDistance = 12; // Оптимальный размер для избежания OutOfMemory
        
        // First pass: generate terrain
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk chunk = new Chunk(pos);
                
                terrainGenerator.generateTerrain(chunk);
                chunks.put(pos, chunk);
            }
        }
        
        // Second pass: generate structures (trees, etc.)
        com.za.minecraft.utils.Logger.info("Generating structures for %d chunks...", (renderDistance * 2 + 1) * (renderDistance * 2 + 1));
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk chunk = chunks.get(pos);
                if (chunk != null) {
                    terrainGenerator.generateStructures(this, chunk);
                }
            }
        }
        
        // Spawn initial scouts on surface
        for (int i = 0; i < 15; i++) {
            int rx = (int) ((Math.random() - 0.5) * 160);
            int rz = (int) ((Math.random() - 0.5) * 160);
            int ry = getSurfaceHeight(rx, rz);
            if (ry > 0) {
                spawnEntity(new ScoutEntity(new Vector3f(rx + 0.5f, ry + 1.0f, rz + 0.5f)));
            }
        }

        // Spawn initial resources on surface
        for (int i = 0; i < 40; i++) {
            int rx = (int) ((Math.random() - 0.5) * 200);
            int rz = (int) ((Math.random() - 0.5) * 200);
            int ry = getSurfaceHeight(rx, rz);
            if (ry > 0) {
                com.za.minecraft.world.items.Item item = Math.random() > 0.5 ? 
                    com.za.minecraft.world.items.Items.ROCK : 
                    (Math.random() > 0.5 ? com.za.minecraft.world.items.Items.STICK : com.za.minecraft.world.items.Items.FLINT);
                float randomRot = (float) (Math.random() * Math.PI * 2);
                spawnEntity(new com.za.minecraft.entities.ResourceEntity(
                    new Vector3f(rx + 0.5f, ry + 1.0f, rz + 0.5f), 
                    new com.za.minecraft.world.items.ItemStack(item),
                    randomRot
                ));
            }
        }
        
        com.za.minecraft.utils.Logger.info("World generation completed!");
    }

    private int getSurfaceHeight(int x, int z) {
        for (int y = 255; y > 0; y--) {
            Block b = getBlock(x, y, z);
            if (!b.isAir() && com.za.minecraft.world.blocks.BlockRegistry.getBlock(b.getType()).isSolid()) {
                return y;
            }
        }
        return -1;
    }

    public void update(float deltaTime) {
        // Update all entities
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            
            if (entity.isRemoved()) {
                entities.remove(i);
                continue;
            }

            entity.update(deltaTime, this);
            
            // Item Pickup logic
            if (entity instanceof com.za.minecraft.entities.ItemEntity itemEntity) {
                if (player != null && itemEntity.canBePickedUp()) {
                    float pickupRadius = com.za.minecraft.world.physics.PhysicsSettings.getInstance().itemPickupRadius;
                    
                    Vector3f playerCenter = new Vector3f(player.getPosition());
                    playerCenter.y += player.getHeight() * 0.5f;
                    
                    // Центр предмета (чуть выше его базовой позиции)
                    Vector3f itemCenter = new Vector3f(itemEntity.getPosition());
                    itemCenter.y += 0.125f; 
                    
                    float dist = playerCenter.distance(itemCenter);
                    
                    // 100% надежный подбор: пересечение хитбоксов ИЛИ вхождение в радиус
                    // Если предмет уже летит к нам (magnetic), мы расширяем окно подбора для стабильности
                    boolean isMagnetic = itemEntity.getVelocity().lengthSquared() > 25.0f; // Признак активного полета
                    float effectiveRadius = isMagnetic ? pickupRadius * 1.5f : pickupRadius;
                    
                    if (dist < effectiveRadius || player.getBoundingBox().intersects(itemEntity.getBoundingBox())) {
                        if (player.getInventory().addItem(itemEntity.getStack())) {
                            entities.remove(i);
                            com.za.minecraft.utils.Logger.info("Picked up item: %s", itemEntity.getStack().getItem().getName());
                            continue;
                        }
                    }
                }
            }

            // Remove dead entities (if they are LivingEntity)
            if (entity instanceof com.za.minecraft.entities.LivingEntity living) {
                if (living.isDead()) {
                    entities.remove(i);
                }
            }
        }
        
        // Update tickable block entities
        for (int i = tickableBlockEntities.size() - 1; i >= 0; i--) {
            ITickable tickable = tickableBlockEntities.get(i);
            if (tickable instanceof BlockEntity be && be.isRemoved()) {
                tickableBlockEntities.remove(i);
                continue;
            }
            if (tickable.shouldTick()) {
                tickable.update(deltaTime);
            }
        }
        
        if (player != null) {
            player.update(deltaTime, this);
        }

        // Heal blocks over time and fade scars
        if (!blockDamageMap.isEmpty()) {
            for (java.util.Map.Entry<BlockPos, BlockDamageInstance> entry : blockDamageMap.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockDamageInstance info = entry.getValue();
                float damage = info.getDamage();
                
                int blockType = getBlock(pos).getType();
                com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(blockType);
                
                if (def.getHealingSpeed() > 0) {
                    float maxHealth = def.getHardness() * 10.0f;
                    float healAmount = def.getHealingSpeed() * maxHealth * deltaTime;
                    float newDamage = damage - healAmount;
                    
                    if (newDamage <= 0) {
                        blockDamageMap.remove(pos);
                    } else {
                        info.setDamage(newDamage);
                        
                        // Fade scars: remove oldest hits as the block heals
                        List<Vector3f> history = info.getHitHistory();
                        if (!history.isEmpty()) {
                            // Link history size to current damage percentage (max 16 hits in shader)
                            int targetHistorySize = (int) Math.ceil((newDamage / maxHealth) * 16);
                            while (history.size() > targetHistorySize && !history.isEmpty()) {
                                history.remove(0); // Remove oldest
                            }
                        }
                    }
                }
            }
        }
    }

    public void registerTickable(ITickable tickable) {
        if (!tickableBlockEntities.contains(tickable)) {
            tickableBlockEntities.add(tickable);
        }
    }

    public void unregisterTickable(ITickable tickable) {
        tickableBlockEntities.remove(tickable);
    }

    public float getBlockDamage(BlockPos pos) {
        BlockDamageInstance info = blockDamageMap.get(pos);
        return (info != null) ? info.getDamage() : 0.0f;
    }

    public List<Vector3f> getBlockHitHistory(BlockPos pos) {
        BlockDamageInstance info = blockDamageMap.get(pos);
        return (info != null) ? info.getHitHistory() : new ArrayList<>();
    }

    public void setBlockDamage(BlockPos pos, float damage) {
        setBlockDamage(pos, damage, new ArrayList<>());
    }

    public void setBlockDamage(BlockPos pos, float damage, List<Vector3f> history) {
        if (damage <= 0.0f) {
            blockDamageMap.remove(pos);
        } else {
            BlockDamageInstance info = blockDamageMap.get(pos);
            if (info != null) {
                info.setDamage(damage);
                // Update history, keeping only the last 16 hits
                List<Vector3f> targetHistory = info.getHitHistory();
                targetHistory.clear();
                for (int i = Math.max(0, history.size() - 16); i < history.size(); i++) {
                    targetHistory.add(history.get(i));
                }
            } else {
                blockDamageMap.put(pos, new BlockDamageInstance(damage, history));
            }
        }
    }

    public void spawnEntity(Entity entity) {
        entities.add(entity);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public java.util.Map<BlockPos, com.za.minecraft.world.blocks.entity.BlockEntity> getBlockEntities() {
        return blockEntities;
    }
    
    public Block getBlock(BlockPos pos) {
        return getBlock(pos.x(), pos.y(), pos.z());
    }
    
    public Block getBlock(int x, int y, int z) {
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk == null) {
            return new Block(com.za.minecraft.world.blocks.Blocks.AIR.getId());
        }
        
        int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
        int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
        
        return chunk.getBlock(localX, y, localZ);
    }
    
    public void setBlock(BlockPos pos, Block block) {
        setBlock(pos.x(), pos.y(), pos.z(), block);
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        
        // Remove old block entity if it exists
        removeBlockEntity(pos);
        
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk != null) {
            int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
            int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, block);
            chunk.setNeedsMeshUpdate(true);
            
            // Automatically create block entity if defined
            BlockEntity be = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType()).createBlockEntity(pos);
            if (be != null) {
                setBlockEntity(be);
            }

            // Notify neighbors if block is on the edge
            if (localX == 0) notifyChunkUpdate(chunkPos.x() - 1, chunkPos.z());
            if (localX == Chunk.CHUNK_SIZE - 1) notifyChunkUpdate(chunkPos.x() + 1, chunkPos.z());
            if (localZ == 0) notifyChunkUpdate(chunkPos.x(), chunkPos.z() - 1);
            if (localZ == Chunk.CHUNK_SIZE - 1) notifyChunkUpdate(chunkPos.x(), chunkPos.z() + 1);
        }
    }

    /**
     * Вызывается игроком при завершении прогресса разрушения блока.
     * @return true если блок должен быть удален.
     */
    public boolean onBlockBreak(BlockPos pos, Player player) {
        Block block = getBlock(pos);
        if (block.isAir()) return true;

        com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType());
        return def.onBlockBreak(this, pos, block, player);
    }

    /**
     * Разрушает блок игроком. Вызывает хук onDestroyed.
     */
    public void destroyBlock(BlockPos pos, Player player) {
        Block block = getBlock(pos);
        if (block.isAir()) return;

        com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType());
        def.onDestroyed(this, pos, block, player);

        setBlock(pos, new Block(Blocks.AIR.getId()));
    }

    public void setBlockEntity(BlockEntity entity) {
        BlockPos pos = entity.getPos();
        removeBlockEntity(pos); // Clean up any existing at this position
        
        entity.setWorld(this);
        blockEntities.put(pos, entity);
        if (entity instanceof ITickable) {
            tickableBlockEntities.add((ITickable) entity);
        }

        // Trigger mesh update for the chunk
        com.za.minecraft.world.chunks.Chunk chunk = getChunk(com.za.minecraft.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
        if (chunk != null) {
            chunk.setNeedsMeshUpdate(true);
        }
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    public void removeBlockEntity(BlockPos pos) {
        BlockEntity entity = blockEntities.remove(pos);
        if (entity != null) {
            entity.setRemoved();
            // Trigger mesh update for the chunk
            com.za.minecraft.world.chunks.Chunk chunk = getChunk(com.za.minecraft.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
            if (chunk != null) {
                chunk.setNeedsMeshUpdate(true);
            }
        }
    }

    
    private void notifyChunkUpdate(int cx, int cz) {
        Chunk neighbor = chunks.get(new ChunkPos(cx, cz));
        if (neighbor != null) {
            neighbor.setNeedsMeshUpdate(true);
        }
    }
    
    public Iterable<Chunk> getLoadedChunks() {
        return chunks.values();
    }
    
    public Chunk getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }
    
    public long getSeed() {
        return seed;
    }
    
    public void setBlock(int x, int y, int z, int blockType) {
        setBlock(x, y, z, new Block(blockType));
    }

    public void spawnItem(ItemStack stack, float x, float y, float z) {
        com.za.minecraft.entities.ItemEntity entity = new com.za.minecraft.entities.ItemEntity(new Vector3f(x, y, z), stack);
        spawnEntity(entity);
    }

    /**
     * Рассчитывает уровень шума в конкретной точке мира.
     * Учитывает шум игрока и работающих машин (BlockEntities).
     */
    public float getNoiseLevelAt(Vector3f pos) {
        float totalNoise = 0.0f;
        
        // Шум от игрока
        if (player != null) {
            float dist = pos.distance(player.getPosition());
            float playerNoise = player.getNoiseLevel();
            // Затухание шума игрока (радиус 32 блока)
            if (dist < 32.0f) {
                totalNoise = Math.max(totalNoise, playerNoise * (1.0f - dist / 32.0f));
            }
        }
        
        // Шум от активных сущностей блоков
        for (BlockEntity be : blockEntities.values()) {
            if (be instanceof com.za.minecraft.world.blocks.entity.GeneratorBlockEntity generator && generator.isRunning()) {
                float dist = pos.distance(new Vector3f(be.getPos().x() + 0.5f, be.getPos().y() + 0.5f, be.getPos().z() + 0.5f));
                // Шум генератора (радиус 20 блоков, базовый шум 0.5)
                if (dist < 20.0f) {
                    float genNoise = 0.5f * (1.0f - dist / 20.0f);
                    totalNoise = Math.max(totalNoise, genNoise);
                }
            }
        }
        
        return totalNoise;
    }
}
