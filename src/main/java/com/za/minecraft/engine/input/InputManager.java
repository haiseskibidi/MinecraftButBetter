package com.za.minecraft.engine.input;

import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
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
    private boolean qKeyPressed = false;
    private boolean eKeyPressed = false;
    
    public InputManager() {
        previousPos = new Vector2f();
        currentPos = new Vector2f();
    }
    
    public void init(Window window) {
        // Устанавливаем курсор в центр окна перед переводом в disabled режим
        glfwSetCursorPos(window.getWindowHandle(), window.getWidth() / 2.0, window.getHeight() / 2.0);
        
        // Инициализируем позицию мыши в центре экрана
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
            leftButtonPressed = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS;
            rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS;
        });
        
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }
    
    public RaycastResult input(Window window, Camera camera, Player player, float deltaTime, com.za.minecraft.engine.graphics.Renderer renderer, World world, com.za.minecraft.network.GameClient networkClient) {
        Vector2f rotVec = new Vector2f();
        
        if (firstMouse) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            firstMouse = false;
            // Пропускаем первый кадр движения мыши полностью
            return new RaycastResult();
        }
        
        if (inWindow) {
            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            
            if (deltaX != 0) {
                rotVec.y = (float) -deltaX;
            }
            if (deltaY != 0) {
                rotVec.x = (float) -deltaY;
            }
        }
        
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;
        
        camera.moveRotation(
            rotVec.x * MOUSE_SENSITIVITY,
            rotVec.y * MOUSE_SENSITIVITY,
            0
        );
        
        Vector2f moveVector = new Vector2f();
        
        if (window.isKeyPressed(GLFW_KEY_W)) {
            moveVector.y = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_S)) {
            moveVector.y = -1;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            moveVector.x = -1;
        }
        if (window.isKeyPressed(GLFW_KEY_D)) {
            moveVector.x = 1;
        }
        
        float moveY = 0;
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            moveY = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            moveY = -1;
        }
        
        boolean sprinting = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
        float baseSpeed = player.isFlying() ? FLY_SPEED : MOVE_SPEED;
        if (sprinting) baseSpeed *= (player.isFlying() ? FLY_FAST_MULTIPLIER : GROUND_SPRINT_MULTIPLIER);

        if (moveVector.length() > 0) {
            moveVector.normalize();
            
            // Инвертируем yaw, поскольку Camera.getViewMatrix() использует -rotation.y
            float yaw = -camera.getRotation().y;
            
            // Стандартные формулы FPS камеры
            float forwardX = (float) Math.sin(yaw);
            float forwardZ = -(float) Math.cos(yaw);
            
            // Right vector (перпендикуляр к forward)
            float rightX = (float) Math.cos(yaw);
            float rightZ = (float) Math.sin(yaw);
            
            // Calculate movement direction
            float moveX = forwardX * moveVector.y + rightX * moveVector.x;
            float moveZ = forwardZ * moveVector.y + rightZ * moveVector.x;
            
            // Normalize and apply speed
            float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (length > 0) {
                moveX /= length;
                moveZ /= length;
            }
            
            // Apply acceleration for slight inertia
            float targetVx = moveX * baseSpeed;
            float targetVz = moveZ * baseSpeed;
            float accelGain = player.isFlying() ? 24.0f : 18.0f;
            float ax = (targetVx - player.getVelocity().x) * accelGain * deltaTime;
            float az = (targetVz - player.getVelocity().z) * accelGain * deltaTime;
            player.applyHorizontalAcceleration(ax, az, baseSpeed);
        } else {
            float decelGain = player.isFlying() ? 20.0f : 15.0f;
            float ax = -player.getVelocity().x * decelGain * deltaTime;
            float az = -player.getVelocity().z * decelGain * deltaTime;
            player.applyHorizontalAcceleration(ax, az, baseSpeed);
        }
        
        if (player.isFlying()) {
            float currentVy = player.getVelocity().y;
            float targetVy = moveY * baseSpeed;
            float vyGain = 25.0f;
            float ay = (targetVy - currentVy) * vyGain * deltaTime;
            player.addVelocity(0, ay, 0);
        } else if (moveY > 0) {
            player.jump();
        }
        
        boolean fKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_F);
        if (fKeyCurrentlyPressed && !fKeyPressed) {
            player.setFlying(!player.isFlying());
        }
        fKeyPressed = fKeyCurrentlyPressed;
        
        boolean gKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_G);
        if (gKeyCurrentlyPressed && !gKeyPressed) {
            renderer.toggleFXAA();
        }
        gKeyPressed = gKeyCurrentlyPressed;
        
        // Переключение блоков в инвентаре
        boolean qKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_Q);
        if (qKeyCurrentlyPressed && !qKeyPressed) {
            player.getInventory().previousBlock();
        }
        qKeyPressed = qKeyCurrentlyPressed;
        
        boolean eKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_E);
        if (eKeyCurrentlyPressed && !eKeyPressed) {
            player.getInventory().nextBlock();
        }
        eKeyPressed = eKeyCurrentlyPressed;
        
        // Raycast для определения блока, на который смотрит игрок
        Vector3f cameraPos = camera.getPosition();
        Vector3f cameraRot = camera.getRotation();
        
        // Направление взгляда: поворачиваем вектор (0,0,-1) камерой
        Vector3f lookDirection = new Vector3f(0, 0, -1)
            .rotateX(cameraRot.x)
            .rotateY(cameraRot.y)
            .normalize();
        
        RaycastResult raycast = Raycast.raycast(world, cameraPos, lookDirection);
        
        // Ломание блоков (левая кнопка мыши)
        boolean leftMouseCurrentlyPressed = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (leftMouseCurrentlyPressed && !leftMousePressed) {
            com.za.minecraft.utils.Logger.info("Left click detected");
            if (raycast.isHit()) {
                com.za.minecraft.utils.Logger.info("Breaking block at: %s", raycast.getBlockPos());
                world.setBlock(raycast.getBlockPos(), new Block(BlockType.AIR));
                
                // Синхронизация по сети
                if (networkClient != null && networkClient.isConnected()) {
                    BlockPos pos = raycast.getBlockPos();
                    networkClient.sendBlockUpdate(pos.x(), pos.y(), pos.z(), BlockType.AIR);
                }
            } else {
                com.za.minecraft.utils.Logger.info("No block hit by raycast. Camera pos: %.2f, %.2f, %.2f. Direction: %.2f, %.2f, %.2f", 
                    cameraPos.x, cameraPos.y, cameraPos.z, lookDirection.x, lookDirection.y, lookDirection.z);
            }
        }
        leftMousePressed = leftMouseCurrentlyPressed;
        
        // Установка блоков (правая кнопка мыши)
        boolean rightMouseCurrentlyPressed = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (rightMouseCurrentlyPressed && !rightMousePressed && raycast.isHit()) {
            // Размещаем блок на противоположной стороне от точки попадания
            Vector3f normal = raycast.getNormal();
            BlockPos hitPos = raycast.getBlockPos();
            BlockPos placePos = new BlockPos(
                hitPos.x() + (int)normal.x,
                hitPos.y() + (int)normal.y,
                hitPos.z() + (int)normal.z
            );
            
            com.za.minecraft.utils.Logger.info("Placing block: hit=(%d,%d,%d), normal=(%.1f,%.1f,%.1f), place=(%d,%d,%d)", 
                hitPos.x(), hitPos.y(), hitPos.z(), normal.x, normal.y, normal.z, placePos.x(), placePos.y(), placePos.z());
            
            // Проверяем, что позиция не занята игроком
            if (!isPlayerAt(player, placePos)) {
                com.za.minecraft.world.blocks.Block selected = player.getInventory().getSelectedBlock();
                // Determine axis from normal for orientation-aware blocks (e.g., WOOD logs)
                com.za.minecraft.world.blocks.Block.Axis axis;
                if (Math.abs(normal.x) > 0.5f) axis = com.za.minecraft.world.blocks.Block.Axis.X;
                else if (Math.abs(normal.y) > 0.5f) axis = com.za.minecraft.world.blocks.Block.Axis.Y;
                else axis = com.za.minecraft.world.blocks.Block.Axis.Z;
                // Apply axis to any block (renderer will use it when needed)
                selected = new com.za.minecraft.world.blocks.Block(selected.getType(), axis);
                world.setBlock(placePos, selected);
                
                // Синхронизация по сети
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendBlockUpdate(placePos.x(), placePos.y(), placePos.z(), selected.getType());
                }
            }
        }
        rightMousePressed = rightMouseCurrentlyPressed;
        
        camera.setPosition(
            player.getPosition().x,
            player.getPosition().y + 1.62f,
            player.getPosition().z
        );
        
        return raycast;
    }
    
    private boolean isPlayerAt(Player player, BlockPos blockPos) {
        // Проверяем, пересекается ли bounding box игрока с блоком
        Vector3f playerPos = player.getPosition();
        float playerWidth = 0.6f;
        float playerHeight = 1.8f;
        
        // Границы игрока
        float playerMinX = playerPos.x - playerWidth / 2;
        float playerMaxX = playerPos.x + playerWidth / 2;
        float playerMinY = playerPos.y;
        float playerMaxY = playerPos.y + playerHeight;
        float playerMinZ = playerPos.z - playerWidth / 2;
        float playerMaxZ = playerPos.z + playerWidth / 2;
        
        // Границы блока
        float blockMinX = blockPos.x();
        float blockMaxX = blockPos.x() + 1;
        float blockMinY = blockPos.y();
        float blockMaxY = blockPos.y() + 1;
        float blockMinZ = blockPos.z();
        float blockMaxZ = blockPos.z() + 1;
        
        // Проверка пересечения AABB
        return !(playerMaxX <= blockMinX || playerMinX >= blockMaxX ||
                 playerMaxY <= blockMinY || playerMinY >= blockMaxY ||
                 playerMaxZ <= blockMinZ || playerMinZ >= blockMaxZ);
    }
}
