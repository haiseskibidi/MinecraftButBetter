package com.za.minecraft.world.generation;

import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
import java.util.Random;

public class TreeGenerator {
    private final Random random;
    
    public TreeGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    public boolean canGenerateTree(World world, int x, int y, int z) {
        // Check if there's grass block at surface
        Block groundBlock = world.getBlock(x, y - 1, z);
        if (groundBlock.getType() != BlockType.GRASS) {
            return false;
        }
        
        // Check if there's enough space above (at least 6 blocks)
        for (int dy = 0; dy < 6; dy++) {
            Block block = world.getBlock(x, y + dy, z);
            if (!block.isAir()) {
                return false;
            }
        }
        
        return true;
    }
    
    public void generateOakTree(World world, int x, int y, int z) {
        if (!canGenerateTree(world, x, y, z)) {
            return;
        }
        
        // Tree height: 4-6 blocks
        int height = 4 + random.nextInt(3);
        
        // Generate trunk
        for (int dy = 0; dy < height; dy++) {
            world.setBlock(new BlockPos(x, y + dy, z), new Block(BlockType.WOOD, Block.Axis.Y));
        }
        
        // Generate leaves crown
        int crownY = y + height - 1;
        
        // Top layer (single block)
        world.setBlock(new BlockPos(x, crownY + 1, z), new Block(BlockType.LEAVES));
        
        // Main crown layers
        for (int layer = 0; layer < 2; layer++) {
            int currentY = crownY - layer;
            int radius = layer == 0 ? 2 : 2;
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue; // Skip trunk
                    
                    // Skip corners for more natural look
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        if (random.nextFloat() < 0.5f) continue;
                    }
                    
                    // Random gaps for natural look
                    if (random.nextFloat() < 0.15f) continue;
                    
                    BlockPos leafPos = new BlockPos(x + dx, currentY, z + dz);
                    Block existingBlock = world.getBlock(leafPos);
                    if (existingBlock.isAir()) {
                        world.setBlock(leafPos, new Block(BlockType.LEAVES));
                    }
                }
            }
        }
        
        // Bottom layer (smaller)
        int bottomY = crownY - 2;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip trunk
                
                // Random gaps
                if (random.nextFloat() < 0.3f) continue;
                
                BlockPos leafPos = new BlockPos(x + dx, bottomY, z + dz);
                Block existingBlock = world.getBlock(leafPos);
                if (existingBlock.isAir()) {
                    world.setBlock(leafPos, new Block(BlockType.LEAVES));
                }
            }
        }
    }
    
    public void generateBirchTree(World world, int x, int y, int z) {
        if (!canGenerateTree(world, x, y, z)) {
            return;
        }
        
        // Birch trees are taller and thinner
        int height = 5 + random.nextInt(3);
        
        // Generate trunk (using wood blocks for now - could add birch wood later)
        for (int dy = 0; dy < height; dy++) {
            world.setBlock(new BlockPos(x, y + dy, z), new Block(BlockType.WOOD, Block.Axis.Y));
        }
        
        // Smaller, more compact crown
        int crownY = y + height - 1;
        
        // Top
        world.setBlock(new BlockPos(x, crownY + 1, z), new Block(BlockType.LEAVES));
        
        // Main crown (smaller than oak)
        for (int layer = 0; layer < 2; layer++) {
            int currentY = crownY - layer;
            int radius = 1;
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    
                    if (random.nextFloat() < 0.1f) continue;
                    
                    BlockPos leafPos = new BlockPos(x + dx, currentY, z + dz);
                    Block existingBlock = world.getBlock(leafPos);
                    if (existingBlock.isAir()) {
                        world.setBlock(leafPos, new Block(BlockType.LEAVES));
                    }
                }
            }
        }
    }
    
    public void tryGenerateTree(World world, int x, int y, int z, double density) {
        // Use seeded random for consistent generation
        Random treeRandom = new Random(((long)x << 32) | ((long)z & 0xFFFFFFFF) ^ random.nextLong());
        
        if (treeRandom.nextDouble() < density) {
            // 80% oak, 20% birch
            if (treeRandom.nextFloat() < 0.8f) {
                generateOakTree(world, x, y, z);
            } else {
                generateBirchTree(world, x, y, z);
            }
        }
    }
}
