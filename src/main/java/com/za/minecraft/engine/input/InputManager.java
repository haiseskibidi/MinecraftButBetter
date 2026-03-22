package com.za.minecraft.engine.input;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.entities.Player;
import com.za.minecraft.entities.Inventory;
import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.PlacementType;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.items.ToolItem;
import com.za.minecraft.world.items.FoodItem;
import com.za.minecraft.world.items.component.FuelComponent;
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
    private boolean inWindow = false;
    private boolean fKeyPressed = false;
    private boolean gKeyPressed = false;
    private boolean firstMouse = true;
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private boolean rKeyPressed = false;
    private boolean zKeyPressed = false;
    private boolean f3KeyPressed = false;
    private boolean qKeyPressed = false;
    private boolean verticalMode = false;
    private ItemStack heldStack = null;
    private int devScroll = 0;
    private int hoveredSlotIndex = -1;
    
    // Drag-to-Distribute state
    private java.util.Set<Integer> draggedSlots = new java.util.LinkedHashSet<>();
    private int dragButton = -1;
    private boolean isDragging = false;
    
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
                if (action == GLFW_PRESS) {
                    dragButton = button;
                    draggedSlots.clear();
                    isDragging = false;
                    
                    if (heldStack == null) {
                        handleInventoryClick(window, button);
                    } else {
                        Player p = GameLoop.getInstance().getPlayer();
                        int slotIdx = getSlotAt(currentPos.x, currentPos.y, window.getWidth(), window.getHeight(), p);
                        if (slotIdx != -1) {
                            draggedSlots.add(slotIdx);
                            isDragging = true;
                        } else {
                            handleInventoryClick(window, button);
                        }
                    }
                } else if (action == GLFW_RELEASE) {
                    if (dragButton == button) {
                        if (isDragging && !draggedSlots.isEmpty()) {
                            if (draggedSlots.size() == 1) {
                                handleInventoryClickOnSlot(window, button, draggedSlots.iterator().next());
                            } else {
                                finishDrag();
                            }
                        }
                        dragButton = -1;
                        draggedSlots.clear();
                        isDragging = false;
                    }
                }
            }
        });
        
        glfwSetScrollCallback(window.getWindowHandle(), (windowHandle, xoffset, yoffset) -> {
            Player p = GameLoop.getInstance().getPlayer();
            if (p != null) {
                if (GameLoop.getInstance().isInventoryOpen() && p.getMode() == PlayerMode.DEVELOPER) {
                    int totalItems = ItemRegistry.getAllItems().size();
                    int columns = 7;
                    int rows = 12;
                    int maxScroll = Math.max(0, (int) Math.ceil((double) totalItems / columns) - rows);
                    devScroll = Math.min(maxScroll, Math.max(0, devScroll - (int) yoffset));
                } else if (!GameLoop.getInstance().isNappingOpen()) {
                    if (yoffset > 0) p.getInventory().previousSlot();
                    else if (yoffset < 0) p.getInventory().nextSlot();
                }
            }
        });
        
        enableMouseCapture(window);
    }
    
    private int getSlotAt(float mx, float my, int sw, int sh, Player player) {
        int cols = 9;
        int slotSize = (int)(18 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
        int totalWidth = cols * (slotSize + spacing);
        int totalHeight = (3 + 1) * (slotSize + spacing) + spacing * 2;
        int startX = (sw - totalWidth) / 2;
        int startY = (sh - totalHeight) / 2;

        if (player.getMode() == PlayerMode.DEVELOPER) {
            startX -= 120;
        }

        for (int i = 0; i < 27; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing);
            if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) return 9 + i;
        }

        int hotbarY = startY + 3 * (slotSize + spacing) + spacing * 4;
        for (int i = 0; i < 9; i++) {
            int x = startX + i * (slotSize + spacing);
            if (mx >= x && mx <= x + slotSize && my >= hotbarY && my <= hotbarY + slotSize) return i;
        }

        return -1;
    }

    private void dropStack(ItemStack stack, Player player, World world, Camera camera, boolean dropAll) {
        if (stack == null) return;

        ItemStack toDrop;
        if (dropAll) {
            toDrop = stack.copy();
            stack.setCount(0); 
        } else {
            toDrop = stack.split(1);
        }

        if (toDrop == null) return;

        Vector3f lookDirV = new Vector3f(0, 0, -1)
            .rotateX(camera.getRotation().x)
            .rotateY(camera.getRotation().y)
            .normalize();
        
        Vector3f spawnPos = new Vector3f(camera.getPosition()).add(new Vector3f(lookDirV).mul(0.5f));
        com.za.minecraft.entities.ItemEntity itemEntity = new com.za.minecraft.entities.ItemEntity(spawnPos, toDrop);
        
        float throwStrength = 6.0f / toDrop.getItem().getWeight();
        itemEntity.getVelocity().set(lookDirV).mul(throwStrength);
        itemEntity.getVelocity().y += 1.5f / toDrop.getItem().getWeight();
        
        world.spawnEntity(itemEntity);
        com.za.minecraft.utils.Logger.info("Dropped stack: %s (x%d)", toDrop.getItem().getName(), toDrop.getCount());
    }
    
    private void finishDrag() {
        if (heldStack == null || draggedSlots.isEmpty()) return;
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;
        Inventory inv = player.getInventory();

        if (dragButton == GLFW_MOUSE_BUTTON_1) {
            int amountPerSlot = heldStack.getCount() / draggedSlots.size();
            if (amountPerSlot > 0) {
                for (int slotIdx : draggedSlots) {
                    ItemStack slotStack = inv.getStackInSlot(slotIdx);
                    if (slotStack == null) {
                        inv.setStackInSlot(slotIdx, heldStack.split(amountPerSlot));
                    } else if (heldStack.isStackableWith(slotStack)) {
                        ItemStack split = heldStack.split(amountPerSlot);
                        if (split != null) {
                            slotStack.setCount(slotStack.getCount() + split.getCount());
                        }
                    }
                }
            }
        } else if (dragButton == GLFW_MOUSE_BUTTON_2) {
            for (int slotIdx : draggedSlots) {
                if (heldStack.getCount() <= 0) break;
                ItemStack slotStack = inv.getStackInSlot(slotIdx);
                if (slotStack == null) {
                    inv.setStackInSlot(slotIdx, heldStack.split(1));
                } else if (heldStack.isStackableWith(slotStack)) {
                    ItemStack split = heldStack.split(1);
                    if (split != null) {
                        slotStack.setCount(slotStack.getCount() + 1);
                    }
                }
            }
        }
        
        if (heldStack.getCount() <= 0) {
            heldStack = null;
        }
    }

    private void handleInventoryClickOnSlot(Window window, int button, int slotIdx) {
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;
        Inventory inv = player.getInventory();

        boolean shift = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || window.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
        if (shift && button == GLFW_MOUSE_BUTTON_1) {
            inv.quickMove(slotIdx);
            return;
        }

        ItemStack slotStack = inv.getStackInSlot(slotIdx);
        
        if (button == GLFW_MOUSE_BUTTON_1) {
            if (heldStack != null && slotStack != null && heldStack.isStackableWith(slotStack)) {
                slotStack.setCount(slotStack.getCount() + heldStack.getCount());
                heldStack = null;
            } else {
                inv.setStackInSlot(slotIdx, heldStack);
                heldStack = slotStack;
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
            if (heldStack == null) {
                if (slotStack != null) {
                    int toTake = (int) Math.ceil(slotStack.getCount() / 2.0);
                    heldStack = slotStack.split(toTake);
                    if (slotStack.getCount() <= 0) inv.setStackInSlot(slotIdx, null);
                }
            } else {
                if (slotStack == null) {
                    inv.setStackInSlot(slotIdx, heldStack.split(1));
                    if (heldStack.getCount() <= 0) heldStack = null;
                } else if (heldStack.isStackableWith(slotStack)) {
                    slotStack.setCount(slotStack.getCount() + 1);
                    heldStack.setCount(heldStack.getCount() - 1);
                    if (heldStack.getCount() <= 0) heldStack = null;
                } else {
                    inv.setStackInSlot(slotIdx, heldStack);
                    heldStack = slotStack;
                }
            }
        }
    }

    private void handleInventoryClick(Window window, int button) {
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;
        
        int sw = window.getWidth();
        int sh = window.getHeight();
        float mx = currentPos.x;
        float my = currentPos.y;

        int slotIdx = getSlotAt(mx, my, sw, sh, player);

        if (slotIdx != -1) {
            handleInventoryClickOnSlot(window, button, slotIdx);
        } else {
            if (heldStack != null) {
                dropStack(heldStack, player, GameLoop.getInstance().getWorld(), GameLoop.getInstance().getCamera(), true);
                heldStack = null;
            } else if (player.getMode() == PlayerMode.DEVELOPER) {
                int cols = 9;
                int slotSize = (int)(18 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
                int spacing = (int)(2 * com.za.minecraft.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
                int totalWidth = cols * (slotSize + spacing);
                int startX = (sw - totalWidth) / 2 - 120;
                int devX = startX + totalWidth + 20;
                
                int devCols = 7;
                int devWidth = devCols * (slotSize + spacing);
                int devRows = 12;
                int devHeight = devRows * (slotSize + spacing);
                int startY = (sh - ((3 + 1) * (slotSize + spacing) + spacing * 2)) / 2;

                if (mx >= devX && mx <= devX + devWidth && my >= startY && my <= startY + devHeight) {
                    handleDevPanelClick(mx, my, devX, startY, slotSize, spacing);
                }
            }
        }
    }

    private void handleDevPanelClick(float mx, float my, int devX, int startY, int slotSize, int spacing) {
        java.util.List<Item> allItems = new java.util.ArrayList<>(ItemRegistry.getAllItems().values());
        int cols = 7;
        int rows = 12; 
        
        for (int i = 0; i < allItems.size(); i++) {
            int idx = i - devScroll * cols;
            if (idx < 0) continue;
            
            int col = idx % cols;
            int row = idx / cols;
            if (row >= rows) break;
            
            int x = devX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing);
            
            if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                Item item = allItems.get(i);
                heldStack = new ItemStack(item, item.isBlock() ? 64 : 1);
                return;
            }
        }
    }

    public ItemStack getHeldStack() {
        return heldStack;
    }

    public void clearHeldStack() {
        heldStack = null;
    }

    public Vector2f getCurrentMousePos() {
        return currentPos;
    }

    public int getDevScroll() {
        return devScroll;
    }

    public int getHoveredSlotIndex() {
        return hoveredSlotIndex;
    }
    
    public java.util.Set<Integer> getDraggedSlots() {
        return draggedSlots;
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
        boolean inventoryOpen = GameLoop.getInstance().isInventoryOpen();
        boolean nappingOpen = GameLoop.getInstance().isNappingOpen();
        boolean paused = GameLoop.getInstance().isPaused();

        // Блокируем хотбар во время скалывания или инвентаря
        if (!nappingOpen && !inventoryOpen) {
            for (int i = 0; i < 9; i++) {
                if (window.isKeyPressed(GLFW_KEY_1 + i)) {
                    player.getInventory().setSelectedSlot(i);
                    break;
                }
            }
        }

        if (inventoryOpen || nappingOpen) {
            if (nappingOpen) {
                if (window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1) && !leftMousePressed) {
                    int slotIdx = com.za.minecraft.engine.graphics.ui.NappingGUI.getSlotIndexAt(currentPos.x, currentPos.y, window.getWidth(), window.getHeight());
                    if (slotIdx != -1) {
                        com.za.minecraft.world.recipes.NappingSession session = GameLoop.getInstance().getNappingSession();
                        session.removePiece(slotIdx);
                        
                        com.za.minecraft.world.recipes.NappingRecipe result = session.checkMatch();
                        if (result != null) {
                            com.za.minecraft.utils.Logger.info("Napping complete!");
                            
                            ItemStack current = player.getInventory().getSelectedItemStack();
                            if (current != null) {
                                ItemStack newStack = current.getCount() > 1 ? new ItemStack(current.getItem(), current.getCount() - 1) : null;
                                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                            }
                            
                            player.getInventory().addItem(result.getResult());
                            GameLoop.getInstance().closeNapping();
                            leftMousePressed = true;
                            rightMousePressed = false;
                            return null;
                        }
                    }
                }
                
                leftMousePressed = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1);
                rightMousePressed = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_2);
            }

            if (inventoryOpen) {
                int newHovered = getSlotAt(currentPos.x, currentPos.y, window.getWidth(), window.getHeight(), player);
                if (newHovered != hoveredSlotIndex) {
                    hoveredSlotIndex = newHovered;
                    if (isDragging && dragButton != -1 && heldStack != null && hoveredSlotIndex != -1) {
                        ItemStack slotStack = player.getInventory().getStackInSlot(hoveredSlotIndex);
                        boolean canReceive = (slotStack == null || heldStack.isStackableWith(slotStack));
                        if (canReceive && (draggedSlots.contains(hoveredSlotIndex) || draggedSlots.size() < heldStack.getCount())) {
                            draggedSlots.add(hoveredSlotIndex);
                        }
                    }
                }
                
                boolean zKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_Z);
                if (zKeyCurrentlyPressed && !zKeyPressed) {
                    player.getInventory().sortMainInventory();
                }
                zKeyPressed = zKeyCurrentlyPressed;
            }
        } else {
            hoveredSlotIndex = -1;
            isDragging = false;
            draggedSlots.clear();
        }

        placeDelayTimer = Math.max(0, placeDelayTimer - deltaTime);
        breakDelayTimer = Math.max(0, breakDelayTimer - deltaTime);

        Vector2f rotVec = new Vector2f();
        if (firstMouse) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            firstMouse = false;
        } else if (inWindow && !inventoryOpen && !paused && !nappingOpen) {
            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            rotVec.y = (float) -deltaX;
            rotVec.x = (float) -deltaY;
        }
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;

        if (!inventoryOpen && !paused && !nappingOpen) {
            camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
        }

        float intensity = player.getBobIntensity();
        float bobX = (float) Math.sin(player.getWalkBobTimer() * 0.5f) * 0.04f * intensity;
        float bobY = (float) Math.sin(player.getWalkBobTimer()) * 0.04f * intensity;
        camera.setOffsets(bobX, bobY, 0);
        
        Vector2f moveVector = new Vector2f();
        if (!inventoryOpen && !paused && !nappingOpen) {
            if (window.isKeyPressed(GLFW_KEY_W)) moveVector.y = 1;
            if (window.isKeyPressed(GLFW_KEY_S)) moveVector.y = -1;
            if (window.isKeyPressed(GLFW_KEY_A)) moveVector.x = -1;
            if (window.isKeyPressed(GLFW_KEY_D)) moveVector.x = 1;
        }
        
        float moveY = 0;
        if (!inventoryOpen && !paused && !nappingOpen) {
            if (window.isKeyPressed(GLFW_KEY_SPACE)) moveY = 1;
        }
        boolean shiftPressed = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT);
        if (shiftPressed && !inventoryOpen && !paused && !nappingOpen) moveY = -1;
        
        boolean sneaking = shiftPressed && !player.isFlying() && !inventoryOpen && !paused && !nappingOpen;
        player.setSneaking(sneaking);
        
        boolean sprinting = (window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) && !inventoryOpen && !paused && !nappingOpen;
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
            float accelGain = player.getMode() == PlayerMode.DEVELOPER ? 30.0f : (player.isFlying() ? 24.0f : 18.0f);
            player.applyHorizontalAcceleration((targetVx - player.getVelocity().x) * accelGain * deltaTime, (targetVz - player.getVelocity().z) * accelGain * deltaTime, baseSpeed);
        } else {
            float decelGain = player.isFlying() ? 20.0f : 15.0f;
            player.applyHorizontalAcceleration(-player.getVelocity().x * decelGain * deltaTime, -player.getVelocity().z * decelGain * deltaTime, baseSpeed);
        }
        
        if (player.isFlying()) {
            player.addVelocity(0, (moveY * baseSpeed - player.getVelocity().y) * 25.0f * deltaTime, 0);
        } else if (moveY > 0 && !inventoryOpen && !paused && !nappingOpen) {
            if (player.isOnGround()) {
                player.addNoise(0.20f); 
            }
            player.jump();
        }
        
        boolean fKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_F);
        if (fKeyCurrentlyPressed && !fKeyPressed && !inventoryOpen && !paused && !nappingOpen) player.setFlying(!player.isFlying());
        fKeyPressed = fKeyCurrentlyPressed;

        boolean f3KeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_F3);
        if (f3KeyCurrentlyPressed && !f3KeyPressed && !inventoryOpen && !paused && !nappingOpen) {
            PlayerMode newMode = (player.getMode() == PlayerMode.SURVIVAL) 
                ? PlayerMode.DEVELOPER : PlayerMode.SURVIVAL;
            player.setMode(newMode);
            com.za.minecraft.utils.Logger.info("Player mode changed to: %s", newMode);
        }
        f3KeyPressed = f3KeyCurrentlyPressed;
        
        boolean rKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_R);
        if (rKeyCurrentlyPressed && !rKeyPressed && !inventoryOpen && !paused && !nappingOpen) verticalMode = !verticalMode;
        rKeyPressed = rKeyCurrentlyPressed;
        
        boolean gKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_G);
        if (gKeyCurrentlyPressed && !gKeyPressed && !inventoryOpen && !paused && !nappingOpen) renderer.toggleFXAA();
        gKeyPressed = gKeyCurrentlyPressed;

        boolean qKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_Q);
        if (qKeyCurrentlyPressed && !qKeyPressed && !paused && !nappingOpen) {
            if (inventoryOpen) {
                if (hoveredSlotIndex != -1) {
                    ItemStack stack = player.getInventory().getStackInSlot(hoveredSlotIndex);
                    if (stack != null) {
                        boolean ctrlPressed = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
                        dropStack(stack, player, world, camera, ctrlPressed);
                        if (stack.getCount() <= 0) player.getInventory().setStackInSlot(hoveredSlotIndex, null);
                    }
                }
            } else {
                ItemStack stack = player.getInventory().getSelectedItemStack();
                if (stack != null) {
                    boolean ctrlPressed = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
                    dropStack(stack, player, world, camera, ctrlPressed);
                    if (stack.getCount() <= 0) player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                }
            }
        }
        qKeyPressed = qKeyCurrentlyPressed;
        
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        com.za.minecraft.entities.Entity hitEntity = Raycast.raycastEntity(world, camera.getPosition(), lookDir);
        RaycastResult raycast = Raycast.raycast(world, camera.getPosition(), lookDir);
        
        ItemStack currentStack = player.getInventory().getSelectedItemStack();
        Item currentItem = currentStack != null ? currentStack.getItem() : null;

        if (!inventoryOpen && !paused && !nappingOpen) {
            boolean lm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1);
            boolean isNewLeftClick = lm && !leftMousePressed;

            if (lm) {
                player.swing();
                if (isNewLeftClick && hitEntity instanceof com.za.minecraft.entities.ResourceEntity resource) {
                    player.getInventory().addItem(resource.getStack());
                    resource.setRemoved();
                    com.za.minecraft.utils.Logger.info("Picked up %s", resource.getStack().getItem().getName());
                } else if (raycast.isHit()) {
                    BlockPos hitPos = raycast.getBlockPos();
                    int blockType = world.getBlock(hitPos).getType();
                    BlockDefinition blockDef = BlockRegistry.getBlock(blockType);
                    float hardness = blockDef.getHardness();

                    if (hardness >= 0) {
                        if (hardness == 0.0f) {
                            breakingProgress = 1.0f;
                        } else {
                            if (!hitPos.equals(breakingBlockPos)) {
                                breakingBlockPos = hitPos;
                                breakingProgress = 0.0f;
                            }
                            Item mineItem = currentItem != null ? currentItem : ItemRegistry.getItem(Blocks.AIR.getId());
                            if (breakDelayTimer <= 0 && mineItem != null) {
                                float breakSpeed = mineItem.getMiningSpeed(blockType) / hardness;
                                breakingProgress += breakSpeed * deltaTime;
                                player.setContinuousNoise(Math.min(0.4f, 0.15f + hardness * 0.1f));
                                player.addNoise(hardness * 0.05f * deltaTime);
                            }
                        }

                        if (breakingProgress >= 1.0f) {
                            String dropId = blockDef.getDropItem();
                            Item itemToGive = (dropId != null) ? ItemRegistry.getItem(Identifier.of(dropId)) : ItemRegistry.getItem(blockDef.getIdentifier());
                            if (itemToGive != null) player.getInventory().addItem(new ItemStack(itemToGive));
                            world.setBlock(hitPos, new Block(Blocks.AIR.getId()));
                            if (networkClient != null && networkClient.isConnected()) {
                                networkClient.sendBlockUpdate(hitPos.x(), hitPos.y(), hitPos.z(), Blocks.AIR.getId());
                            }
                            breakingBlockPos = null;
                            breakingProgress = 0.0f;
                            breakDelayTimer = BREAK_COOLDOWN;
                            if (currentStack != null && currentItem.isTool()) {
                                currentStack.setDurability(currentStack.getDurability() - 1);
                                if (currentStack.getDurability() <= 0) player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                            }
                        }
                    }
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
                if (raycast.isHit() && isNewRightClick) {
                    BlockPos hitPos = raycast.getBlockPos();
                    int hitBlockType = world.getBlock(hitPos).getType();
                    if (hitBlockType == Blocks.CAMPFIRE.getId()) {
                        if (currentItem != null && currentItem.getId() == Items.RAW_MEAT.getId()) {
                            player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), new ItemStack(Items.COOKED_MEAT));
                            actionConsumed = true;
                        }
                    } else if (hitBlockType == Blocks.GENERATOR.getId()) {
                        com.za.minecraft.world.blocks.entity.BlockEntity be = world.getBlockEntity(hitPos);
                        if (be instanceof com.za.minecraft.world.blocks.entity.GeneratorBlockEntity generator) {
                            if (currentItem != null && currentItem.hasComponent(FuelComponent.class)) {
                                FuelComponent fuel = currentItem.getComponent(FuelComponent.class);
                                generator.addFuel(fuel.fuelAmount());
                                ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                                actionConsumed = true;
                            } else {
                                actionConsumed = true;
                            }
                        }
                    }
                }

                if (!actionConsumed && isNewRightClick && currentStack != null) {
                    if (currentStack.getCount() >= 2) {
                        java.util.List<com.za.minecraft.world.recipes.IRecipe> nappingRecipes = com.za.minecraft.world.recipes.RecipeRegistry.getRecipesByType("napping");
                        
                        boolean hasNapping = false;
                        for (com.za.minecraft.world.recipes.IRecipe r : nappingRecipes) {
                            com.za.minecraft.world.recipes.NappingRecipe nr = (com.za.minecraft.world.recipes.NappingRecipe) r;
                            if (nr.getInputId().equals(currentItem.getIdentifier())) {
                                hasNapping = true;
                                break;
                            }
                        }
                        
                        if (hasNapping) {
                            com.za.minecraft.utils.Logger.info("Starting napping for: " + currentItem.getName());
                            GameLoop.getInstance().startNapping(currentItem);
                            actionConsumed = true;
                        }
                    }
                }

                if (!actionConsumed && isNewRightClick && currentItem != null && currentItem.isFood()) {
                    if (player.getHunger() < 20.0f) {
                        player.eat(currentItem);
                        ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                        player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                        actionConsumed = true;
                    }
                }
                
                if (!actionConsumed && (isNewRightClick || placeDelayTimer <= 0) && raycast.isHit()) {
                    if (currentItem != null && currentItem.isBlock()) {
                        int blockType = currentItem.getId();
                        Vector3f normal = raycast.getNormal();
                        BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
                        if (!isPlayerAt(player, pPos)) {
                            byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                            world.setBlock(pPos, new Block(blockType, meta));
                            if (networkClient != null && networkClient.isConnected()) networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), blockType);
                            ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                            player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                            placeDelayTimer = PLACE_COOLDOWN;
                            actionConsumed = true;
                        }
                    }
                }
            }
            rightMousePressed = rm;
        }

        if (!inventoryOpen && !paused && !nappingOpen && raycast.isHit() && currentItem != null && currentItem.isBlock()) {
            int blockType = currentItem.getId();
            Vector3f normal = raycast.getNormal();
            BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
            if (!isPlayerAt(player, pPos) && needsPreview(blockType)) {
                byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                renderer.setPreviewBlock(pPos, new Block(blockType, meta));
            } else renderer.setPreviewBlock(null, null);
        } else renderer.setPreviewBlock(null, null);

        return raycast;
    }
    
    private boolean needsPreview(int type) {
        return com.za.minecraft.world.blocks.BlockRegistry.getBlock(type).getPlacementType() != com.za.minecraft.world.blocks.PlacementType.DEFAULT;
    }

    private byte calculateMetadata(int type, Vector3f normal, Vector3f hitPoint, Camera camera) {
        com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(type);
        float yaw = camera.getRotation().y;
        float deg = (float) Math.toDegrees(yaw) % 360;
        if (deg < 0) deg += 360;
        byte viewDir;
        if (deg >= 45 && deg < 135) viewDir = Block.DIR_WEST;
        else if (deg >= 135 && deg < 225) viewDir = Block.DIR_SOUTH;
        else if (deg >= 225 && deg < 315) viewDir = Block.DIR_EAST;
        else viewDir = Block.DIR_NORTH;

        switch (def.getPlacementType()) {
            case SLAB:
                if (verticalMode) return viewDir;
                if (Math.abs(normal.y) > 0.5f) return normal.y > 0 ? Block.DIR_DOWN : Block.DIR_UP;
                float relativeY = hitPoint.y - (float)Math.floor(hitPoint.y);
                return relativeY > 0.5f ? Block.DIR_UP : Block.DIR_DOWN;
            case STAIRS: return viewDir;
            case LOG:
                if (Math.abs(normal.y) > 0.5f) return Block.DIR_UP;
                if (Math.abs(normal.x) > 0.5f) return Block.DIR_EAST;
                if (Math.abs(normal.z) > 0.5f) return Block.DIR_SOUTH;
                return 0;
            default: return 0;
        }
    }
    
    private boolean isPlayerAt(Player player, BlockPos blockPos) {
        Vector3f p = player.getPosition();
        return !(p.x + 0.3f <= blockPos.x() || p.x - 0.3f >= blockPos.x() + 1 || p.y + 1.8f <= blockPos.y() || p.y >= blockPos.y() + 1 || p.z + 0.3f <= blockPos.z() || p.z - 0.3f >= blockPos.z() + 1);
    }
}
