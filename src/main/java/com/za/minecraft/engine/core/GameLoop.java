package com.za.minecraft.engine.core;

import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.Renderer;
import com.za.minecraft.engine.graphics.ui.Hotbar;
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

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class GameLoop {
    private static GameLoop instance;
    private static final int TARGET_FPS = 300;
    private static final int TARGET_UPS = 170;
    
    private Window window;
    private Timer timer;
    private Camera camera;
    private InputManager inputManager;
    private Renderer renderer;
    private World world;
    private Player player;
    private Hotbar hotbar;
    private com.za.minecraft.engine.graphics.ui.PauseMenu pauseMenu;
    private boolean running;
    private boolean paused = false;
    private boolean inventoryOpen = false;
    private boolean ePressed = false;
    private boolean escPressed = false;
    
    private GameMode gameMode;
    private GameServer localServer;
    private GameClient networkClient;
    private String playerName;
    private String serverAddress;
    
    private int fpsCounter = 0;
    private float currentFps = 0;
    private float fpsTimer = 0;
    private RaycastResult highlightedBlock;
    
    public GameLoop() {
        instance = this;
    }
    
    public static GameLoop getInstance() {
        return instance;
    }
    
    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public Camera getCamera() {
        return camera;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public void setInventoryOpen(boolean open) {
        this.inventoryOpen = open;
    }

    public void runSingleplayer() {
        runWithMode(GameMode.SINGLEPLAYER, "Player", null);
    }
    
    public void runAsHost(String name) {
        runWithMode(GameMode.MULTIPLAYER_HOST, name, null);
    }
    
    public void runAsClient(String name, String address) {
        runWithMode(GameMode.MULTIPLAYER_CLIENT, name, address);
    }
    
    private void runWithMode(GameMode mode, String name, String address) {
        this.gameMode = mode;
        this.playerName = name;
        this.serverAddress = address;
        
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        window = new Window("Protocol: Grounding", 1280, 720, true);
        window.init();
        
        timer = new Timer();
        camera = new Camera(new Vector3f(8, 65, 8));
        camera.updateAspectRatio(window.getAspectRatio());
        
        inputManager = new InputManager();
        renderer = new Renderer();
        
        inputManager.init(window);
        renderer.init(window.getWidth(), window.getHeight());
        
        long seed = 12345;
        
        if (gameMode == GameMode.MULTIPLAYER_HOST) {
            localServer = new GameServer(seed);
            localServer.start();
        }
        
        world = new World(seed);
        player = new Player(new Vector3f(8, 65, 8));
        world.setPlayer(player);
        
        // Give Admin Hammer to dev
        player.getInventory().setStackInSlot(0, new com.za.minecraft.world.items.ItemStack(com.za.minecraft.world.items.ItemRegistry.getItem(com.za.minecraft.world.items.ItemType.ADMIN_HAMMER)));
        
        hotbar = new Hotbar(player);
        renderer.setHotbar(hotbar);
        
        pauseMenu = new com.za.minecraft.engine.graphics.ui.PauseMenu();
        renderer.setPauseMenu(pauseMenu);
        
        if (gameMode != GameMode.SINGLEPLAYER) {
            String ip = (gameMode == GameMode.MULTIPLAYER_HOST) ? "localhost" : serverAddress;
            networkClient = new GameClient(world, player, camera, playerName);
            networkClient.connect(ip);
        }
        
        running = true;
    }
    
    private void loop() {
        float interval = 1f / TARGET_UPS;
        float accumulator = 0f;
        
        while (running && !window.shouldClose()) {
            timer.updateDelta();
            float elapsedTime = timer.getDeltaF();
            accumulator += elapsedTime;
            
            input();
            
            while (accumulator >= interval) {
                update(interval);
                accumulator -= interval;
            }
            
            render();
            
            sync(elapsedTime);
        }
    }
    
    private void input() {
        boolean eKey = window.isKeyPressed(GLFW_KEY_E);
        if (eKey && !ePressed && !paused) {
            toggleInventory();
        }
        ePressed = eKey;

        boolean escKey = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escKey && !escPressed) {
            if (inventoryOpen) {
                toggleInventory();
            } else {
                togglePause();
            }
        }
        escPressed = escKey;

        highlightedBlock = inputManager.input(window, camera, player, timer.getDeltaF(), renderer, world, networkClient);
    }
    
    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        if (inventoryOpen) {
            inputManager.disableMouseCapture(window);
        } else {
            inputManager.enableMouseCapture(window);
            com.za.minecraft.world.items.ItemStack held = inputManager.getHeldStack();
            if (held != null) {
                player.getInventory().addItem(held);
                inputManager.clearHeldStack();
            }
        }
    }

    public void togglePause() {
        paused = !paused;
        if (paused) inputManager.disableMouseCapture(window);
        else inputManager.enableMouseCapture(window);
    }

    private void closeInventory() {
        inventoryOpen = false;
        inputManager.enableMouseCapture(window);
    }

    private void update(float interval) {
        if (paused) return;
        
        world.update(interval);
        
        // Синхронизация камеры после движения игрока
        camera.setPosition(player.getPosition().x, player.getPosition().y + 1.62f, player.getPosition().z);
        
        if (networkClient != null && networkClient.isConnected()) {
            networkClient.sendPlayerPosition();
        }
        
        fpsTimer += interval;
        fpsCounter++;
        
        if (fpsTimer >= 1.0f) {
            currentFps = fpsCounter;
            fpsCounter = 0;
            fpsTimer = 0;
        }
    }
    
    private void render() {
        renderer.render(window, camera, world, highlightedBlock, networkClient);
        
        if (inventoryOpen) {
            renderer.getUIRenderer().renderInventory(window.getWidth(), window.getHeight(), renderer.getAtlas());
        }
        
        // Отрисовка прогресса разрушения блока
        renderer.getUIRenderer().renderMiningProgress(window.getWidth(), window.getHeight(), inputManager.getBreakingProgress());
        
        // Отрисовка голода
        renderer.getUIRenderer().renderHunger(window.getWidth(), window.getHeight(), player.getHunger());
        
        // Отрисовка шума
        renderer.getUIRenderer().renderNoise(window.getWidth(), window.getHeight(), player.getNoiseLevel());
        
        renderer.renderDebug(currentFps, window.getWidth(), window.getHeight());
        window.update();
    }
    
    private void sync(float elapsedTime) {
        float loopSlot = 1f / TARGET_FPS;
        if (elapsedTime < loopSlot) {
            double endTime = timer.getDelta() + loopSlot;
        }
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

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    public boolean isPaused() {
        return paused;
    }
}
