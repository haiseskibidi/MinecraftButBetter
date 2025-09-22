package com.za.minecraft.network.packets;

public class PlayerPositionPacket extends NetworkPacket {
    public String playerId;
    public String playerName;
    public float x, y, z;
    public float yaw, pitch;
    public long timestamp;
    
    public PlayerPositionPacket() {
    }
    
    public PlayerPositionPacket(String playerId, String playerName, float x, float y, float z, float yaw, float pitch) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timestamp = System.currentTimeMillis();
    }
}
