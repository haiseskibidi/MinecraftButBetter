package com.za.minecraft.world;

import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.chunks.ChunkPos;
import com.za.minecraft.world.generation.TerrainGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class World {
    private final Map<ChunkPos, Chunk> chunks;
    private final TerrainGenerator terrainGenerator;
    
    public World() {
        this.chunks = new ConcurrentHashMap<>();
        long randomSeed = System.currentTimeMillis(); // Random seed each time
        com.za.minecraft.utils.Logger.info("Generating new world with seed: %d", randomSeed);
        this.terrainGenerator = new TerrainGenerator(randomSeed);
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
        com.za.minecraft.utils.Logger.info("World generation completed!");
    }
    
    public Block getBlock(BlockPos pos) {
        return getBlock(pos.x(), pos.y(), pos.z());
    }
    
    public Block getBlock(int x, int y, int z) {
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk == null) {
            return new Block(BlockType.AIR);
        }
        
        int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
        int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
        
        return chunk.getBlock(localX, y, localZ);
    }
    
    public void setBlock(BlockPos pos, Block block) {
        setBlock(pos.x(), pos.y(), pos.z(), block);
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        ChunkPos chunkPos = ChunkPos.fromBlockPos(x, z);
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk != null) {
            int localX = x - chunkPos.x() * Chunk.CHUNK_SIZE;
            int localZ = z - chunkPos.z() * Chunk.CHUNK_SIZE;
            chunk.setBlock(localX, y, localZ, block);
        }
    }
    
    public Iterable<Chunk> getLoadedChunks() {
        return chunks.values();
    }
    
    public Chunk getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }
}
