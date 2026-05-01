package com.za.zenith;

import com.za.zenith.engine.core.GameLoop;

public class Application {
    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        GameLoop game = new GameLoop();
        
        if (args.length == 0) {
            System.out.println("=== zenithButBetter - Multiplayer Ready! ===");
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
        
        System.exit(0);
    }
}


