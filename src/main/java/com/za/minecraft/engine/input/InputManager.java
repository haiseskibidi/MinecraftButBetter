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
    private com.za.minecraft.entities.inventory.Slot hoveredSlot = null;
    
    // Drag-to-Distribute state
    private java.util.Set<com.za.minecraft.entities.inventory.Slot> draggedSlots = new java.util.LinkedHashSet<>();
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
                        com.za.minecraft.entities.inventory.Slot slot = getSlotAt(currentPos.x, currentPos.y);
                        if (slot != null) {
                            draggedSlots.add(slot);
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
    
    private com.za.minecraft.entities.inventory.Slot getSlotAt(float mx, float my) {
        com.za.minecraft.engine.graphics.ui.InventoryScreen screen = com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (screen != null) {
            com.za.minecraft.engine.graphics.ui.SlotUI ui = screen.getSlotAt(mx, my);
            return ui != null ? ui.getSlot() : null;
        }
        return null;
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

        if (dragButton == GLFW_MOUSE_BUTTON_1) {
            int amountPerSlot = heldStack.getCount() / draggedSlots.size();
            if (amountPerSlot > 0) {
                for (com.za.minecraft.entities.inventory.Slot slot : draggedSlots) {
                    if (!slot.isItemValid(heldStack)) continue;
                    ItemStack slotStack = slot.getStack();
                    if (slotStack == null) {
                        slot.setStack(heldStack.split(amountPerSlot));
                    } else if (heldStack.isStackableWith(slotStack)) {
                        ItemStack split = heldStack.split(amountPerSlot);
                        if (split != null) {
                            slotStack.setCount(slotStack.getCount() + split.getCount());
                        }
                    }
                }
            }
        } else if (dragButton == GLFW_MOUSE_BUTTON_2) {
            for (com.za.minecraft.entities.inventory.Slot slot : draggedSlots) {
                if (heldStack.getCount() <= 0) break;
                if (!slot.isItemValid(heldStack)) continue;
                ItemStack slotStack = slot.getStack();
                if (slotStack == null) {
                    slot.setStack(heldStack.split(1));
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

    private void handleInventoryClickOnSlot(Window window, int button, com.za.minecraft.entities.inventory.Slot slot) {
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;

        boolean shift = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || window.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
        if (shift && button == GLFW_MOUSE_BUTTON_1) {
            if (player.getInventory() instanceof Inventory inv) {
                inv.quickMove(slot.getIndex());
            }
            return;
        }

        ItemStack slotStack = slot.getStack();
        
        if (button == GLFW_MOUSE_BUTTON_1) {
            if (heldStack != null && slotStack != null && heldStack.isStackableWith(slotStack)) {
                slotStack.setCount(slotStack.getCount() + heldStack.getCount());
                heldStack = null;
            } else if (slot.isItemValid(heldStack)) {
                slot.setStack(heldStack);
                heldStack = slotStack;
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
            if (heldStack == null) {
                if (slotStack != null) {
                    int toTake = (int) Math.ceil(slotStack.getCount() / 2.0);
                    heldStack = slotStack.split(toTake);
                    if (slotStack.getCount() <= 0) slot.setStack(null);
                }
            } else if (slot.isItemValid(heldStack)) {
                if (slotStack == null) {
                    slot.setStack(heldStack.split(1));
                    if (heldStack.getCount() <= 0) heldStack = null;
                } else if (heldStack.isStackableWith(slotStack)) {
                    slotStack.setCount(slotStack.getCount() + 1);
                    heldStack.setCount(heldStack.getCount() - 1);
                    if (heldStack.getCount() <= 0) heldStack = null;
                } else {
                    slot.setStack(heldStack);
                    heldStack = slotStack;
                }
            }
        }
    }

    private void handleInventoryClick(Window window, int button) {
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;
        
        float mx = currentPos.x;
        float my = currentPos.y;

        com.za.minecraft.engine.graphics.ui.InventoryScreen activeScreen = com.za.minecraft.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (activeScreen == null) return;

        com.za.minecraft.engine.graphics.ui.SlotUI slotUI = activeScreen.getSlotAt(mx, my);

        if (slotUI != null) {
            handleInventoryClickOnSlot(window, button, slotUI.getSlot());
        } else {
            if (heldStack != null) {
                dropStack(heldStack, player, GameLoop.getInstance().getWorld(), GameLoop.getInstance().getCamera(), true);
                heldStack = null;
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

    public com.za.minecraft.entities.inventory.Slot getHoveredSlot() {
        return hoveredSlot;
    }
    
    public java.util.Set<com.za.minecraft.entities.inventory.Slot> getDraggedSlots() {
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
                com.za.minecraft.entities.inventory.Slot newHovered = getSlotAt(currentPos.x, currentPos.y);
                if (newHovered != hoveredSlot) {
                    hoveredSlot = newHovered;
                    if (isDragging && dragButton != -1 && heldStack != null && hoveredSlot != null) {
                        ItemStack slotStack = hoveredSlot.getStack();
                        boolean canReceive = (slotStack == null || heldStack.isStackableWith(slotStack));
                        if (canReceive && (draggedSlots.contains(hoveredSlot) || draggedSlots.size() < heldStack.getCount())) {
                            if (hoveredSlot.isItemValid(heldStack)) {
                                draggedSlots.add(hoveredSlot);
                            }
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
            hoveredSlot = null;
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
                if (hoveredSlot != null) {
                    ItemStack stack = hoveredSlot.getStack();
                    if (stack != null) {
                        boolean ctrlPressed = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
                        dropStack(stack, player, world, camera, ctrlPressed);
                        if (stack.getCount() <= 0) hoveredSlot.setStack(null);
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
                            java.util.List<com.za.minecraft.world.blocks.DropRule> rules = blockDef.getDropRules();
                            boolean customDropHandled = false;
                            
                            if (!rules.isEmpty()) {
                                String currentToolStr = "none";
                                if (currentItem != null && currentItem.isTool()) {
                                    com.za.minecraft.world.items.component.ToolComponent tool = currentItem.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
                                    if (tool != null) currentToolStr = tool.type().name().toLowerCase();
                                }
                                
                                for (com.za.minecraft.world.blocks.DropRule rule : rules) {
                                    // Проверяем, подходит ли инструмент для этого правила (none = любой)
                                    if (rule.requiredToolType().equalsIgnoreCase("none") || rule.requiredToolType().equalsIgnoreCase(currentToolStr)) {
                                        if (Math.random() <= rule.chance()) {
                                            Item itemToGive = ItemRegistry.getItem(Identifier.of(rule.dropItemIdentifier()));
                                            if (itemToGive != null) {
                                                Vector3f dropPos = new Vector3f(hitPos.x() + 0.5f, hitPos.y() + 0.5f, hitPos.z() + 0.5f);
                                                com.za.minecraft.entities.ItemEntity drop = new com.za.minecraft.entities.ItemEntity(dropPos, new ItemStack(itemToGive));
                                                drop.getVelocity().set((float)Math.random() * 0.2f - 0.1f, 0.2f, (float)Math.random() * 0.2f - 0.1f);
                                                world.spawnEntity(drop);
                                                customDropHandled = true;
                                            }
                                        }
                                    }
                                }
                            }

                            // Если правил нет, используем старую (Legacy) логику дропа
                            if (rules.isEmpty()) {
                                String dropId = blockDef.getDropItem();
                                float chance = blockDef.getDropChance();
                                
                                if (Math.random() <= chance) {
                                    Item itemToGive = (dropId != null) ? ItemRegistry.getItem(Identifier.of(dropId)) : ItemRegistry.getItem(blockDef.getIdentifier());
                                    
                                    if (itemToGive != null) {
                                        Vector3f dropPos = new Vector3f(hitPos.x() + 0.5f, hitPos.y() + 0.5f, hitPos.z() + 0.5f);
                                        com.za.minecraft.entities.ItemEntity drop = new com.za.minecraft.entities.ItemEntity(dropPos, new ItemStack(itemToGive));
                                        drop.getVelocity().set((float)Math.random() * 0.2f - 0.1f, 0.2f, (float)Math.random() * 0.2f - 0.1f);
                                        world.spawnEntity(drop);
                                    }
                                }
                            }
                            
                            // Если это двойное растение - ломаем и соседа
                            if (blockDef.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.DOUBLE_PLANT) {
                                int meta = world.getBlock(hitPos).getMetadata();
                                BlockPos otherPos = (meta == 0) ? hitPos.up() : hitPos.down();
                                if (world.getBlock(otherPos).getType() == blockType) {
                                    world.setBlock(otherPos, new Block(com.za.minecraft.world.blocks.Blocks.AIR.getId()));
                                    if (networkClient != null && networkClient.isConnected()) {
                                        networkClient.sendBlockUpdate(otherPos.x(), otherPos.y(), otherPos.z(), com.za.minecraft.world.blocks.Blocks.AIR.getId());
                                    }
                                }
                            }

                            world.setBlock(hitPos, new Block(com.za.minecraft.world.blocks.Blocks.AIR.getId()));                            if (networkClient != null && networkClient.isConnected()) {
                                networkClient.sendBlockUpdate(hitPos.x(), hitPos.y(), hitPos.z(), Blocks.AIR.getId());
                            }
                            breakingBlockPos = null;
                            breakingProgress = 0.0f;
                            breakDelayTimer = BREAK_COOLDOWN;
                            if (currentStack != null && currentItem.isTool()) {
                                com.za.minecraft.world.items.component.ToolComponent tool = currentItem.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
                                if (tool != null && (tool.isEffectiveAgainstAll() || tool.type().name().equalsIgnoreCase(blockDef.getRequiredTool()))) {
                                    currentStack.setDurability(currentStack.getDurability() - 1);
                                    if (currentStack.getDurability() <= 0) player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                                }
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
                    BlockDefinition blockDef = BlockRegistry.getBlock(hitBlockType);
                    
                    if (blockDef != null && blockDef.onUse(world, hitPos, player, currentStack)) {
                        actionConsumed = true;
                    }
                    
                    // --- Log to Stump Transformation ---
                    if (!actionConsumed && hitBlockType == Blocks.OAK_LOG.getId() && currentItem != null && currentItem.getId() == Items.STONE_AXE.getId()) {
                        world.setBlock(hitPos, new Block(Blocks.STUMP.getId()));
                        com.za.minecraft.utils.Logger.info("Created a stump from log");
                        actionConsumed = true;
                    }
                    
                    // --- Pit Kiln Logic ---
                    if (player.isSneaking() && isNewRightClick) {
                        if (hitBlockType == Blocks.UNFIRED_VESSEL.getId()) {
                            if (currentItem != null && currentItem.getId() == Items.STRAW.getId()) {
                                world.setBlock(hitPos, new Block(Blocks.PIT_KILN.getId(), (byte)0));
                                player.getInventory().consumeSelected(1);
                                actionConsumed = true;
                            }
                        } else if (hitBlockType == Blocks.PIT_KILN.getId()) {
                            Block block = world.getBlock(hitPos);
                            int logs = block.getMetadata();
                            
                            if (logs < 4 && currentItem != null && currentItem.getIdentifier().getPath().contains("log")) {
                                // Позиции для бревен "плюсиком"
                                float[][] offsets = {{0.25f, 0, 0}, {-0.25f, 0, 0}, {0, 0, 0.25f}, {0, 0, -0.25f}};
                                float[] rotations = {0, 0, 1.57f, 1.57f};
                                // Добавляем микро-смещение по Y для второй пары бревен (индексы 2 и 3), чтобы избежать Z-fighting
                                float yOffset = (logs >= 2) ? 0.005f : 0.0f;
                                
                                Vector3f pos = new Vector3f(hitPos.x() + 0.5f + offsets[logs][0], hitPos.y() + 0.05f + yOffset, hitPos.z() + 0.5f + offsets[logs][2]);
                                world.spawnEntity(new com.za.minecraft.entities.DecorationEntity(pos, Identifier.of("minecraft:log_pile"), rotations[logs]));
                                
                                world.setBlock(hitPos, new Block(Blocks.PIT_KILN.getId(), (byte)(logs + 1)));
                                player.getInventory().consumeSelected(1);
                                actionConsumed = true;
                            } else if (logs == 4 && currentItem != null && currentItem.getId() == Items.FIRE_STARTER.getId()) {
                                // Удаляем сущности бревен в радиусе блока
                                world.getEntities().stream()
                                    .filter(e -> e instanceof com.za.minecraft.entities.DecorationEntity)
                                    .filter(e -> e.getPosition().distance(new Vector3f(hitPos.x() + 0.5f, hitPos.y(), hitPos.z() + 0.5f)) < 1.0f)
                                    .forEach(e -> e.setRemoved());
                                
                                world.setBlock(hitPos, new Block(Blocks.BURNING_PIT_KILN.getId()));
                                world.setBlockEntity(new com.za.minecraft.world.blocks.entity.PitKilnBlockEntity(hitPos));
                                // В будущем уменьшить прочность огнива
                                actionConsumed = true;
                            }
                        }
                    }

                    if (!actionConsumed && hitBlockType == Blocks.CAMPFIRE.getId()) {
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
                
                if (!actionConsumed && (isNewRightClick || placeDelayTimer <= 0) && raycast.isHit() && !isSpecialInteracting(player, raycast, currentStack)) {
                    if (currentItem != null && currentItem.isBlock()) {
                        int blockType = currentItem.getId();
                        BlockDefinition def = BlockRegistry.getBlock(blockType);
                        Vector3f normal = raycast.getNormal();
                        BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
                        
                        if (!isPlayerAt(player, pPos) && world.getBlock(pPos).isReplaceable()) {
                            if (def.getPlacementType() == PlacementType.DOUBLE_PLANT) {
                                BlockPos topPos = pPos.up();
                                if (world.getBlock(topPos).isReplaceable() && !isPlayerAt(player, topPos)) {
                                    // Ставим низ (meta 0) и верх (meta 1)
                                    world.setBlock(pPos, new Block(blockType, (byte)0));
                                    world.setBlock(topPos, new Block(blockType, (byte)1));
                                    
                                    if (networkClient != null && networkClient.isConnected()) {
                                        networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), blockType);
                                        networkClient.sendBlockUpdate(topPos.x(), topPos.y(), topPos.z(), blockType);
                                    }
                                    
                                    ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                                    placeDelayTimer = PLACE_COOLDOWN;
                                    actionConsumed = true;
                                }
                            } else {
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
            }
            rightMousePressed = rm;
        }

        if (!inventoryOpen && !paused && !nappingOpen && raycast.isHit() && currentItem != null && currentItem.isBlock() && !isSpecialInteracting(player, raycast, currentStack)) {
            int blockType = currentItem.getId();
            Vector3f normal = raycast.getNormal();
            BlockPos pPos = new BlockPos(raycast.getBlockPos().x() + (int)normal.x, raycast.getBlockPos().y() + (int)normal.y, raycast.getBlockPos().z() + (int)normal.z);
            if (!isPlayerAt(player, pPos) && world.getBlock(pPos).isReplaceable() && needsPreview(blockType)) {
                byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                renderer.setPreviewBlock(pPos, new Block(blockType, meta));
            } else renderer.setPreviewBlock(null, null);
        } else renderer.setPreviewBlock(null, null);

        return raycast;
    }

    /**
     * Проверяет, выполняется ли сейчас специальное взаимодействие с блоком,
     * которое должно подавлять стандартные действия (например, превью установки блока).
     */
    private boolean isSpecialInteracting(Player player, RaycastResult raycast, ItemStack currentStack) {
        if (!player.isSneaking() || !raycast.isHit() || currentStack == null) return false;

        int hitBlockType = GameLoop.getInstance().getWorld().getBlock(raycast.getBlockPos()).getType();
        Item currentItem = currentStack.getItem();

        // Условия для Pit Kiln
        if (hitBlockType == Blocks.UNFIRED_VESSEL.getId() && currentItem.getId() == Items.STRAW.getId()) return true;
        if (hitBlockType == Blocks.PIT_KILN.getId()) {
            if (currentItem.getIdentifier().getPath().contains("log")) return true;
            if (currentItem.getId() == Items.FIRE_STARTER.getId()) return true;
        }

        return false;
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
