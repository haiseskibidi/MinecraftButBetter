package com.za.zenith.world.lighting;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.chunks.Chunk;

public class LightEngine {
    private final World world;
    
    // Circular queue for long-packed nodes: [x:25 | y:10 | z:25 | val:4]
    private static final int QUEUE_SIZE = 1024 * 1024; // 1M nodes
    private static final int QUEUE_MASK = QUEUE_SIZE - 1;
    private final long[] fillQueue = new long[QUEUE_SIZE];
    private final long[] removalQueue = new long[QUEUE_SIZE];
    private int fillHead, fillTail;
    private int removalHead, removalTail;

    public LightEngine(World world) {
        this.world = world;
    }

    private long pack(int x, int y, int z, int val) {
        return ((long) (x & 0x1FFFFFF) << 39) |
               ((long) (y & 0x3FF) << 29) |
               ((long) (z & 0x1FFFFFF) << 4) |
               (long) (val & 0xF);
    }

    private int unpackX(long p) {
        int x = (int) (p >> 39);
        if ((x & 0x1000000) != 0) x |= 0xFE000000;
        return x;
    }

    private int unpackY(long p) {
        return (int) ((p >> 29) & 0x3FF);
    }

    private int unpackZ(long p) {
        int z = (int) ((p >> 4) & 0x1FFFFFF);
        if ((z & 0x1000000) != 0) z |= 0xFE000000;
        return z;
    }

    private int unpackVal(long p) {
        return (int) (p & 0xF);
    }

    public void onBlockChanged(BlockPos pos) {
        updateBlockLight(pos);
        updateSunlight(pos);
    }

    public void updateBlockLight(BlockPos pos) {
        fillHead = fillTail = 0;
        removalHead = removalTail = 0;

        int oldLevel = getBlockLight(pos.x(), pos.y(), pos.z());
        int newLevel = calculateBlockLightSource(pos);

        if (newLevel > oldLevel) {
            setBlockLight(pos.x(), pos.y(), pos.z(), newLevel);
            enqueueFill(pack(pos.x(), pos.y(), pos.z(), newLevel));
        } else if (newLevel < oldLevel) {
            setBlockLight(pos.x(), pos.y(), pos.z(), 0);
            enqueueRemoval(pack(pos.x(), pos.y(), pos.z(), oldLevel));
        }

        processLightRemoval(false);
        processLightFill(false);
    }

    private int calculateBlockLightSource(BlockPos pos) {
        Block block = world.getBlock(pos);
        com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(block.getType());
        if (def == null) return 0;
        int emission = def.getEmission();
        
        if (def.getIdentifier().toString().contains("lamp")) {
            com.za.zenith.world.blocks.entity.BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof com.za.zenith.world.blocks.entity.LampBlockEntity) {
                return world.getBlockLight(pos); 
            }
        }
        
        if (emission > 0) return emission;

        int maxNeighbor = 0;
        for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
            maxNeighbor = Math.max(maxNeighbor, getBlockLight(pos.x() + dir.getDx(), pos.y() + dir.getDy(), pos.z() + dir.getDz()) - 1);
        }
        return Math.max(0, maxNeighbor);
    }

    public void updateSunlight(BlockPos pos) {
        fillHead = fillTail = 0;
        removalHead = removalTail = 0;

        int x = pos.x();
        int z = pos.z();
        
        // Recalculate vertical column and enqueue changes
        int light = 15;
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block b = world.getBlock(x, y, z);
            if (isOpaque(b)) {
                light = 0;
            }
            
            int old = getSunlight(x, y, z);
            if (old != light) {
                if (light > old) {
                    setSunlight(x, y, z, light);
                    enqueueFill(pack(x, y, z, light));
                } else {
                    setSunlight(x, y, z, 0);
                    enqueueRemoval(pack(x, y, z, old));
                }
            } else if (light == 0) {
                // If we reached opaque blocks and light is already 0, we might still need to 
                // check for horizontal light bleeding if this was the block that changed.
                // But generally vertical scan stops here.
                if (y < pos.y()) break;
            }
        }

        processLightRemoval(true);
        processLightFill(true);
    }

    private void processLightFill(boolean sun) {
        while (fillHead != fillTail) {
            long p = dequeueFill();
            int x = unpackX(p);
            int y = unpackY(p);
            int z = unpackZ(p);
            int level = unpackVal(p);

            for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                int nx = x + dir.getDx();
                int ny = y + dir.getDy();
                int nz = z + dir.getDz();

                if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) continue;
                
                Chunk nChunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(nx, nz));
                if (nChunk == null) continue;

                if (isOpaque(nChunk, nx & 15, ny, nz & 15)) continue;

                int neighborLevel = sun ? nChunk.getSunlight(nx & 15, ny, nz & 15) : nChunk.getBlockLight(nx & 15, ny, nz & 15);
                
                // Sunlight vertical propagation: 15 stays 15 when going down
                int nextLevel = (sun && dir == com.za.zenith.utils.Direction.DOWN && level == 15) ? 15 : level - 1;
                if (nextLevel < 0) nextLevel = 0;

                if (neighborLevel < nextLevel) {
                    if (sun) nChunk.setSunlight(nx & 15, ny, nz & 15, nextLevel);
                    else nChunk.setBlockLight(nx & 15, ny, nz & 15, nextLevel);
                    enqueueFill(pack(nx, ny, nz, nextLevel));
                }
            }
        }
    }

    private void processLightRemoval(boolean sun) {
        while (removalHead != removalTail) {
            long p = dequeueRemoval();
            int x = unpackX(p);
            int y = unpackY(p);
            int z = unpackZ(p);
            int level = unpackVal(p);

            for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                int nx = x + dir.getDx();
                int ny = y + dir.getDy();
                int nz = z + dir.getDz();

                if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) continue;
                
                Chunk nChunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(nx, nz));
                if (nChunk == null) continue;

                int neighborLevel = sun ? nChunk.getSunlight(nx & 15, ny, nz & 15) : nChunk.getBlockLight(nx & 15, ny, nz & 15);
                
                int expectedLevel = (sun && dir == com.za.zenith.utils.Direction.DOWN && level == 15) ? 15 : level - 1;
                if (expectedLevel < 0) expectedLevel = 0;

                if (neighborLevel != 0 && neighborLevel <= expectedLevel) {
                    // Special case for sunlight: Level 15 should not be removed by horizontal neighbors
                    // as it can only be 15 if it has a vertical source.
                    if (sun && neighborLevel == 15 && dir != com.za.zenith.utils.Direction.DOWN) {
                        enqueueFill(pack(nx, ny, nz, 15));
                        continue;
                    }

                    if (sun) nChunk.setSunlight(nx & 15, ny, nz & 15, 0);
                    else nChunk.setBlockLight(nx & 15, ny, nz & 15, 0);
                    enqueueRemoval(pack(nx, ny, nz, neighborLevel));
                } else if (neighborLevel > 0) {
                    enqueueFill(pack(nx, ny, nz, neighborLevel));
                }
            }
        }
    }

    private boolean isOpaque(Chunk chunk, int lx, int ly, int lz) {
        Block b = chunk.getBlock(lx, ly, lz);
        if (b.getType() == 0) return false;
        com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(b.getType());
        return def != null && def.isSolid() && !def.isTransparent();
    }

    private void enqueueFill(long p) {
        fillQueue[fillTail] = p;
        fillTail = (fillTail + 1) & QUEUE_MASK;
    }

    private long dequeueFill() {
        long p = fillQueue[fillHead];
        fillHead = (fillHead + 1) & QUEUE_MASK;
        return p;
    }

    private void enqueueRemoval(long p) {
        removalQueue[removalTail] = p;
        removalTail = (removalTail + 1) & QUEUE_MASK;
    }

    private long dequeueRemoval() {
        long p = removalQueue[removalHead];
        removalHead = (removalHead + 1) & QUEUE_MASK;
        return p;
    }

    private int getSunlight(int x, int y, int z) {
        Chunk chunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z));
        if (chunk == null) return 15; // Assume bright for unloaded
        return chunk.getSunlight(x & 15, y, z & 15);
    }

    private void setSunlight(int x, int y, int z, int level) {
        Chunk chunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z));
        if (chunk != null) {
            chunk.setSunlight(x & 15, y, z & 15, level);
        }
    }

    private int getBlockLight(int x, int y, int z) {
        Chunk chunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z));
        if (chunk == null) return 0;
        return chunk.getBlockLight(x & 15, y, z & 15);
    }

    private void setBlockLight(int x, int y, int z, int level) {
        Chunk chunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z));
        if (chunk != null) {
            chunk.setBlockLight(x & 15, y, z & 15, level);
        }
    }

    public void generateInitialSunlight(Chunk chunk) {
        // Just do vertical scan, the world will trigger BFS if needed 
        // or we can just leave it to the world generator to call updateSunlight
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int light = 15;
                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block b = chunk.getBlock(x, y, z);
                    if (isOpaque(b)) {
                        light = 0;
                    }
                    chunk.setSunlight(x, y, z, light);
                }
            }
        }
    }

    private boolean isOpaque(Block b) {
        if (b.getType() == 0) return false;
        com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(b.getType());
        return def != null && def.isSolid() && !def.isTransparent();
    }
}
