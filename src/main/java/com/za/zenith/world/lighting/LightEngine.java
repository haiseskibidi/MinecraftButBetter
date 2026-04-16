package com.za.zenith.world.lighting;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.chunks.Chunk;

import java.util.LinkedList;
import java.util.Queue;

public class LightEngine {
    private final World world;

    public LightEngine(World world) {
        this.world = world;
    }

    private static class LightNode {
        int x, y, z;
        int level;

        LightNode(int x, int y, int z, int level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }
    }

    public void onBlockChanged(BlockPos pos) {
        updateBlockLight(pos);
        updateSunlight(pos);
    }

    public void updateBlockLight(BlockPos pos) {
        Queue<LightNode> fillQueue = new LinkedList<>();
        Queue<LightNode> removalQueue = new LinkedList<>();

        int oldLevel = getBlockLight(pos.x(), pos.y(), pos.z());
        int newLevel = calculateBlockLightSource(pos);

        if (newLevel > oldLevel) {
            fillQueue.add(new LightNode(pos.x(), pos.y(), pos.z(), newLevel));
            setBlockLight(pos.x(), pos.y(), pos.z(), newLevel);
        } else if (newLevel < oldLevel) {
            removalQueue.add(new LightNode(pos.x(), pos.y(), pos.z(), oldLevel));
            setBlockLight(pos.x(), pos.y(), pos.z(), 0);
        }

        processBlockLightRemoval(removalQueue, fillQueue);
        processBlockLightFill(fillQueue);
    }

    private int calculateBlockLightSource(BlockPos pos) {
        Block block = world.getBlock(pos);
        com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(block.getType());
        int emission = def.getEmission();
        
        // If it's a lamp, check if it's actually LIT via BlockEntity
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

    private void processBlockLightFill(Queue<LightNode> queue) {
        while (!queue.isEmpty()) {
            LightNode node = queue.poll();

            for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                int nx = node.x + dir.getDx();
                int ny = node.y + dir.getDy();
                int nz = node.z + dir.getDz();

                if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) continue;

                if (isOpaque(nx, ny, nz)) continue;

                int neighborLevel = getBlockLight(nx, ny, nz);
                if (neighborLevel < node.level - 1) {
                    setBlockLight(nx, ny, nz, node.level - 1);
                    queue.add(new LightNode(nx, ny, nz, node.level - 1));
                }
            }
        }
    }

    private void processBlockLightRemoval(Queue<LightNode> removalQueue, Queue<LightNode> fillQueue) {
        while (!removalQueue.isEmpty()) {
            LightNode node = removalQueue.poll();

            for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                int nx = node.x + dir.getDx();
                int ny = node.y + dir.getDy();
                int nz = node.z + dir.getDz();

                if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) continue;

                int neighborLevel = getBlockLight(nx, ny, nz);
                if (neighborLevel != 0 && neighborLevel < node.level) {
                    removalQueue.add(new LightNode(nx, ny, nz, neighborLevel));
                    setBlockLight(nx, ny, nz, 0);
                } else if (neighborLevel >= node.level) {
                    fillQueue.add(new LightNode(nx, ny, nz, neighborLevel));
                }
            }
        }
    }

    public void updateSunlight(BlockPos pos) {
        // Sunlight BFS is similar but needs to handle vertical columns for 15
        // For brevity and simplicity in v1, we focus on blocklight and basic sun
        // Full sunlight logic would involve recalculating columns
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
        return def != null && def.isSolid();
    }

    private boolean isOpaque(int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        return isOpaque(b);
    }
}
