package com.za.minecraft.network.packets;

public class ChatMessagePacket extends NetworkPacket {
    public String senderId;
    public String senderName;
    public String message;
    public long timestamp;
    
    public ChatMessagePacket() {
    }
    
    public ChatMessagePacket(String senderId, String senderName, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
