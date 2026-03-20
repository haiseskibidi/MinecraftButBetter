package com.za.minecraft.engine.input;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.entities.Player;
import com.za.minecraft.entities.Inventory;
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
    private boolean eKeyPressed = false;
    private boolean verticalMode = false;
    private ItemStack heldStack = null;
    
    // Breaking block state
    private BlockPos breakingBlockPos = null;
    private float breakingProgress = 0.0f;
    private float breakDelayTimer = 0.0f;
    private float placeDelayTimer = 0.0f;
    private static final float BREAK_COOLDOWN = 1.0f / 20.0f; // 20 bps (1 tick)
    private static final float PLACE_COOLDOWN = 0.25f;        // 4 bps (5 ticks)
    
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
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;
        Inventory inv = player.getInventory();

        int sw = window.getWidth();
        int sh = window.getHeight();
        
        // Inventory Layout (Sync with UIRenderer)
        int cols = 9;
        int slotSize = (int)(18 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
        int totalWidth = cols * (slotSize + spacing);
        int totalHeight = (3 + 1) * (slotSize + spacing) + spacing * 2;
        int startX = (sw - totalWidth) / 2;
        int startY = (sh - totalHeight) / 2;
        
        float mx = currentPos.x;
        float my = currentPos.y;

        // Check Main Inventory (9-35)
        for (int i = 0; i < 27; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing);
            
            if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                swapWithHeld(inv, 9 + i);
                return;
            }
        }
        
        // Check Hotbar (0-8)
        int hotbarY = startY + 3 * (slotSize + spacing) + spacing * 4;
        for (int i = 0; i < 9; i++) {
            int x = startX + i * (slotSize + spacing);
            if (mx >= x && mx <= x + slotSize && my >= hotbarY && my <= hotbarY + slotSize) {
                swapWithHeld(inv, i);
                return;
            }
        }
    }

    private void swapWithHeld(Inventory inv, int slotIndex) {
        ItemStack slotStack = inv.getStackInSlot(slotIndex);
        
        if (heldStack == null && slotStack == null) return;
        
        // Simple swap logic
        inv.setStackInSlot(slotIndex, heldStack);
        heldStack = slotStack;
        
        com.za.minecraft.utils.Logger.info("Inventory click: slot %d, held: %s", slotIndex, (heldStack != null ? heldStack.getItem().getName() : "EMPTY"));
    }

    public ItemStack getHeldStack() {
        return heldStack;
    }

    public Vector2f getCurrentMousePos() {
        return currentPos;
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

        boolean eKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_E);
        if (eKeyCurrentlyPressed && !eKeyPressed) {
            boolean open = !GameLoop.getInstance().isInventoryOpen();
            GameLoop.getInstance().setInventoryOpen(open);
            if (open) {
                disableMouseCapture(window);
            } else {
                enableMouseCapture(window);
                // Drop held stack if inventory closed
                if (heldStack != null) {
                    player.getInventory().addItem(heldStack);
                    heldStack = null;
                }
            }
        }
        eKeyPressed = eKeyCurrentlyPressed;

        if (GameLoop.getInstance().isInventoryOpen() || GameLoop.getInstance().isPaused()) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            return new RaycastResult();
        }

        placeDelayTimer = Math.max(0, placeDelayTimer - deltaTime);
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

                Vector3f lookDirV = new Vector3f(0, 0, -1)
                    .rotateX(camera.getRotation().x)
                    .rotateY(camera.getRotation().y)
                    .normalize();
                
                Vector3f spawnPos = new Vector3f(camera.getPosition()).add(new Vector3f(lookDirV).mul(0.5f));
                com.za.minecraft.entities.ItemEntity itemEntity = new com.za.minecraft.entities.ItemEntity(spawnPos, droppedStack);
                
                // Set initial velocity - heavier items are harder to throw
                float throwStrength = 6.0f / droppedStack.getItem().getWeight();
                itemEntity.getVelocity().set(lookDirV).mul(throwStrength);
                // Slight upward boost for better arc
                itemEntity.getVelocity().y += 1.5f / droppedStack.getItem().getWeight();
                
                world.spawnEntity(itemEntity);
                com.za.minecraft.utils.Logger.info("Dropped item: %s", droppedStack.getItem().getName());
            }
        }
        qKeyPressed = qKeyCurrentlyPressed;
        
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        RaycastResult raycast = Raycast.raycast(world, camera.getPosition(), lookDir);
        
        ItemStack currentStack = player.getInventory().getSelectedItemStack();
        Item currentItem = currentStack != null ? currentStack.getItem() : null;

        boolean lm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1);
        if (lm) {
            player.swing();
            if (raycast.isHit()) {
                BlockPos hitPos = raycast.getBlockPos();
                byte blockType = world.getBlock(hitPos).getType();
                float hardness = BlockRegistry.getBlock(blockType).getHardness();
                
                if (hardness >= 0) {
                    if (!hitPos.equals(breakingBlockPos)) {
                        breakingBlockPos = hitPos;
                        breakingProgress = 0.0f;
                    }
                    
                    Item mineItem = currentItem != null ? currentItem : ItemRegistry.getItem(BlockType.AIR);
                    if (breakDelayTimer <= 0 && mineItem != null) {
                        float breakSpeed = mineItem.getMiningSpeed(blockType) / hardness;
                        breakingProgress += breakSpeed * deltaTime;
                        player.setContinuousNoise(Math.min(0.4f, 0.15f + hardness * 0.1f));
                        player.addNoise(hardness * 0.05f * deltaTime);
                    }

                    if (breakingProgress >= 1.0f) {
                        world.setBlock(hitPos, new Block(BlockType.AIR));
                        if (networkClient != null && networkClient.isConnected()) {
                            networkClient.sendBlockUpdate(hitPos.x(), hitPos.y(), hitPos.z(), BlockType.AIR);
                        }
                        breakingBlockPos = null;
                        breakingProgress = 0.0f;
                        breakDelayTimer = BREAK_COOLDOWN;
                        
                        if (currentStack != null && currentItem.isTool()) {
                            currentStack.setDurability(currentStack.getDurability() - 1);
                            if (currentStack.getDurability() <= 0) {
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
        
        boolean rm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_2);
        boolean isNewRightClick = rm && !rightMousePressed;
        
        if (rm) {
            boolean actionConsumed = false;

            // 1. Interaction with blocks
            if (raycast.isHit() && isNewRightClick) {
                BlockPos hitPos = raycast.getBlockPos();
                byte hitBlockType = world.getBlock(hitPos).getType();
                if (hitBlockType == BlockType.CAMPFIRE) {
                    if (currentItem != null && currentItem.getId() == ItemRegistry.RAW_MEAT) {
                        player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), new ItemStack(ItemRegistry.getItem(ItemRegistry.COOKED_MEAT)));
                        actionConsumed = true;
                    }
                } else if (hitBlockType == BlockType.GENERATOR) {
                    com.za.minecraft.world.blocks.entity.BlockEntity be = world.getBlockEntity(hitPos);
                    if (be instanceof com.za.minecraft.world.blocks.entity.GeneratorBlockEntity generator) {
                        if (currentItem != null && currentItem.getId() == ItemType.FUEL_CANISTER) {
                            generator.addFuel(100.0f);
                            ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                            player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                            actionConsumed = true;
                        }
                    }
                }
            }

            // 2. Using items (eating)
            if (!actionConsumed && isNewRightClick && currentItem != null && currentItem.isFood()) {
                if (player.getHunger() < 20.0f) {
                    player.eat((FoodItem) currentItem);
                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                    actionConsumed = true;
                }
            }
            
            // 3. Block Placement (Continuous or Fast Click)
            if (!actionConsumed && (isNewRightClick || placeDelayTimer <= 0) && raycast.isHit()) {
                if (currentItem != null && !currentItem.isTool() && !currentItem.isFood() && currentItem.getId() != BlockType.AIR) {
                    byte blockType = currentItem.getId();
                    Vector3f normal = raycast.getNormal();
                    BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
                    
                    if (!isPlayerAt(player, pPos)) {
                        byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                        world.setBlock(pPos, new Block(blockType, meta));
                        if (networkClient != null && networkClient.isConnected()) {
                            networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), blockType);
                        }
                        placeDelayTimer = PLACE_COOLDOWN;
                        actionConsumed = true;
                    }
                }
            }
        }

        // Preview logic
        if (raycast.isHit() && currentItem != null && !currentItem.isTool() && !currentItem.isFood() && currentItem.getId() != BlockType.AIR) {
            byte blockType = currentItem.getId();
            Vector3f normal = raycast.getNormal();
            BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
            
            if (!isPlayerAt(player, pPos) && needsPreview(blockType)) {
                byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                renderer.setPreviewBlock(pPos, new Block(blockType, meta));
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
