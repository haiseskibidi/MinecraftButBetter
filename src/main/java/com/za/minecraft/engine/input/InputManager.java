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
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.ToolItem;
import com.za.minecraft.world.items.FoodItem;
import com.za.minecraft.world.items.ItemType;
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
    private boolean qKeyPressed = false;
    private boolean firstMouse = true;
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private boolean rKeyPressed = false;
    private boolean verticalMode = false;
    
    // Breaking block state
    private BlockPos breakingBlockPos = null;
    private float breakingProgress = 0.0f;
    private float breakDelayTimer = 0.0f;
    private static final float BREAK_COOLDOWN = 1.0f / 20.0f; // 20 blocks per second limit (1 block per tick)
    
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
                if (yoffset > 0) p.getInventory().previousSlot();
                else if (yoffset < 0) p.getInventory().nextSlot();
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
        
        var allItems = ItemRegistry.getAllItems();
        int index = 0;
        for (var entry : allItems.entrySet()) {
            Item item = entry.getValue();
            if (item.getId() == BlockType.AIR) continue;
            
            int col = index % columns;
            int row = index / columns;
            
            int x = padding + col * (slotSize + spacing);
            int y = padding + row * (slotSize + spacing);
            
            if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                Player p = GameLoop.getInstance().getPlayer();
                if (p != null) {
                    p.getInventory().setStackInSlot(p.getInventory().getSelectedSlot(), new ItemStack(item));
                    com.za.minecraft.utils.Logger.info("Picked item from inventory: %s", item.getName());
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
    
    public float getBreakingProgress() {
        return breakingProgress;
    }

    public RaycastResult input(Window window, Camera camera, Player player, float deltaTime, com.za.minecraft.engine.graphics.Renderer renderer, World world, com.za.minecraft.network.GameClient networkClient) {
        for (int i = 0; i < 9; i++) {
            if (window.isKeyPressed(GLFW_KEY_1 + i)) {
                player.getInventory().setSelectedSlot(i);
                break;
            }
        }

        if (GameLoop.getInstance().isInventoryOpen() || GameLoop.getInstance().isPaused()) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            return new RaycastResult();
        }

        breakDelayTimer = Math.max(0, breakDelayTimer - deltaTime);

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
            rotVec.y = (float) -deltaX;
            rotVec.x = (float) -deltaY;
        }

        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;

        camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);

        // Apply View Bobbing
        float intensity = player.getBobIntensity();
        float bobX = (float) Math.sin(player.getWalkBobTimer() * 0.5f) * 0.04f * intensity;
        float bobY = (float) Math.sin(player.getWalkBobTimer()) * 0.04f * intensity;
        
        camera.setOffsets(bobX, bobY, 0);
        
        Vector2f moveVector = new Vector2f();
        if (window.isKeyPressed(GLFW_KEY_W)) moveVector.y = 1;
        if (window.isKeyPressed(GLFW_KEY_S)) moveVector.y = -1;
        if (window.isKeyPressed(GLFW_KEY_A)) moveVector.x = -1;
        if (window.isKeyPressed(GLFW_KEY_D)) moveVector.x = 1;
        
        float moveY = 0;
        if (window.isKeyPressed(GLFW_KEY_SPACE)) moveY = 1;
        boolean shiftPressed = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT);
        if (shiftPressed) moveY = -1;
        
        boolean sneaking = shiftPressed && !player.isFlying();
        player.setSneaking(sneaking);
        
        boolean sprinting = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
        player.setSprinting(sprinting);
        float baseSpeed = player.isFlying() ? FLY_SPEED : (sneaking ? MOVE_SPEED * 0.3f : MOVE_SPEED);
        if (sprinting && !sneaking) baseSpeed *= (player.isFlying() ? FLY_FAST_MULTIPLIER : GROUND_SPRINT_MULTIPLIER);

        player.setMoving(moveVector.length() > 0);
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
            if (player.isOnGround()) {
                player.addNoise(0.20f); // Spike above the floor
            }
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

        boolean qKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_Q);
        if (qKeyCurrentlyPressed && !qKeyPressed) {
            ItemStack stack = player.getInventory().getSelectedItemStack();
            if (stack != null) {
                // Drop 1 item
                ItemStack droppedStack = new ItemStack(stack.getItem(), 1);
                if (stack.getCount() > 1) {
                    stack.setCount(stack.getCount() - 1);
                } else {
                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                }

                Vector3f lookDirV = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
                Vector3f spawnPos = new Vector3f(camera.getPosition()).add(new Vector3f(lookDirV).mul(0.5f));
                com.za.minecraft.entities.ItemEntity itemEntity = new com.za.minecraft.entities.ItemEntity(spawnPos, droppedStack);
                
                // Set initial velocity
                itemEntity.getVelocity().set(lookDirV).mul(5.0f);
                world.spawnEntity(itemEntity);
                com.za.minecraft.utils.Logger.info("Dropped item: %s", droppedStack.getItem().getName());
            }
        }
        qKeyPressed = qKeyCurrentlyPressed;
        
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        RaycastResult raycast = Raycast.raycast(world, camera.getPosition(), lookDir);
        
        boolean lm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (lm) {
            player.swing();
            if (raycast.isHit()) {
                BlockPos currentPos = raycast.getBlockPos();
                byte blockType = world.getBlock(currentPos).getType();
                float hardness = BlockRegistry.getBlock(blockType).getHardness();
                
                // If block is unbreakable (hardness < 0), do nothing
                if (hardness >= 0) {
                    if (!currentPos.equals(breakingBlockPos)) {
                        breakingBlockPos = currentPos;
                        breakingProgress = 0.0f;
                    }
                    
                    ItemStack stack = player.getInventory().getSelectedItemStack();
                    Item item = stack != null ? stack.getItem() : ItemRegistry.getItem(BlockType.AIR);
                    
                    // Only progress if cooldown is over and item is valid
                    if (breakDelayTimer <= 0 && item != null) {
                        float speed = item.getMiningSpeed(blockType);
                        float breakSpeed = speed / hardness;
                        breakingProgress += breakSpeed * deltaTime;
                        
                        // Noise from breaking (depends on hardness)
                        player.setContinuousNoise(Math.min(0.4f, 0.15f + hardness * 0.1f));
                        player.addNoise(hardness * 0.05f * deltaTime);
                    }

                    if (breakingProgress >= 1.0f) {
                        world.setBlock(currentPos, new Block(BlockType.AIR));
                        if (networkClient != null && networkClient.isConnected()) {
                            networkClient.sendBlockUpdate(currentPos.x(), currentPos.y(), currentPos.z(), BlockType.AIR);
                        }
                        breakingBlockPos = null;
                        breakingProgress = 0.0f;
                        breakDelayTimer = BREAK_COOLDOWN; // Apply cooldown after EVERY break
                        
                        // Damage tool
                        if (stack != null && stack.getItem().isTool()) {
                            stack.setDurability(stack.getDurability() - 1);
                            if (stack.getDurability() <= 0) {
                                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                                com.za.minecraft.utils.Logger.info("Tool broken!");
                            }
                        }
                    }
                }
            } else {
                breakingBlockPos = null;
                breakingProgress = 0.0f;
            }
        } else {
            breakingBlockPos = null;
            breakingProgress = 0.0f;
        }
        leftMousePressed = lm;
        
        boolean rm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (rm && !rightMousePressed) {
            ItemStack stack = player.getInventory().getSelectedItemStack();
            Item selectedItem = stack != null ? stack.getItem() : null;
            
            boolean actionConsumed = false;

            // 1. Interaction with blocks (highest priority)
            if (raycast.isHit()) {
                BlockPos hitPos = raycast.getBlockPos();
                byte hitBlockType = world.getBlock(hitPos).getType();
                if (hitBlockType == BlockType.CAMPFIRE) {
                    if (selectedItem != null && selectedItem.getId() == ItemRegistry.RAW_MEAT) {
                        player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), new ItemStack(ItemRegistry.getItem(ItemRegistry.COOKED_MEAT)));
                        com.za.minecraft.utils.Logger.info("Cooked raw meat on campfire");
                        actionConsumed = true;
                    }
                } else if (hitBlockType == BlockType.GENERATOR) {
                    com.za.minecraft.world.blocks.entity.BlockEntity be = world.getBlockEntity(hitPos);
                    if (be instanceof com.za.minecraft.world.blocks.entity.GeneratorBlockEntity generator) {
                        if (selectedItem != null && selectedItem.getId() == ItemType.FUEL_CANISTER) {
                            generator.addFuel(100.0f);
                            // Consume one canister or empty it (for now just consume)
                            ItemStack newStack = stack.getCount() > 1 ? new ItemStack(selectedItem, stack.getCount() - 1) : null;
                            player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                            actionConsumed = true;
                        } else {
                            com.za.minecraft.utils.Logger.info("Generator status: Fuel: %.1f, Energy: %.1f, Running: %b", 
                                generator.getFuel(), generator.getEnergy(), generator.isRunning());
                        }
                    }
                }
            }

            // 2. Using items (eating)
            if (!actionConsumed && selectedItem != null && selectedItem.isFood()) {
                if (player.getHunger() < 20.0f) {
                    player.eat((FoodItem) selectedItem);
                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                    actionConsumed = true;
                }
            }
            
            if (actionConsumed) {
                rightMousePressed = true;
            }
        }

        // 3. Block Placement (preview and placement)
        if (raycast.isHit()) {
            ItemStack stack = player.getInventory().getSelectedItemStack();
            Item selectedItem = stack != null ? stack.getItem() : null;

            if (selectedItem != null && !selectedItem.isTool() && !selectedItem.isFood()) {
                byte blockType = selectedItem.getId();
                Vector3f normal = raycast.getNormal();
                BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
                
                byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                Block previewBlock = new Block(blockType, meta);
                
                if (!isPlayerAt(player, pPos)) {
                    if (needsPreview(blockType)) {
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
            } else {
                renderer.setPreviewBlock(null, null);
            }
        } else {
            renderer.setPreviewBlock(null, null);
        }

        rightMousePressed = rm;
        leftMousePressed = lm;

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
        
        byte viewDir;
        if (deg >= 45 && deg < 135) viewDir = Block.DIR_WEST;
        else if (deg >= 135 && deg < 225) viewDir = Block.DIR_SOUTH;
        else if (deg >= 225 && deg < 315) viewDir = Block.DIR_EAST;
        else viewDir = Block.DIR_NORTH;

        if (type == BlockType.STONE_SLAB || type == BlockType.BRICK_SLAB) {
            if (verticalMode) return viewDir;
            if (Math.abs(normal.y) > 0.5f) return normal.y > 0 ? Block.DIR_DOWN : Block.DIR_UP;
            float relativeY = hitPoint.y - (float)Math.floor(hitPoint.y);
            return relativeY > 0.5f ? Block.DIR_UP : Block.DIR_DOWN;
        }
        
        if (type == BlockType.STONE_STAIRS || type == BlockType.BRICK_STAIRS) return viewDir;

        if (Math.abs(normal.x) > 0.5f) return normal.x > 0 ? Block.DIR_EAST : Block.DIR_WEST;
        if (Math.abs(normal.y) > 0.5f) return normal.y > 0 ? Block.DIR_UP : Block.DIR_DOWN;
        return normal.z > 0 ? Block.DIR_SOUTH : Block.DIR_NORTH;
    }
    
    private boolean isPlayerAt(Player player, BlockPos blockPos) {
        Vector3f p = player.getPosition();
        return !(p.x + 0.3f <= blockPos.x() || p.x - 0.3f >= blockPos.x() + 1 || p.y + 1.8f <= blockPos.y() || p.y >= blockPos.y() + 1 || p.z + 0.3f <= blockPos.z() || p.z - 0.3f >= blockPos.z() + 1);
    }
}
