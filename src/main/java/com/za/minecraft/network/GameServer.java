package com.za.minecraft.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.za.minecraft.network.packets.*;
import com.za.minecraft.utils.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
// import java.util.UUID; // Not needed

public class GameServer {
    private static final int TCP_PORT = 25565;
    private static final int UDP_PORT = 25566;
    private static final int MAX_PLAYERS = 10;
    
    private Server server;
    private Map<Integer, PlayerInfo> connectedPlayers;
    private long worldSeed;
    private boolean running;
    
    public GameServer(long worldSeed) {
        this.worldSeed = worldSeed;
        this.connectedPlayers = new ConcurrentHashMap<>();
        this.server = new Server();
        
        registerPackets();
        setupListeners();
    }
    
    public boolean start() {
        try {
            server.start();
            server.bind(TCP_PORT, UDP_PORT);
            running = true;
            Logger.info("Game server started on port %d (TCP) / %d (UDP)", TCP_PORT, UDP_PORT);
            Logger.info("Max players: %d", MAX_PLAYERS);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to start server: %s", e, e.getMessage());
            return false;
        }
    }
    
    public void stop() {
        if (running) {
            running = false;
            server.stop();
            Logger.info("Game server stopped");
        }
    }
    
    private void registerPackets() {
        var kryo = server.getKryo();
        kryo.register(NetworkPacket.class);
        kryo.register(PlayerPositionPacket.class);
        kryo.register(BlockUpdatePacket.class);
        kryo.register(PlayerJoinPacket.class);
        kryo.register(PlayerLeavePacket.class);
        kryo.register(ChatMessagePacket.class);
        // kryo.register(UUID.class); // Not needed
        kryo.register(com.za.minecraft.world.blocks.BlockType.class);
    }
    
    private void setupListeners() {
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Logger.info("Player connected: %s", connection.getRemoteAddressTCP());
                
                if (connectedPlayers.size() >= MAX_PLAYERS) {
                    Logger.warn("Server full, disconnecting player %s", connection.getRemoteAddressTCP());
                    connection.close();
                    return;
                }
            }
            
            @Override
            public void disconnected(Connection connection) {
                PlayerInfo player = connectedPlayers.remove(connection.getID());
                if (player != null) {
                    Logger.info("Player %s disconnected", player.name);
                    
                    PlayerLeavePacket leavePacket = new PlayerLeavePacket(player.id, player.name);
                    server.sendToAllExceptTCP(connection.getID(), leavePacket);
                }
            }
            
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerJoinPacket joinPacket) {
                    handlePlayerJoin(connection, joinPacket);
                } else if (object instanceof PlayerPositionPacket posPacket) {
                    server.sendToAllExceptUDP(connection.getID(), posPacket);
                } else if (object instanceof BlockUpdatePacket blockPacket) {
                    server.sendToAllExceptTCP(connection.getID(), blockPacket);
                } else if (object instanceof ChatMessagePacket chatPacket) {
                    server.sendToAllTCP(chatPacket);
                    Logger.info("[CHAT] %s: %s", chatPacket.senderName, chatPacket.message);
                }
            }
        });
    }
    
    private void handlePlayerJoin(Connection connection, PlayerJoinPacket joinPacket) {
        PlayerInfo newPlayer = new PlayerInfo(joinPacket.playerId, joinPacket.playerName);
        connectedPlayers.put(connection.getID(), newPlayer);
        
        Logger.info("Player %s (%s) joined the game", newPlayer.name, newPlayer.id);
        
        PlayerJoinPacket responsePacket = new PlayerJoinPacket(
            newPlayer.id, newPlayer.name, 
            joinPacket.spawnX, joinPacket.spawnY, joinPacket.spawnZ, 
            worldSeed
        );
        connection.sendTCP(responsePacket);
        
        server.sendToAllExceptTCP(connection.getID(), joinPacket);
        
        for (PlayerInfo existingPlayer : connectedPlayers.values()) {
            if (!existingPlayer.id.equals(newPlayer.id)) {
                PlayerJoinPacket existingPlayerPacket = new PlayerJoinPacket(
                    existingPlayer.id, existingPlayer.name,
                    0, 0, 0, worldSeed
                );
                connection.sendTCP(existingPlayerPacket);
            }
        }
    }
    
    public int getConnectedPlayersCount() {
        return connectedPlayers.size();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private static class PlayerInfo {
        public final String id;
        public final String name;
        
        public PlayerInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
