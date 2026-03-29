package com.za.minecraft.engine.core;

import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.Renderer;
import com.za.minecraft.engine.graphics.ui.Hotbar;
import com.za.minecraft.engine.input.InputManager;
import com.za.minecraft.entities.Player;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.network.GameServer;
import com.za.minecraft.world.World;
import com.za.minecraft.world.physics.RaycastResult;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

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
    private boolean jPressed = false;
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
    private com.za.minecraft.world.recipes.NappingSession currentNappingSession = null;
    
    public GameLoop() {
        instance = this;
    }
    
    public static GameLoop getInstance() {
        return instance;
    }
    
    public Player getPlayer() { return player; }
    public World getWorld() { return world; }
    public Camera getCamera() { return camera; }
    public Timer getTimer() { return timer; }
    public InputManager getInputManager() { return inputManager; }
    public Renderer getRenderer() { return renderer; }
    public void setInventoryOpen(boolean open) { this.inventoryOpen = open; }

    public void runSingleplayer() { runWithMode(GameMode.SINGLEPLAYER, "Player", null); }
    public void runAsHost(String name) { runWithMode(GameMode.MULTIPLAYER_HOST, name, null); }
    public void runAsClient(String name, String address) { runWithMode(GameMode.MULTIPLAYER_CLIENT, name, address); }
    
    private void runWithMode(GameMode mode, String name, String address) {
        this.gameMode = mode;
        this.playerName = name;
        this.serverAddress = address;
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        com.za.minecraft.world.DataLoader.loadAll();
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
        player.getInventory().setStackInSlot(0, new com.za.minecraft.world.items.ItemStack(com.za.minecraft.world.items.Items.ADMIN_HAMMER));
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
                update(interval); // Fixed Physics Update
                accumulator -= interval;
            }
            
            // Calculate interpolation alpha [0..1]
            float alpha = accumulator / interval;
            
            render(alpha); // Render with interpolation
            
            sync(elapsedTime);
        }
    }
    
    private void input() {
        com.za.minecraft.engine.graphics.ui.Screen active = com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        
        boolean eKey = window.isKeyPressed(GLFW_KEY_E);
        if (eKey && !ePressed && !paused) {
            if (active != null) {
                if (active.handleKeyPress(GLFW_KEY_E)) return;
                com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().closeScreen();
                toggleInventory();
            } else toggleInventory();
        }
        ePressed = eKey;

        boolean jKey = window.isKeyPressed(GLFW_KEY_J);
        if (jKey && !jPressed && !paused) {
            if (active != null && active.handleKeyPress(GLFW_KEY_J)) { }
            else toggleJournal();
        }
        jPressed = jKey;

        boolean escKey = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escKey && !escPressed) {
            if (active != null && active.handleKeyPress(GLFW_KEY_ESCAPE)) { }
            else if (inventoryOpen) {
                com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().closeScreen();
                toggleInventory();
            } else if (currentNappingSession != null) closeNappingWithWaste();
            else togglePause();
        }
        escPressed = escKey;

        highlightedBlock = inputManager.input(window, camera, player, timer.getDeltaF(), renderer, world, networkClient);
    }

    public void toggleJournal() {
        if (currentNappingSession != null) return;
        inventoryOpen = !inventoryOpen;
        if (inventoryOpen) {
            inputManager.disableMouseCapture(window);
            com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().openScreen(
                new com.za.minecraft.engine.graphics.ui.JournalScreen(), window.getWidth(), window.getHeight());
        } else {
            inputManager.enableMouseCapture(window);
            com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().closeScreen();
        }
    }
    
    public void toggleInventory() {
        if (currentNappingSession != null) return;
        inventoryOpen = !inventoryOpen;
        if (inventoryOpen) inputManager.disableMouseCapture(window);
        else {
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
        else if (!inventoryOpen && currentNappingSession == null) inputManager.enableMouseCapture(window);
    }

    public void startNapping(com.za.minecraft.world.items.Item item) {
        currentNappingSession = new com.za.minecraft.world.recipes.NappingSession(item);
        inputManager.disableMouseCapture(window);
    }

    public void closeNapping() {
        currentNappingSession = null;
        if (!inventoryOpen && !paused) inputManager.enableMouseCapture(window);
    }

    public void closeNappingWithWaste() {
        if (currentNappingSession != null) {
            com.za.minecraft.world.items.ItemStack current = player.getInventory().getSelectedItemStack();
            if (current != null) {
                com.za.minecraft.world.items.ItemStack newStack = current.getCount() > 1 
                    ? new com.za.minecraft.world.items.ItemStack(current.getItem(), current.getCount() - 1) : null;
                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
            }
        }
        closeNapping();
    }

    public boolean isNappingOpen() { return currentNappingSession != null; }
    public com.za.minecraft.world.recipes.NappingSession getNappingSession() { return currentNappingSession; }

    private void update(float interval) {
        if (paused) return;
        
        world.update(interval); // Fixed Physics
        
        // Fixed Camera Position (Physics Based)
        camera.setPosition(player.getPosition().x, player.getPosition().y + player.getEyeHeight(), player.getPosition().z);

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
    
    private void render(float alpha) {
        // High-Frequency Animation Update (Right before render)
        if (!paused) {
            player.updateAnimations(timer.getDeltaF(), world);
            
            // Sync animated offsets to camera (High-frequency)
            camera.setPitchOffset(player.getCameraPitchOffset());
            camera.setRollOffset(player.getCameraRollOffset());
            camera.setFovOffset(player.getFovOffset());
            camera.setOffsets(player.getCameraOffsetX(), player.getCameraOffsetY(), player.getCameraOffsetZ());
        }

        renderer.render(window, camera, world, highlightedBlock, networkClient, alpha);
        
        if (inventoryOpen) renderer.getUIRenderer().renderInventory(window.getWidth(), window.getHeight(), renderer.getAtlas());
        else if (currentNappingSession != null) com.za.minecraft.engine.graphics.ui.NappingGUI.render(renderer.getUIRenderer(), window.getWidth(), window.getHeight(), currentNappingSession);

        renderer.getUIRenderer().renderMiningProgress(window.getWidth(), window.getHeight(), inputManager.getBreakingProgress());
        
        if (highlightedBlock != null && highlightedBlock.isHit()) {
            com.za.minecraft.world.blocks.Block block = world.getBlock(highlightedBlock.getBlockPos());
            if (block.getType() == com.za.minecraft.world.blocks.Blocks.BURNING_PIT_KILN.getId()) {
                com.za.minecraft.world.blocks.entity.BlockEntity be = world.getBlockEntity(highlightedBlock.getBlockPos());
                if (be instanceof com.za.minecraft.world.blocks.entity.PitKilnBlockEntity kiln) {
                    renderer.getUIRenderer().renderFiringProgress(window.getWidth(), window.getHeight(), kiln.getProgress());
                }
            }
        }
        
        renderer.getUIRenderer().renderHunger(window.getWidth(), window.getHeight(), player.getHunger());
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
        if (networkClient != null) networkClient.disconnect();
        if (localServer != null) localServer.stop();
        if (renderer != null) renderer.cleanup();
        if (window != null) window.cleanup();
    }

    public boolean isInventoryOpen() { return inventoryOpen; }
    public boolean isPaused() { return paused; }
    public Window getWindow() { return window; }
}
