package com.za.minecraft.network.packets;

public class PlayerJoinPacket extends NetworkPacket {
    public String playerId;
    public String playerName;
    public float spawnX, spawnY, spawnZ;
    public long worldSeed;
    
    public PlayerJoinPacket() {
    }
    
    public PlayerJoinPacket(String playerId, String playerName, float spawnX, float spawnY, float spawnZ, long worldSeed) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.worldSeed = worldSeed;
    }
}
