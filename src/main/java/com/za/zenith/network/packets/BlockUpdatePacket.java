package com.za.zenith.network.packets;

public class BlockUpdatePacket extends NetworkPacket {
    public int x, y, z;
    public int blockType;
    public long timestamp;
    
    public BlockUpdatePacket() {
    }
    
    public BlockUpdatePacket(int x, int y, int z, int blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.timestamp = System.currentTimeMillis();
    }
}


