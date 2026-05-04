package com.za.zenith.world.generation.caves;

import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.World;
import com.za.zenith.world.chunks.Chunk;

import java.util.Random;

public class WoodenBeamDecorator implements ICaveDecorator {
    private static final int CHANCE = 100; // 1 in 100 per column

    @Override
    public void decorate(World world, Chunk chunk) {
        BlockDefinition beamBlock = BlockRegistry.getRegistry().get(Identifier.of("zenith:oak_log"));
        if (beamBlock == null) return;
        
        int startX = chunk.getPosition().x() * Chunk.CHUNK_SIZE;
        int startZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE;
        Random random = new Random(chunk.getPosition().x() * 341873128712L + chunk.getPosition().z() * 132897987541L);

        for (int x = 2; x < Chunk.CHUNK_SIZE - 2; x++) {
            for (int z = 2; z < Chunk.CHUNK_SIZE - 2; z++) {
                if (random.nextInt(CHANCE) != 0) continue;

                // Search for suitable tunnel (logical -100 to 0) -> internal 28 to 128
                for (int y = 28; y < 128; y++) {
                    int id = chunk.getBlockType(x, y, z);
                    if (id == 0) { // Air
                        int floor = y - 1;
                        int ceil = y + 1;
                        while (ceil < Chunk.CHUNK_HEIGHT && chunk.getBlockType(x, ceil, z) == 0) ceil++;
                        
                        int height = ceil - floor - 1;
                        if (height >= 3 && height <= 5 && isSolid(chunk, x, floor, z) && isSolid(chunk, x, ceil, z)) {
                            // Check width in X
                            int left = x - 1;
                            while (left >= 0 && chunk.getBlockType(left, y, z) == 0) left--;
                            int right = x + 1;
                            while (right < Chunk.CHUNK_SIZE && chunk.getBlockType(right, y, z) == 0) right++;
                            
                            int widthX = right - left - 1;
                            if (widthX >= 3 && widthX <= 5) {
                                buildBeamX(chunk, left + 1, right - 1, floor + 1, ceil - 1, z, beamBlock);
                                y = ceil + 1;
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSolid(Chunk chunk, int x, int y, int z) {
        int id = chunk.getBlockType(x, y, z);
        return id != 0;
    }

    private void buildBeamX(Chunk chunk, int startX, int endX, int floorY, int ceilY, int z, BlockDefinition beamBlock) {
        int id = beamBlock.getId();
        // Pillars
        for (int y = floorY; y <= ceilY; y++) {
            chunk.setBlock(startX, y, z, id, 0);
            chunk.setBlock(endX, y, z, id, 0);
        }
        // Crossbeam
        for (int x = startX + 1; x <= endX - 1; x++) {
            chunk.setBlock(x, ceilY, z, id, 0);
        }
    }
}