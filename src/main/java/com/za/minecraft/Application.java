package com.za.minecraft;

import com.za.minecraft.engine.core.GameLoop;

public class Application {
    public static void main(String[] args) {
        GameLoop game = new GameLoop();
        
        if (args.length == 0) {
            System.out.println("=== MinecraftButBetter - Multiplayer Ready! ===");
            System.out.println("Usage examples:");
            System.out.println("  Single player:     java -jar game.jar singleplayer");
            System.out.println("  Host multiplayer:   java -jar game.jar host YourName");
            System.out.println("  Join multiplayer:   java -jar game.jar client YourName 192.168.1.100");
            System.out.println("Running in singleplayer mode...");
            game.runSingleplayer();
        } else if (args[0].equalsIgnoreCase("singleplayer")) {
            game.runSingleplayer();
        } else if (args[0].equalsIgnoreCase("host") && args.length >= 2) {
            String playerName = args[1];
            System.out.println("Starting as host with player name: " + playerName);
            System.out.println("Other players can connect to your IP address");
            game.runAsHost(playerName);
        } else if (args[0].equalsIgnoreCase("client") && args.length >= 3) {
            String playerName = args[1];
            String serverIP = args[2];
            System.out.println("Connecting to server " + serverIP + " as " + playerName);
            game.runAsClient(playerName, serverIP);
        } else {
            System.err.println("Invalid arguments!");
            System.err.println("Usage: java -jar game.jar [singleplayer|host <name>|client <name> <server_ip>]");
            System.exit(1);
        }
    }
}
