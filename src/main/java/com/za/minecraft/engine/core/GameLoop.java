package com.za.minecraft.engine.core;

import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.Renderer;
import com.za.minecraft.engine.input.InputManager;
import com.za.minecraft.entities.Player;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.network.GameServer;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.physics.RaycastResult;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.opengl.GL11.*;

public class GameLoop {
    private static final int TARGET_FPS = 300;
    private static final int TARGET_UPS = 170;
    
    private Window window;
    private Timer timer;
    private Camera camera;
    private InputManager inputManager;
    private Renderer renderer;
    private World world;
    private Player player;
    private boolean running;
    
    private GameMode gameMode;
    private GameServer localServer;
    private GameClient networkClient;
    private String playerName;
    private String serverAddress;
    
    private int fpsCounter = 0;
    private float currentFps = 0;
    private float fpsTimer = 0;
    private float debugTimer = 0;
    private RaycastResult highlightedBlock;
    
    public void runSingleplayer() {
        runWithMode(GameMode.SINGLEPLAYER, "Player", null);
    }
    
    public void runAsHost(String playerName) {
        runWithMode(GameMode.MULTIPLAYER_HOST, playerName, null);
    }
    
    public void runAsClient(String playerName, String serverAddress) {
        runWithMode(GameMode.MULTIPLAYER_CLIENT, playerName, serverAddress);
    }
    
    private void runWithMode(GameMode mode, String playerName, String serverAddress) {
        this.gameMode = mode;
        this.playerName = playerName;
        this.serverAddress = serverAddress;
        
        Logger.info("Starting game in %s mode...", mode);
        
        try {
            init();
            gameLoop();
        } catch (Exception e) {
            Logger.error("Game crashed: %s", e, e.getMessage());
        } finally {
            cleanup();
        }
        
        Logger.info("Game stopped");
    }
    
    @Deprecated
    public void run() {
        runSingleplayer();
    }
    
    private void init() {
        timer = new Timer();
        window = new Window("MinecraftButBetter - v0.1", 1600, 800, true); // Enable VSync for stability
        window.init();
        
        world = new World();
        
        // Find a good spawn location on the generated terrain
        Vector3f spawnPos = findSpawnLocation(world);
        player = new Player(spawnPos);
        
        camera = new Camera(new Vector3f(spawnPos.x, spawnPos.y + 1.62f, spawnPos.z));
        
        Logger.info("Player spawned at: (%.1f, %.1f, %.1f)", spawnPos.x, spawnPos.y, spawnPos.z);
        camera.updateAspectRatio(window.getAspectRatio());
        
        inputManager = new InputManager();
        inputManager.init(window);
        
        renderer = new Renderer();
        renderer.init(window.getWidth(), window.getHeight());
        
        setupMultiplayer();
        
        running = true;
        
        Logger.info("Game initialized");
    }
    
    private void setupMultiplayer() {
        if (gameMode == GameMode.MULTIPLAYER_HOST) {
            localServer = new GameServer(world.getSeed());
            if (localServer.start()) {
                Logger.info("Local server started successfully");
                
                networkClient = new GameClient(world, player, camera, playerName);
                if (networkClient.connect("127.0.0.1")) {
                    Logger.info("Connected to local server");
                } else {
                    Logger.error("Failed to connect to local server");
                    running = false;
                }
            } else {
                Logger.error("Failed to start local server");
                running = false;
            }
        } else if (gameMode == GameMode.MULTIPLAYER_CLIENT) {
            networkClient = new GameClient(world, player, camera, playerName);
            if (!networkClient.connect(serverAddress)) {
                Logger.error("Failed to connect to server at %s", serverAddress);
                running = false;
            }
        }
    }
    
    private void gameLoop() {
        float accumulator = 0f;
        float interval = 1f / TARGET_UPS;
        
        while (running && !window.shouldClose()) {
            // Проверяем отключение сервера в мультиплеере
            if (networkClient != null && networkClient.isServerDisconnected()) {
                Logger.error("Server disconnected, closing game...");
                running = false;
                break;
            }
            
            timer.updateDelta();
            accumulator += timer.getDeltaF();
            
            input();
            
            while (accumulator >= interval) {
                update(interval);
                accumulator -= interval;
            }
            
            render();
            sync();
        }
    }
    
    private void input() {
        // if (window.isKeyPressed(GLFW_KEY_ESCAPE)) {
        //     running = false;
        // }
        
        // Debug: показываем позицию игрока каждые 5 секунд
        debugTimer += timer.getDeltaF();
        if (debugTimer >= 5.0f) {
            debugTimer = 0;
            Vector3f pos = player.getPosition();
            Logger.info("Player position: (%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z);
            
            // Показываем что под ногами
            Block groundBlock = world.getBlock((int)pos.x, (int)pos.y - 1, (int)pos.z);
            Logger.info("  Ground block: %s", groundBlock.isAir() ? "AIR" : groundBlock.getType().getName());
        }
        
        highlightedBlock = inputManager.input(window, camera, player, timer.getDeltaF(), renderer, world, networkClient);
    }
    
    private void update(float interval) {
        player.update(interval, world);
        
        if (networkClient != null && networkClient.isConnected()) {
            networkClient.sendPlayerPosition();
        }
    }
    
    private void render() {
        renderer.render(window, camera, world, highlightedBlock, networkClient);
        
        // Подсчет FPS
        updateFPS();
        // renderer.renderDebug(currentFps, window.getWidth(), window.getHeight()); // Временно отключено
        
        window.update();
    }
    
    private void updateFPS() {
        fpsCounter++;
        fpsTimer += timer.getDeltaF();
        
        if (fpsTimer >= 1.0f) {
            currentFps = fpsCounter;
            fpsCounter = 0;
            fpsTimer = 0;
            
            // Временно выводим FPS в заголовок окна
            String fxaaStatus = renderer.isFXAAEnabled() ? "ON" : "OFF";
            String dir = getCompassDirection();
            window.setTitle("MinecraftButBetter - v0.1 | FPS: " + (int)currentFps + " | FOV: 80° | FXAA: " + fxaaStatus + " | Dir: " + dir);
        }
    }

    private String getCompassDirection() {
        if (camera == null) return "?";
        org.joml.Vector3f rot = camera.getRotation();
        org.joml.Vector3f dir = new org.joml.Vector3f(0, 0, -1)
            .rotateX(rot.x)
            .rotateY(rot.y);
        // Проекция на горизонтальную плоскость
        float angle = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z));
        if (angle < 0) angle += 360f;
        // Квантование в 4 сектора
        if (angle >= 45 && angle < 135) return "Восток";
        if (angle >= 135 && angle < 225) return "Юг";
        if (angle >= 225 && angle < 315) return "Запад";
        return "Север";
    }
    
    private Vector3f findSpawnLocation(World world) {
        // Try to find a good spawn location in a spiral pattern
        int maxRadius = 32; // Search within 32 blocks radius
        
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int angle = 0; angle < 360; angle += 15) { // Check every 15 degrees
                int x = (int)(radius * Math.cos(Math.toRadians(angle)));
                int z = (int)(radius * Math.sin(Math.toRadians(angle)));
                
                // Find surface height at this position
                float surfaceY = findSurfaceHeight(world, x, z);
                if (surfaceY > 0) {
                    // Check if there's enough space for player (2 blocks high)
                    Block above1 = world.getBlock(x, (int)surfaceY + 1, z);
                    Block above2 = world.getBlock(x, (int)surfaceY + 2, z);
                    
                    if (above1.isAir() && above2.isAir()) {
                        Logger.info("Found spawn location at (%d, %.1f, %d) after checking radius %d", x, surfaceY, z, radius);
                        return new Vector3f(x + 0.5f, surfaceY, z + 0.5f); // Center of block
                    }
                }
            }
        }
        
        // Fallback - spawn at center with default height
        Logger.warn("Could not find suitable spawn location, using default");
        return new Vector3f(0.5f, 80.0f, 0.5f);
    }
    
    private float findSurfaceHeight(World world, int x, int z) {
        // Find the highest solid block
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block block = world.getBlock(x, y, z);
            if (!block.isAir()) {
                // Make sure it's a good surface block (not leaves in trees)
                if (block.getType() == com.za.minecraft.world.blocks.BlockType.GRASS || 
                    block.getType() == com.za.minecraft.world.blocks.BlockType.DIRT ||
                    block.getType() == com.za.minecraft.world.blocks.BlockType.STONE) {
                    return y + 1.0f; // Spawn one block above the surface
                }
            }
        }
        return -1; // No suitable surface found
    }
    
    private void sync() {
        // No artificial FPS limiting for maximum performance
    }
    
    private void cleanup() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        if (localServer != null) {
            localServer.stop();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        if (window != null) {
            window.cleanup();
        }
    }
}
