package com.za.minecraft.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.za.minecraft.network.packets.*;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.engine.graphics.Camera;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GameClient {
    private static final int TIMEOUT = 5000;
    
    private Client client;
    private Map<String, RemotePlayer> remotePlayers;
    private String localPlayerId;
    private String localPlayerName;
    private boolean connected;
    private World world;
    private Player localPlayer;
    private Camera camera;
    private boolean serverDisconnected = false;
    
    public GameClient(World world, Player localPlayer, Camera camera, String playerName) {
        this.client = new Client();
        this.remotePlayers = new ConcurrentHashMap<>();
        this.world = world;
        this.localPlayer = localPlayer;
        this.camera = camera;
        this.localPlayerId = java.util.UUID.randomUUID().toString();
        this.localPlayerName = playerName;
        this.connected = false;
        
        registerPackets();
        setupListeners();
    }
    
    public boolean connect(String serverAddress) {
        try {
            client.start();
            client.connect(TIMEOUT, serverAddress, 25565, 25566);
            
            sendJoinPacket();
            connected = true;
            Logger.info("Connected to server at %s", serverAddress);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to connect to server %s: %s", e, serverAddress, e.getMessage());
            return false;
        }
    }
    
    public void disconnect() {
        if (connected) {
            PlayerLeavePacket leavePacket = new PlayerLeavePacket(localPlayerId, localPlayerName);
            client.sendTCP(leavePacket);
            
            connected = false; // Устанавливаем до остановки, чтобы избежать флага serverDisconnected
            client.stop();
            remotePlayers.clear();
            Logger.info("Disconnected from server");
        }
    }
    
    public void sendPlayerPosition() {
        if (!connected) return;
        
        var pos = localPlayer.getPosition();
        var rotation = camera.getRotation();
        
        PlayerPositionPacket packet = new PlayerPositionPacket(
            localPlayerId, localPlayerName,
            pos.x, pos.y, pos.z,
            rotation.x, rotation.y
        );
        
        client.sendUDP(packet);
    }
    
    public void sendBlockUpdate(int x, int y, int z, com.za.minecraft.world.blocks.BlockType blockType) {
        if (!connected) return;
        
        BlockUpdatePacket packet = new BlockUpdatePacket(x, y, z, blockType);
        client.sendTCP(packet);
    }
    
    public void sendChatMessage(String message) {
        if (!connected) return;
        
        ChatMessagePacket packet = new ChatMessagePacket(localPlayerId, localPlayerName, message);
        client.sendTCP(packet);
    }
    
    private void registerPackets() {
        var kryo = client.getKryo();
        kryo.register(NetworkPacket.class);
        kryo.register(PlayerPositionPacket.class);
        kryo.register(BlockUpdatePacket.class);
        kryo.register(PlayerJoinPacket.class);
        kryo.register(PlayerLeavePacket.class);
        kryo.register(ChatMessagePacket.class);
        // kryo.register(UUID.class); // Not needed anymore
        kryo.register(com.za.minecraft.world.blocks.BlockType.class);
    }
    
    private void setupListeners() {
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Logger.info("Connected to game server");
            }
            
            @Override
            public void disconnected(Connection connection) {
                Logger.info("Disconnected from game server");
                // Если мы еще считались подключенными, значит это принудительное отключение сервера
                if (connected) {
                    serverDisconnected = true;
                    Logger.error("Server disconnected! Game will close...");
                }
                connected = false;
                remotePlayers.clear();
            }
            
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerJoinPacket joinPacket) {
                    handlePlayerJoin(joinPacket);
                } else if (object instanceof PlayerLeavePacket leavePacket) {
                    handlePlayerLeave(leavePacket);
                } else if (object instanceof PlayerPositionPacket posPacket) {
                    handlePlayerPosition(posPacket);
                } else if (object instanceof BlockUpdatePacket blockPacket) {
                    handleBlockUpdate(blockPacket);
                } else if (object instanceof ChatMessagePacket chatPacket) {
                    handleChatMessage(chatPacket);
                }
            }
        });
    }
    
    private void sendJoinPacket() {
        var pos = localPlayer.getPosition();
        PlayerJoinPacket packet = new PlayerJoinPacket(
            localPlayerId, localPlayerName,
            pos.x, pos.y, pos.z,
            0 // World seed will be received from server
        );
        client.sendTCP(packet);
    }
    
    private void handlePlayerJoin(PlayerJoinPacket packet) {
        if (packet.playerId.equals(localPlayerId)) {
            Logger.info("Join confirmed by server. World seed: %d", packet.worldSeed);
            return;
        }
        
        RemotePlayer remotePlayer = new RemotePlayer(packet.playerId, packet.playerName);
        remotePlayer.setPosition(packet.spawnX, packet.spawnY, packet.spawnZ);
        
        remotePlayers.put(packet.playerId, remotePlayer);
        Logger.info("Player %s joined the game", packet.playerName);
    }
    
    private void handlePlayerLeave(PlayerLeavePacket packet) {
        RemotePlayer player = remotePlayers.remove(packet.playerId);
        if (player != null) {
            Logger.info("Player %s left the game", packet.playerName);
        }
    }
    
    private void handlePlayerPosition(PlayerPositionPacket packet) {
        if (packet.playerId.equals(localPlayerId)) return;
        
        RemotePlayer player = remotePlayers.get(packet.playerId);
        if (player != null) {
            player.setPosition(packet.x, packet.y, packet.z);
            player.setRotation(packet.yaw, packet.pitch);
        }
    }
    
    private void handleBlockUpdate(BlockUpdatePacket packet) {
        world.setBlock(packet.x, packet.y, packet.z, packet.blockType);
        Logger.debug("Block updated at (%d, %d, %d): %s", packet.x, packet.y, packet.z, packet.blockType.getName());
    }
    
    private void handleChatMessage(ChatMessagePacket packet) {
        if (!packet.senderId.equals(localPlayerId)) {
            Logger.info("[CHAT] %s: %s", packet.senderName, packet.message);
        }
    }
    
    public Map<String, RemotePlayer> getRemotePlayers() {
        return remotePlayers;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isServerDisconnected() {
        return serverDisconnected;
    }
    
    public static class RemotePlayer {
        public final String id;
        public final String name;
        private float x, y, z;
        private float yaw, pitch;
        
        public RemotePlayer(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void setRotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }
}
