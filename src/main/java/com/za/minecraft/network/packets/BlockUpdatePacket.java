package com.za.minecraft.network.packets;

import com.za.minecraft.world.blocks.BlockType;

public class BlockUpdatePacket extends NetworkPacket {
    public int x, y, z;
    public BlockType blockType;
    public long timestamp;
    
    public BlockUpdatePacket() {
    }
    
    public BlockUpdatePacket(int x, int y, int z, BlockType blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.timestamp = System.currentTimeMillis();
    }
}
