package com.za.minecraft.engine.input;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.physics.Raycast;
import com.za.minecraft.world.physics.RaycastResult;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    private static final float MOUSE_SENSITIVITY = 0.002f;
    private static final float MOVE_SPEED = 3.8f;
    private static final float FLY_SPEED = 9.0f;
    private static final float FLY_FAST_MULTIPLIER = 1.8f;
    private static final float GROUND_SPRINT_MULTIPLIER = 1.45f;
    
    private final Vector2f previousPos;
    private final Vector2f currentPos;
    private boolean leftButtonPressed = false;
    private boolean rightButtonPressed = false;
    private boolean inWindow = false;
    private boolean fKeyPressed = false;
    private boolean gKeyPressed = false;
    private boolean firstMouse = true;
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private boolean rKeyPressed = false;
    private boolean verticalMode = false;
    
    public InputManager() {
        previousPos = new Vector2f();
        currentPos = new Vector2f();
    }
    
    public void init(Window window) {
        glfwSetCursorPos(window.getWindowHandle(), window.getWidth() / 2.0, window.getHeight() / 2.0);
        
        currentPos.x = window.getWidth() / 2.0f;
        currentPos.y = window.getHeight() / 2.0f;
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;
        
        glfwSetCursorPosCallback(window.getWindowHandle(), (windowHandle, xpos, ypos) -> {
            currentPos.x = (float) xpos;
            currentPos.y = (float) ypos;
        });
        
        glfwSetCursorEnterCallback(window.getWindowHandle(), (windowHandle, entered) -> {
            inWindow = entered;
        });
        
        glfwSetMouseButtonCallback(window.getWindowHandle(), (windowHandle, button, action, mode) -> {
            if (GameLoop.getInstance().isInventoryOpen()) {
                if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                    handleInventoryClick(window);
                }
            } else {
                leftButtonPressed = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS;
                rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS;
            }
        });
        
        glfwSetScrollCallback(window.getWindowHandle(), (windowHandle, xoffset, yoffset) -> {
            Player p = GameLoop.getInstance().getPlayer();
            if (p != null) {
                if (yoffset > 0) p.getInventory().previousBlock();
                else if (yoffset < 0) p.getInventory().nextBlock();
            }
        });
        
        enableMouseCapture(window);
    }
    
    private void handleInventoryClick(Window window) {
        // Логика клика по сетке инвентаря
        int padding = 50;
        int slotSize = 40;
        int spacing = 10;
        int columns = (window.getWidth() - padding * 2) / (slotSize + spacing);
        
        float mx = currentPos.x;
        float my = currentPos.y;
        
        var blocks = BlockRegistry.getRegisteredBlocks();
        int index = 0;
        for (var entry : blocks.entrySet()) {
            if (entry.getValue().getId() == BlockType.AIR) continue;
            
            int col = index % columns;
            int row = index / columns;
            
            int x = padding + col * (slotSize + spacing);
            int y = padding + row * (slotSize + spacing);
            
            if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                // Кликнули по блоку! Кладем его в текущий слот хотбара
                Player p = GameLoop.getInstance().getPlayer();
                if (p != null) {
                    p.getInventory().setSelectedBlock(new Block(entry.getKey()));
                    com.za.minecraft.utils.Logger.info("Picked block from inventory: %s", entry.getValue().getName());
                }
                return;
            }
            index++;
        }
    }

    public void enableMouseCapture(Window window) {
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        firstMouse = true;
    }
    
    public void disableMouseCapture(Window window) {
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }
    
    public RaycastResult input(Window window, Camera camera, Player player, float deltaTime, com.za.minecraft.engine.graphics.Renderer renderer, World world, com.za.minecraft.network.GameClient networkClient) {
        // Клавиши 1-9 доступны всегда (даже если инвентарь открыт, но GameLoop должен вызывать этот метод)
        for (int i = 0; i < 9; i++) {
            if (window.isKeyPressed(GLFW_KEY_1 + i)) {
                player.getInventory().setSelectedSlot(i);
                break;
            }
        }

        if (GameLoop.getInstance().isInventoryOpen()) {
            return new RaycastResult();
        }

        Vector2f rotVec = new Vector2f();
        
        if (firstMouse) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            firstMouse = false;
            return new RaycastResult();
        }
        
        if (inWindow) {
            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            rotVec.y = (float) -deltaX; // Restore minus
            rotVec.x = (float) -deltaY; // Restore minus
        }

        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;

        camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
        
        Vector2f moveVector = new Vector2f();
        if (window.isKeyPressed(GLFW_KEY_W)) moveVector.y = 1;
        if (window.isKeyPressed(GLFW_KEY_S)) moveVector.y = -1;
        if (window.isKeyPressed(GLFW_KEY_A)) moveVector.x = -1;
        if (window.isKeyPressed(GLFW_KEY_D)) moveVector.x = 1;
        
        float moveY = 0;
        if (window.isKeyPressed(GLFW_KEY_SPACE)) moveY = 1;
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) moveY = -1;
        
        boolean sprinting = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
        float baseSpeed = player.isFlying() ? FLY_SPEED : MOVE_SPEED;
        if (sprinting) baseSpeed *= (player.isFlying() ? FLY_FAST_MULTIPLIER : GROUND_SPRINT_MULTIPLIER);

        if (moveVector.length() > 0) {
            moveVector.normalize();
            float yaw = -camera.getRotation().y;
            float moveX = (float)Math.sin(yaw) * moveVector.y + (float)Math.cos(yaw) * moveVector.x;
            float moveZ = -(float)Math.cos(yaw) * moveVector.y + (float)Math.sin(yaw) * moveVector.x;
            float targetVx = moveX * baseSpeed;
            float targetVz = moveZ * baseSpeed;
            float accelGain = player.isFlying() ? 24.0f : 18.0f;
            player.applyHorizontalAcceleration((targetVx - player.getVelocity().x) * accelGain * deltaTime, (targetVz - player.getVelocity().z) * accelGain * deltaTime, baseSpeed);
        } else {
            float decelGain = player.isFlying() ? 20.0f : 15.0f;
            player.applyHorizontalAcceleration(-player.getVelocity().x * decelGain * deltaTime, -player.getVelocity().z * decelGain * deltaTime, baseSpeed);
        }
        
        if (player.isFlying()) {
            player.addVelocity(0, (moveY * baseSpeed - player.getVelocity().y) * 25.0f * deltaTime, 0);
        } else if (moveY > 0) {
            player.jump();
        }
        
        boolean fKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_F);
        if (fKeyCurrentlyPressed && !fKeyPressed) player.setFlying(!player.isFlying());
        fKeyPressed = fKeyCurrentlyPressed;
        
        boolean rKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_R);
        if (rKeyCurrentlyPressed && !rKeyPressed) verticalMode = !verticalMode;
        rKeyPressed = rKeyCurrentlyPressed;
        
        boolean gKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_G);
        if (gKeyCurrentlyPressed && !gKeyPressed) renderer.toggleFXAA();
        gKeyPressed = gKeyCurrentlyPressed;
        
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        RaycastResult raycast = Raycast.raycast(world, camera.getPosition(), lookDir);
        
        boolean lm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (lm && !leftMousePressed && raycast.isHit()) {
            world.setBlock(raycast.getBlockPos(), new Block(BlockType.AIR));
            if (networkClient != null && networkClient.isConnected()) {
                BlockPos pos = raycast.getBlockPos();
                networkClient.sendBlockUpdate(pos.x(), pos.y(), pos.z(), BlockType.AIR);
            }
        }
        leftMousePressed = lm;
        
        boolean rm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (raycast.isHit()) {
            Block selected = player.getInventory().getSelectedBlock();
            if (selected != null) {
                Vector3f normal = raycast.getNormal();
                BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
                
                byte meta = calculateMetadata(selected.getType(), normal, raycast.getHitPoint(), camera);
                Block previewBlock = new Block(selected.getType(), meta);
                
                if (!isPlayerAt(player, pPos)) {
                    if (needsPreview(selected.getType())) {
                        renderer.setPreviewBlock(pPos, previewBlock);
                    } else {
                        renderer.setPreviewBlock(null, null);
                    }
                    
                    if (rm && !rightMousePressed) {
                        world.setBlock(pPos, previewBlock);
                        if (networkClient != null && networkClient.isConnected()) {
                            networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), previewBlock.getType());
                        }
                    }
                } else {
                    renderer.setPreviewBlock(null, null);
                }
            }
        } else {
            renderer.setPreviewBlock(null, null);
        }
        rightMousePressed = rm;
        
        camera.setPosition(player.getPosition().x, player.getPosition().y + 1.62f, player.getPosition().z);
        return raycast;
    }
    
    private boolean needsPreview(byte type) {
        return type == BlockType.STONE_SLAB || 
               type == BlockType.BRICK_SLAB || 
               type == BlockType.STONE_STAIRS || 
               type == BlockType.BRICK_STAIRS ||
               type == BlockType.WOOD;
    }

    private byte calculateMetadata(byte type, Vector3f normal, Vector3f hitPoint, Camera camera) {
        float yaw = camera.getRotation().y;
        float deg = (float) Math.toDegrees(yaw) % 360;
        if (deg < 0) deg += 360;
        
        // Correct Mapping based on Camera.movePosition: 
        // 0 is North (-Z), 90 is West (-X), 180 is South (+Z), 270 is East (+X)
        byte viewDir;
        if (deg >= 45 && deg < 135) viewDir = Block.DIR_WEST;
        else if (deg >= 135 && deg < 225) viewDir = Block.DIR_SOUTH;
        else if (deg >= 225 && deg < 315) viewDir = Block.DIR_EAST;
        else viewDir = Block.DIR_NORTH;

        if (type == BlockType.STONE_SLAB || type == BlockType.BRICK_SLAB) {
            if (verticalMode) {
                // Если мы в вертикальном режиме, возвращаем сторону взгляда
                // Чтобы блок был вплотную к тому, на который мы смотрим
                return viewDir;
            }
            
            if (Math.abs(normal.y) > 0.5f) {
                return normal.y > 0 ? Block.DIR_DOWN : Block.DIR_UP;
            }
            float relativeY = hitPoint.y - (float)Math.floor(hitPoint.y);
            return relativeY > 0.5f ? Block.DIR_UP : Block.DIR_DOWN;
        }
        
        if (type == BlockType.STONE_STAIRS || type == BlockType.BRICK_STAIRS) {
            // Для ступенек: возвращаем сторону взгляда, чтобы они смотрели в сторону взгляда
            // (тогда ступеньки будут повернуты к игроку)
            return viewDir;
        }

        if (Math.abs(normal.x) > 0.5f) return normal.x > 0 ? Block.DIR_EAST : Block.DIR_WEST;
        if (Math.abs(normal.y) > 0.5f) return normal.y > 0 ? Block.DIR_UP : Block.DIR_DOWN;
        return normal.z > 0 ? Block.DIR_SOUTH : Block.DIR_NORTH;
    }
    
    private boolean isPlayerAt(Player player, BlockPos blockPos) {
        Vector3f p = player.getPosition();
        return !(p.x + 0.3f <= blockPos.x() || p.x - 0.3f >= blockPos.x() + 1 || p.y + 1.8f <= blockPos.y() || p.y >= blockPos.y() + 1 || p.z + 0.3f <= blockPos.z() || p.z - 0.3f >= blockPos.z() + 1);
    }
}
