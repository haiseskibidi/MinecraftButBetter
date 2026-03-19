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
            
            if (window.isKeyPressed(GLFW_KEY_ESCAPE)) {
                // simple toggle for pause
            }
            
            sync(elapsedTime);
        }
    }
    
    private void input() {
        if (window.isKeyPressed(GLFW_KEY_ESCAPE)) {
            if (inventoryOpen) {
                closeInventory();
            } else {
                paused = !paused;
                if (paused) inputManager.disableMouseCapture(window);
                else inputManager.enableMouseCapture(window);
            }
            while(window.isKeyPressed(GLFW_KEY_ESCAPE)) {
                glfwPollEvents();
            }
        }

        if (window.isKeyPressed(GLFW_KEY_E) && !paused) {
            inventoryOpen = !inventoryOpen;
            if (inventoryOpen) {
                inputManager.disableMouseCapture(window);
            } else {
                inputManager.enableMouseCapture(window);
            }
            while(window.isKeyPressed(GLFW_KEY_E)) {
                glfwPollEvents();
            }
        }
        
        // Вызываем inputManager даже если инвентарь открыт или игра на паузе,
        // чтобы работали горячие клавиши (1-9) и обновление камеры (внутри inputManager теперь есть проверка на inventoryOpen)
        highlightedBlock = inputManager.input(window, camera, player, timer.getDeltaF(), renderer, world, networkClient);
    }
    
    private void closeInventory() {
        inventoryOpen = false;
        inputManager.enableMouseCapture(window);
    }

    private void update(float interval) {
        if (paused || inventoryOpen) return;
        
        world.update(interval);
        
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
            double endTime = timer.getDelta() + loopSlot; // This is a bit simplified
            // For real sync we would need more precise timer methods, but this project uses simple update/render
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
}
