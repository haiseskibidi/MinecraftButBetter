package com.za.minecraft.network.packets;

public class PlayerLeavePacket extends NetworkPacket {
    public String playerId;
    public String playerName;
    
    public PlayerLeavePacket() {
    }
    
    public PlayerLeavePacket(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }
}
