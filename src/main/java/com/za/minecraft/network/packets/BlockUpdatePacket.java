package com.za.minecraft.network.packets;

public class BlockUpdatePacket extends NetworkPacket {
    public int x, y, z;
    public byte blockType;
    public long timestamp;
    
    public BlockUpdatePacket() {
    }
    
    public BlockUpdatePacket(int x, int y, int z, byte blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.timestamp = System.currentTimeMillis();
    }
}
