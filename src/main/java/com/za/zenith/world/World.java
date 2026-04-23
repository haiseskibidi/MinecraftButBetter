package com.za.zenith.world;

import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.blocks.entity.ITickable;
import com.za.zenith.world.lighting.LightEngine;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.chunks.ChunkPos;
import com.za.zenith.world.generation.TerrainGenerator;
import com.za.zenith.world.items.ItemStack;

import com.za.zenith.entities.Entity;
import com.za.zenith.entities.Player;
import com.za.zenith.entities.ScoutEntity;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.Map;

public class World {
    private final Map<ChunkPos, Chunk> chunks;
    private final Map<ChunkPos, Chunk> stagingChunks = new ConcurrentHashMap<>();
    private final List<Entity> entities;
    private final Map<BlockPos, BlockEntity> blockEntities;
    private final List<ITickable> tickableBlockEntities;
    private final LightEngine lightEngine;
    private float worldTime; // Stored as float for smooth interpolation
    
    private final ExecutorService chunkGenExecutor = Executors.newFixedThreadPool(
        Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 2)),
        r -> {
            Thread t = new Thread(r, "ChunkGenerator");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    );
    private final Set<ChunkPos> generatingChunks = ConcurrentHashMap.newKeySet();
    private int lastPlayerChunkX = Integer.MAX_VALUE;
    private int lastPlayerChunkZ = Integer.MAX_VALUE;

    private static class WorldCache {
        Chunk lastChunk;
        int lastChunkX = Integer.MAX_VALUE;
        int lastChunkZ = Integer.MAX_VALUE;
    }
    private final ThreadLocal<WorldCache> threadCache = ThreadLocal.withInitial(WorldCache::new);

    public Chunk getChunk(int chunkX, int chunkZ) {
        WorldCache cache = threadCache.get();
        if (chunkX == cache.lastChunkX && chunkZ == cache.lastChunkZ) return cache.lastChunk;
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        cache.lastChunk = chunks.get(pos);
        cache.lastChunkX = chunkX;
        cache.lastChunkZ = chunkZ;
        return cache.lastChunk;
    }

    public Chunk getChunk(ChunkPos pos) {
        if (pos == null) return null;
        return getChunk(pos.x(), pos.z());
    }

    public Chunk getChunkInternal(int chunkX, int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        Chunk c = chunks.get(pos);
        if (c == null) {
            c = stagingChunks.get(pos);
            // Double-check chunks in case it was moved from staging to main map between the two calls
            if (c == null) c = chunks.get(pos);
        }
        return c;
    }

    public Chunk getChunkInternal(ChunkPos pos) {
        if (pos == null) return null;
        return getChunkInternal(pos.x(), pos.z());
    }

    public int getRawBlockData(int x, int y, int z) {
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) return 0;
        Chunk chunk = getChunk(x >> 4, z >> 4);
        if (chunk == null) return 0;
        return chunk.getRawBlockData(x & 15, y, z & 15);
    }

    public static class BlockDamageInstance {
        private float damage;
        private final Block block;
        private final List<Vector4f> hitHistory;
        private long lastHitTime;

        public BlockDamageInstance(float damage, Block block, List<Vector4f> hitHistory) {
            this.damage = damage;
            this.block = block;
            this.hitHistory = new ArrayList<>(hitHistory);
            this.lastHitTime = System.currentTimeMillis();
        }

        public Block getBlock() { return block; }
        public float getDamage() { return damage; }
        public void setDamage(float damage) { this.damage = damage; }
        public List<Vector4f> getHitHistory() { return hitHistory; }
        public long getLastHitTime() { return lastHitTime; }
        public void resetLastHitTime() { this.lastHitTime = System.currentTimeMillis(); }
    }

    private final Map<BlockPos, BlockDamageInstance> blockDamageMap = new ConcurrentHashMap<>();

    public Map<BlockPos, BlockDamageInstance> getBlockDamageMap() {
        return blockDamageMap;
    }

    private Player player;
    private final TerrainGenerator terrainGenerator;
    private final long seed;
    private boolean generating = false;
    
    public World() {
        this.chunks = new ConcurrentHashMap<>();
        this.entities = new CopyOnWriteArrayList<>();
        this.blockEntities = new ConcurrentHashMap<>();
        this.tickableBlockEntities = new CopyOnWriteArrayList<>();
        this.seed = System.currentTimeMillis(); // Random seed each time
        com.za.zenith.utils.Logger.info("Generating new world with seed: %d", seed);
        this.terrainGenerator = new TerrainGenerator(seed);
        this.lightEngine = new com.za.zenith.world.lighting.LightEngine(this);
        this.worldTime = WorldSettings.getInstance().initialTime;
        
        generating = true;
        generateWorld();
        generating = false;
    }
    
    public World(long seed) {
        this.chunks = new ConcurrentHashMap<>();
        this.entities = new CopyOnWriteArrayList<>();
        this.blockEntities = new ConcurrentHashMap<>();
        this.tickableBlockEntities = new CopyOnWriteArrayList<>();
        this.seed = seed;
        com.za.zenith.utils.Logger.info("Generating new world with seed: %d", seed);
        this.terrainGenerator = new TerrainGenerator(seed);
        this.lightEngine = new com.za.zenith.world.lighting.LightEngine(this);
        this.worldTime = WorldSettings.getInstance().initialTime;
        
        generating = true;
        generateWorld();
        generating = false;
    }
    
    private void generateWorld() {
        int renderDistance = com.za.zenith.world.generation.GenerationSettings.getInstance().initialRenderDistance; // Generate small radius initially
        
        // First pass: generate terrain
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk chunk = new Chunk(pos);
                
                terrainGenerator.generateTerrain(chunk);
                chunks.put(pos, chunk);
            }
        }
        
        // Second pass: generate structures (trees, etc.) and sunlight
        com.za.zenith.utils.Logger.info("Generating structures and sunlight for %d chunks...", (renderDistance * 2 + 1) * (renderDistance * 2 + 1));
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk chunk = chunks.get(pos);
                if (chunk != null) {
                    terrainGenerator.generateStructures(this, chunk);
                    lightEngine.generateInitialSunlight(chunk);
                    chunk.setReady(true);
                    lightEngine.onChunkReady(chunk);
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
                com.za.zenith.world.items.Item item = Math.random() > 0.5 ? 
                    com.za.zenith.world.items.Items.ROCK : 
                    (Math.random() > 0.5 ? com.za.zenith.world.items.Items.STICK : com.za.zenith.world.items.Items.FLINT);
                float randomRot = (float) (Math.random() * Math.PI * 2);
                spawnEntity(new com.za.zenith.entities.ResourceEntity(
                    new Vector3f(rx + 0.5f, ry + 1.0f, rz + 0.5f), 
                    new com.za.zenith.world.items.ItemStack(item),
                    randomRot
                ));
            }
        }
        
        com.za.zenith.utils.Logger.info("World generation completed!");
        this.worldTime = WorldSettings.getInstance().initialTime;
    }

    private final java.util.LinkedHashSet<ChunkPos> pendingChunkQueue = new java.util.LinkedHashSet<>();

    private void updateChunks() {
        if (player == null) return;

        int currentChunkX = (int) Math.floor(player.getPosition().x / Chunk.CHUNK_SIZE);
        int currentChunkZ = (int) Math.floor(player.getPosition().z / Chunk.CHUNK_SIZE);

        int renderDistance = com.za.zenith.world.generation.GenerationSettings.getInstance().activeRenderDistance;
        int unloadDistance = com.za.zenith.world.generation.GenerationSettings.getInstance().unloadDistance;

        if (currentChunkX != lastPlayerChunkX || currentChunkZ != lastPlayerChunkZ) {
            lastPlayerChunkX = currentChunkX;
            lastPlayerChunkZ = currentChunkZ;

            for (int cx = currentChunkX - renderDistance; cx <= currentChunkX + renderDistance; cx++) {
                for (int cz = currentChunkZ - renderDistance; cz <= currentChunkZ + renderDistance; cz++) {
                    ChunkPos pos = new ChunkPos(cx, cz);
                    if (!chunks.containsKey(pos) && !generatingChunks.contains(pos)) {
                        pendingChunkQueue.add(pos); // O(1) in LinkedHashSet
                    }
                }
            }

            // Unload chunks outside unloadDistance
            List<ChunkPos> toRemove = new ArrayList<>();
            for (ChunkPos pos : chunks.keySet()) {
                if (Math.abs(pos.x() - currentChunkX) > unloadDistance || Math.abs(pos.z() - currentChunkZ) > unloadDistance) {
                    toRemove.add(pos);
                }
            }
            for (ChunkPos pos : toRemove) {
                chunks.remove(pos);
            }

            pendingChunkQueue.removeIf(p -> Math.abs(p.x() - currentChunkX) > renderDistance || Math.abs(p.z() - currentChunkZ) > renderDistance);

            List<ChunkPos> sortedPending = new ArrayList<>(pendingChunkQueue);
            sortedPending.sort((p1, p2) -> {
                int d1 = (p1.x() - currentChunkX) * (p1.x() - currentChunkX) + (p1.z() - currentChunkZ) * (p1.z() - currentChunkZ);
                int d2 = (p2.x() - currentChunkX) * (p2.x() - currentChunkX) + (p2.z() - currentChunkZ) * (p2.z() - currentChunkZ);
                return Integer.compare(d1, d2);
            });
            pendingChunkQueue.clear();
            pendingChunkQueue.addAll(sortedPending);
        }

        if (!pendingChunkQueue.isEmpty()) {
            int submittedThisTick = 0;
            java.util.Iterator<ChunkPos> it = pendingChunkQueue.iterator();
            while (it.hasNext() && submittedThisTick < 4) {
                ChunkPos pos = it.next();
                it.remove();
                if (!chunks.containsKey(pos) && !stagingChunks.containsKey(pos) && !generatingChunks.contains(pos)) {
                    generatingChunks.add(pos);
                    chunkGenExecutor.submit(() -> {                        try {
                            Chunk chunk = new Chunk(pos);
                            
                            terrainGenerator.generateTerrain(chunk);
                            
                            stagingChunks.put(pos, chunk); // Put in staging ONLY after terrain is ready
                            terrainGenerator.generateStructures(World.this, chunk);
                            lightEngine.generateInitialSunlight(chunk);
                            
                            chunk.setReady(true);
                            chunk.setNeedsMeshUpdate(true);
                            
                            lightEngine.onChunkReady(chunk);
                            
                            // Atomic swap from staging to main world
                            chunks.put(pos, chunk);
                            stagingChunks.remove(pos);
                        } catch (Exception e) {
                            com.za.zenith.utils.Logger.error("Error generating chunk %s: %s", pos, e.getMessage());
                            stagingChunks.remove(pos);
                        } finally {
                            generatingChunks.remove(pos);
                        }
                    });
                    submittedThisTick++;
                }
            }
        }
    }

    public int getFastSurfaceColor(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        Chunk chunk = getChunk(new ChunkPos(cx, cz));
        if (chunk == null || !chunk.isReady()) return 0xFF000000;

        int lx = x & 15;
        int lz = z & 15;
        
        int y = chunk.getHighestBlock(lx, lz);
        if (y <= 0) return 0xFF000000;

        int data = chunk.getRawBlockData(lx, y, lz);
        int type = data >> 8;
        if (type == 0) return 0xFF000000; // Air
        
        if (!com.za.zenith.engine.graphics.ui.renderers.MinimapRegistry.isSolid(type) && type != com.za.zenith.world.blocks.Blocks.WATER.getId()) return 0xFF000000;

        int color = com.za.zenith.engine.graphics.ui.renderers.MinimapRegistry.getColor(type);
        
        // Apply height-based shading for volume effect
        float brightness = 0.7f + (y / (float)Chunk.CHUNK_HEIGHT) * 0.6f;
        int r = (int) ((color & 0xFF) * brightness);
        int g = (int) (((color >> 8) & 0xFF) * brightness);
        int b = (int) (((color >> 16) & 0xFF) * brightness);
        
        // Store Y height in Alpha channel for toon-shading outlines in shader
        return (y << 24) | (Math.min(255, b) << 16) | (Math.min(255, g) << 8) | Math.min(255, r);
    }

    private int getSurfaceHeight(int x, int z) {
        for (int y = 255; y > 0; y--) {
            Block b = getBlock(x, y, z);
            if (!b.isAir() && com.za.zenith.world.blocks.BlockRegistry.getBlock(b.getType()).isSolid()) {
                return y;
            }
        }
        return -1;
    }

    public void update(float deltaTime) {
        updateChunks();
        
        // Advance time
        worldTime += deltaTime * WorldSettings.getInstance().dayCycleSpeed * 20.0f; // 20 units per real second at 1.0 speed
        if (worldTime >= WorldSettings.getInstance().dayLength) {
            worldTime -= WorldSettings.getInstance().dayLength;
        }

        // Update all entities
        boolean inventoryFull = (player != null && player.getInventory().isFull());
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            
            if (entity.isRemoved()) {
                entities.remove(i);
                continue;
            }

            entity.update(deltaTime, this);
            
            // Item Pickup logic
            if (entity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                if (player != null && itemEntity.canBePickedUp()) {
                    float pickupRadius = com.za.zenith.world.physics.PhysicsSettings.getInstance().itemPickupRadius;
                    
                    Vector3f playerCenter = new Vector3f(player.getPosition());
                    playerCenter.y += player.getHeight() * 0.5f;
                    
                    Vector3f itemPos = itemEntity.getPosition();
                    float distSq = playerCenter.distanceSquared(itemPos);
                    
                    boolean isMagnetic = itemEntity.isBeingAttracted();
                    float effectiveRadius = isMagnetic ? pickupRadius * 1.5f : pickupRadius;
                    
                    if (distSq < effectiveRadius * effectiveRadius || player.getBoundingBox().intersects(itemEntity.getBoundingBox())) {
                        if (itemEntity.isRemoved()) continue; // Skip if already handled this frame
                        
                        if (inventoryFull) {
                             com.za.zenith.engine.graphics.ui.NotificationTriggers.getInstance().onInventoryFull();
                        } else if (player.getInventory().addItem(itemEntity.getStack(), true)) {
                            itemEntity.setRemoved();
                            com.za.zenith.utils.Logger.info("Picked up item: %s", itemEntity.getStack().getItem().getName());
                            inventoryFull = player.getInventory().isFull();
                            continue;
                        } else {
                            inventoryFull = true; 
                        }
                    }
                }
            }

            // Remove dead entities (if they are LivingEntity)
            if (entity instanceof com.za.zenith.entities.LivingEntity living) {
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
            long currentTime = System.currentTimeMillis();
            for (java.util.Map.Entry<BlockPos, BlockDamageInstance> entry : blockDamageMap.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockDamageInstance info = entry.getValue();
                
                // --- HEALING DELAY ---
                // Do not start healing if block was hit in the last 5 seconds
                if (currentTime - info.getLastHitTime() < 5000) continue;

                float damage = info.getDamage();
                int blockType = getBlock(pos).getType();
                com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(blockType);
                
                if (def.getHealingSpeed() > 0) {
                    float maxHealth = def.getHardness() * 10.0f;
                    float healAmount = def.getHealingSpeed() * maxHealth * deltaTime;
                    float newDamage = damage - healAmount;
                    
                    if (newDamage <= 0) {
                        blockDamageMap.remove(pos);
                    } else {
                        info.setDamage(newDamage);
                        
                        // --- SMOOTH SCAR FADING ---
                        List<Vector4f> history = info.getHitHistory();
                        if (!history.isEmpty()) {
                            // Target total intensity across all scars based on health
                            // 16 is max history size in shader. If damage is 50%, target total intensity is 8.0
                            float targetTotalIntensity = (newDamage / maxHealth) * 16.0f;
                            
                            float currentTotalIntensity = 0;
                            for (Vector4f hit : history) currentTotalIntensity += hit.w;

                            if (currentTotalIntensity > targetTotalIntensity) {
                                float toRemove = currentTotalIntensity - targetTotalIntensity;
                                while (toRemove > 0 && !history.isEmpty()) {
                                    Vector4f oldest = history.get(0);
                                    if (oldest.w <= toRemove) {
                                        toRemove -= oldest.w;
                                        history.remove(0);
                                    } else {
                                        oldest.w -= toRemove;
                                        toRemove = 0;
                                    }
                                }
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

    public List<Vector4f> getBlockHitHistory(BlockPos pos) {
        BlockDamageInstance info = blockDamageMap.get(pos);
        return (info != null) ? info.getHitHistory() : new ArrayList<>();
    }

    public void setBlockDamage(BlockPos pos, float damage) {
        setBlockDamage(pos, damage, new ArrayList<>());
    }

    public void setBlockDamage(BlockPos pos, float damage, List<Vector4f> history) {
        if (damage <= 0.0f) {
            blockDamageMap.remove(pos);
        } else {
            BlockDamageInstance info = blockDamageMap.get(pos);
            if (info != null) {
                info.setDamage(damage);
                info.resetLastHitTime(); // Refresh the delay on every hit
                
                // Update history smoothly: only add new ones that aren't already there
                List<Vector4f> targetHistory = info.getHitHistory();
                if (history.size() > targetHistory.size()) {
                    for (int i = targetHistory.size(); i < history.size(); i++) {
                        targetHistory.add(new Vector4f(history.get(i)));
                    }
                }
                
                // Cap at 16
                while (targetHistory.size() > 16) {
                    targetHistory.remove(0);
                }
            } else {
                blockDamageMap.put(pos, new BlockDamageInstance(damage, getBlock(pos), new ArrayList<>(history)));
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

    public java.util.Map<BlockPos, com.za.zenith.world.blocks.entity.BlockEntity> getBlockEntities() {
        return blockEntities;
    }
    
    public Block getBlock(BlockPos pos) {
        return getBlock(pos.x(), pos.y(), pos.z());
    }
    
    public Block getBlock(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockDamageInstance damageInstance = blockDamageMap.get(pos);
        if (damageInstance != null) {
            return damageInstance.getBlock();
        }

        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) chunk = stagingChunks.get(chunkPos);
        
        if (chunk == null) {
            return new Block(com.za.zenith.world.blocks.Blocks.AIR.getId());
        }
        
        int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
        int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
        
        return chunk.getBlock(localX, y, localZ);
    }
    
    public void setBlock(BlockPos pos, Block block) {
        setBlock(pos.x(), pos.y(), pos.z(), block, true);
    }
    
    public void setBlockQuietly(BlockPos pos, Block block) {
        setBlock(pos.x(), pos.y(), pos.z(), block, false);
    }

    public void setBlockQuietly(int x, int y, int z, Block block) {
        setBlock(x, y, z, block, false);
    }

    public void setBlockDuringGen(int x, int y, int z, Block block) {
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        boolean inMainWorld = (chunk != null);
        if (chunk == null) chunk = stagingChunks.get(chunkPos); // Check staging if not in main world yet
        
        if (chunk != null) {
            int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
            int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, block);
            
            // If we are modifying an ALREADY ready chunk (e.g. tree leaves growing into neighbors),
            // we MUST update lighting, otherwise it creates inconsistent light states.
            if (inMainWorld || chunk.isReady()) {
                lightEngine.onBlockChanged(new BlockPos(x, y, z));
            }

            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
            if (def.hasBlockEntity()) {
                BlockPos pos = new BlockPos(x, y, z);
                com.za.zenith.world.blocks.entity.BlockEntity be = def.createBlockEntity(pos);
                if (be != null) {
                    be.setWorld(this);
                    blockEntities.put(pos, be);
                    if (be instanceof ITickable) {
                        tickableBlockEntities.add((ITickable) be);
                    }
                }
            }
        }
    }

    public boolean isGenerating() {
        return generating;
    }

    public void setBlock(int x, int y, int z, Block block) {
        setBlock(x, y, z, block, true);
    }

    public void setBlock(int x, int y, int z, Block block, boolean notifyAndLight) {
        BlockPos pos = new BlockPos(x, y, z);
        
        // Remove old block entity if it exists
        removeBlockEntity(pos);
        
        // IMPORTANT: Clear any damage/proxy data at this position immediately
        blockDamageMap.remove(pos);
        
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk != null) {
            int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
            int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, block);
            chunk.setNeedsMeshUpdate(true);
            
            // Update lighting (skip during world generation for performance)
            if (notifyAndLight && !generating) {
                lightEngine.updateBlockLight(pos);
                lightEngine.updateSunlight(pos);
            }
            
            // Handle block entities
            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
            // Automatically create block entity if defined
            BlockEntity be = def.createBlockEntity(pos);
            if (be != null) {
                setBlockEntity(be);
            }

            // Notify neighbors if block is on the edge
            if (localX == 0) notifyChunkUpdate(chunkPos.x() - 1, chunkPos.z());
            if (localX == Chunk.CHUNK_SIZE - 1) notifyChunkUpdate(chunkPos.x() + 1, chunkPos.z());
            if (localZ == 0) notifyChunkUpdate(chunkPos.x(), chunkPos.z() - 1);
            if (localZ == Chunk.CHUNK_SIZE - 1) notifyChunkUpdate(chunkPos.x(), chunkPos.z() + 1);

            // Notify all 6 neighbors about the block change for survival/logic updates
            if (notifyAndLight && !generating) {
                notifyNeighbors(pos);
            }
        }
    }

    /**
     * Уведомляет 6 соседних блоков об изменении в текущей позиции.
     * Это запускает логику выживания (requiresSupport) и другие обновления.
     */
    public void notifyNeighbors(BlockPos pos) {
        Block centerBlock = getBlock(pos);
        for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
            BlockPos neighborPos = dir.offset(pos);
            Block neighborBlock = getBlock(neighborPos);
            
            // Воздух не обрабатывает обновления
            if (neighborBlock.isAir()) continue;
            
            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(neighborBlock.getType());
            // Передаем направление от соседа к ИЗМЕНИВШЕМУСЯ блоку
            def.onNeighborChange(this, neighborPos, centerBlock, dir.getOpposite());
        }
    }

    /**
     * Вызывается игроком при завершении прогресса разрушения блока.
     * @return true если блок должен быть удален.
     */
    public boolean onBlockBreak(BlockPos pos, Player player) {
        Block block = getBlock(pos);
        if (block.isAir()) return true;

        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        return def.onBlockBreak(this, pos, block, player);
    }

    /**
     * Разрушает блок игроком. Вызывает хук onDestroyed.
     */
    public void destroyBlock(BlockPos pos, Player player) {
        Block block = getBlock(pos);
        if (block.isAir()) return;

        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        def.spawnDrops(this, pos, block, player);
        def.onDestroyed(this, pos, block, player);

        com.za.zenith.world.particles.ParticleManager.getInstance().spawnShatter(pos, block);

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
        com.za.zenith.world.chunks.Chunk chunk = getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
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
            com.za.zenith.world.chunks.Chunk chunk = getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
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
    
    public float getWorldTime() {
        return worldTime;
    }
    
    public int getSunlight(BlockPos pos) {
        return getSunlight(pos.x(), pos.y(), pos.z());
    }

    public int getSunlight(int x, int y, int z) {
        Chunk chunk = getChunkInternal(ChunkPos.fromBlockPos(x, z));
        if (chunk == null) return 0;
        return chunk.getSunlight(x & 15, y, z & 15);
    }

    public LightEngine getLightEngine() {
        return lightEngine;
    }

    public long getSeed() {
        return seed;
    }

    public int getBlockLight(BlockPos pos) {
        return getBlockLight(pos.x(), pos.y(), pos.z());
    }

    public int getBlockLight(int x, int y, int z) {
        Chunk chunk = getChunkInternal(ChunkPos.fromBlockPos(x, z));
        if (chunk == null) return 0;
        return chunk.getBlockLight(x & 15, y, z & 15);
    }

    public void setBlockLight(BlockPos pos, int level) {
        setBlockLight(pos.x(), pos.y(), pos.z(), level);
    }

    public void setBlockLight(int x, int y, int z, int level) {
        Chunk chunk = getChunk(ChunkPos.fromBlockPos(x, z));
        if (chunk != null) {
            chunk.setBlockLight(x & 15, y, z & 15, level);
        }
    }

    public void setBlock(int x, int y, int z, int blockType) {
        setBlock(x, y, z, new Block(blockType));
    }

    public void spawnItem(ItemStack stack, float x, float y, float z) {
        com.za.zenith.entities.ItemEntity entity = new com.za.zenith.entities.ItemEntity(new Vector3f(x, y, z), stack);
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
            if (be instanceof com.za.zenith.world.blocks.entity.GeneratorBlockEntity generator && generator.isRunning()) {
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

    public void cleanup() {
        chunkGenExecutor.shutdown();
        try {
            if (!chunkGenExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                chunkGenExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkGenExecutor.shutdownNow();
        }
    }
}


