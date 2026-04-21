package com.za.zenith.engine.input;

import com.za.zenith.utils.Identifier;
import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.core.PlayerMode;
import com.za.zenith.engine.core.Window;
import com.za.zenith.engine.graphics.Camera;
import com.za.zenith.entities.Player;
import com.za.zenith.entities.LivingEntity;
import com.za.zenith.entities.Inventory;
import com.za.zenith.world.World;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.PlacementType;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Items;
import com.za.zenith.world.physics.Raycast;
import com.za.zenith.world.physics.RaycastResult;
import com.za.zenith.world.physics.PhysicsSettings;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    
    private final Vector2f previousPos;
    private final Vector2f currentPos;
    private boolean inWindow = false;
    private boolean fKeyPressed = false;
    private boolean gKeyPressed = false;
    private boolean firstMouse = true;
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private boolean spaceKeyPressed = false;
    private boolean rKeyPressed = false;
    private boolean zKeyPressed = false;
    private boolean f3KeyPressed = false;
    private boolean f9KeyPressed = false;
    private boolean qKeyPressed = false;
    private boolean verticalMode = false;
    private ItemStack heldStack = null;
    private com.za.zenith.entities.inventory.Slot hoveredSlot = null;
    
    // Event handling sync
    private int lastHandledKey = -1;
    private long lastHandledFrame = -1;

    public boolean isKeyHandled(int key) {
        long currentFrame = GameLoop.getInstance().getTimer().getFrames();
        // Allow match for current or previous frame due to callback timing
        return lastHandledKey == key && (lastHandledFrame == currentFrame || lastHandledFrame == currentFrame - 1);
    }

    private void markKeyHandled(int key) {
        lastHandledKey = key;
        lastHandledFrame = GameLoop.getInstance().getTimer().getFrames();
    }
    
    // UI tracking
    private long lastClickTime = 0;
    private int lastClickSlot = -1;
    private boolean[] numKeysPressed = new boolean[9];
    
    private int lastQuickMovedSlot = -1;
    private int lastQuickCopiedDevItem = -1;
    
    // Drag-to-Distribute state
    private final java.util.Set<com.za.zenith.entities.inventory.Slot> draggedSlots = new java.util.LinkedHashSet<>();
    private int dragButton = -1;
    private boolean isDragging = false;
    
    // Breaking block state
    private final MiningController miningController;
    private com.za.zenith.entities.Entity hitEntity;
    private float placeDelayTimer = 0.0f;
    private float lootboxOpeningTimer = 0.0f;
    private com.za.zenith.world.items.ItemStack lootboxStack = null;
    private static final float PLACE_COOLDOWN = 0.25f; // 4 bps (5 ticks)

    public float getLootboxOpeningTimer() { return lootboxOpeningTimer; }
    public com.za.zenith.world.items.ItemStack getLootboxStack() { return lootboxStack; }

    public boolean isActionPressed(String actionId) {
        int keyCode = com.za.zenith.engine.core.SettingsManager.getInstance().getKeyCode(actionId);
        if (keyCode == -1) return false;
        return GameLoop.getInstance().getWindow().isKeyPressed(keyCode);
    }

    public InputManager() {
        previousPos = new Vector2f();
        currentPos = new Vector2f();
        this.miningController = new MiningController();
    }
    
    public MiningController getMiningController() { return miningController; }
    public com.za.zenith.entities.Entity getHitEntity() { return hitEntity; }
    
    public void init(Window window) {
        glfwSetCursorPos(window.getWindowHandle(), window.getWidth() / 2.0, window.getHeight() / 2.0);
        
        currentPos.x = window.getWidth() / 2.0f;
        currentPos.y = window.getHeight() / 2.0f;
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;
        
        glfwSetCursorPosCallback(window.getWindowHandle(), (windowHandle, xpos, ypos) -> {
            currentPos.x = (float) xpos;
            currentPos.y = (float) ypos;

            com.za.zenith.engine.graphics.ui.Screen active = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
            if (active != null) {
                active.handleMouseMove(currentPos.x, currentPos.y);
            }
        });
        
        glfwSetCursorEnterCallback(window.getWindowHandle(), (windowHandle, entered) -> {
            inWindow = entered;
        });

        glfwSetKeyCallback(window.getWindowHandle(), (windowHandle, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                com.za.zenith.engine.graphics.ui.Screen active = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
                if (active != null && active.handleKeyPress(key)) {
                    markKeyHandled(key);
                }
            }
        });

        glfwSetCharCallback(window.getWindowHandle(), (windowHandle, codepoint) -> {
            com.za.zenith.engine.graphics.ui.Screen active = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
            if (active != null) {
                active.handleChar(codepoint);
            }
        });
        
        glfwSetMouseButtonCallback(window.getWindowHandle(), (windowHandle, button, action, mode) -> {
            com.za.zenith.engine.graphics.ui.Screen active = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
            
            if (action == GLFW_PRESS) {
                if (active != null && active.handleMouseClick(currentPos.x, currentPos.y, button)) {
                    if (button == GLFW_MOUSE_BUTTON_1) leftMousePressed = true;
                    if (button == GLFW_MOUSE_BUTTON_2) rightMousePressed = true;
                    return;
                }

                if (GameLoop.getInstance().isInventoryOpen()) {
                    dragButton = button;
                    draggedSlots.clear();
                    isDragging = false;
                    
                    if (heldStack == null) {
                        handleInventoryClick(window, button);
                    } else {
                        com.za.zenith.entities.inventory.Slot slot = getSlotAt(currentPos.x, currentPos.y);
                        if (slot != null) {
                            draggedSlots.add(slot);
                            isDragging = true;
                        } else {
                            handleInventoryClick(window, button);
                        }
                    }
                }
            } else if (action == GLFW_RELEASE) {
                if (active != null && active.handleMouseRelease(currentPos.x, currentPos.y, button)) {
                    if (button == GLFW_MOUSE_BUTTON_1) leftMousePressed = false;
                    if (button == GLFW_MOUSE_BUTTON_2) rightMousePressed = false;
                    return;
                }

                if (GameLoop.getInstance().isInventoryOpen()) {
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
            com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
            if (screen != null && screen.handleScroll(yoffset)) {
                return;
            }
            
            Player p = GameLoop.getInstance().getPlayer();
            if (p != null && !GameLoop.getInstance().isNappingOpen()) {
                if (GameLoop.getInstance().isInventoryOpen() && p.getMode() == PlayerMode.DEVELOPER) {
                    com.za.zenith.engine.graphics.ui.UIRenderer ui = GameLoop.getInstance().getRenderer().getUIRenderer();
                    if (ui.getDevScroller().isMouseOver(currentPos.x, currentPos.y)) {
                        ui.getDevScroller().handleScroll(yoffset);
                        return;
                    }
                }
                
                if (yoffset > 0) p.getInventory().previousSlot();
                else if (yoffset < 0) p.getInventory().nextSlot();
            }
        });
        
        enableMouseCapture(window);
    }
    
    private com.za.zenith.entities.inventory.Slot getSlotAt(float mx, float my) {
        com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (screen instanceof com.za.zenith.engine.graphics.ui.InventoryScreen invScreen) {
            com.za.zenith.engine.graphics.ui.SlotUI ui = invScreen.getSlotAt(mx, my);
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

        Vector3f rightDir = new Vector3f(1, 0, 0)
            .rotateX(camera.getRotation().x)
            .rotateY(camera.getRotation().y)
            .normalize();

        Vector3f downDir = new Vector3f(0, -1, 0)
            .rotateX(camera.getRotation().x)
            .rotateY(camera.getRotation().y)
            .normalize();

        // Offset spawn position slightly to the right and down to simulate throwing from hand
        Vector3f spawnPos = new Vector3f(camera.getPosition())
            .add(new Vector3f(lookDirV).mul(0.5f))
            .add(rightDir.mul(0.3f))
            .add(downDir.mul(0.2f));

        com.za.zenith.entities.ItemEntity itemEntity = new com.za.zenith.entities.ItemEntity(spawnPos, toDrop);

        float throwStrength = 6.0f / toDrop.getItem().getWeight();
        itemEntity.getVelocity().set(lookDirV).mul(throwStrength);
        itemEntity.getVelocity().y += 1.5f / toDrop.getItem().getWeight();

        // Add random tumbling angular velocity
        Vector3f angVel = new Vector3f(
            (float) (Math.random() - 0.5) * 15f, 
            (float) (Math.random() - 0.5) * 15f, 
            (float) (Math.random() - 0.5) * 15f
        );
        itemEntity.setAngularVelocity(angVel);

        world.spawnEntity(itemEntity);
        com.za.zenith.utils.Logger.info("Dropped stack: %s (x%d)", toDrop.getItem().getName(), toDrop.getCount());
    }
    
    private void finishDrag() {
        if (heldStack == null || draggedSlots.isEmpty()) return;

        if (dragButton == GLFW_MOUSE_BUTTON_1) {
            int amountPerSlot = heldStack.getCount() / draggedSlots.size();
            if (amountPerSlot > 0) {
                for (com.za.zenith.entities.inventory.Slot slot : draggedSlots) {
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
            for (com.za.zenith.entities.inventory.Slot slot : draggedSlots) {
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

    private void handleInventoryClickOnSlot(Window window, int button, com.za.zenith.entities.inventory.Slot slot) {
        Player player = GameLoop.getInstance().getPlayer();
        if (player == null) return;

        boolean shift = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || window.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
        long currentTime = System.currentTimeMillis();
        boolean doubleClick = (currentTime - lastClickTime < 250) && (lastClickSlot == slot.getIndex());
        
        lastClickTime = currentTime;
        lastClickSlot = slot.getIndex();

        if (shift && button == GLFW_MOUSE_BUTTON_1) {
            com.za.zenith.engine.graphics.ui.Screen activeScreen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
            if (activeScreen instanceof com.za.zenith.engine.graphics.ui.InventoryScreen invScreen) {
                com.za.zenith.engine.graphics.ui.SlotUI slotUI = invScreen.getSlotAt(currentPos.x, currentPos.y);
                if (slotUI != null) {
                    if (doubleClick) {
                        if (player.getInventory() instanceof Inventory inv) {
                            inv.collectAllTo(slot);
                        }
                    } else {
                        invScreen.onQuickMove(slotUI, player);
                    }
                }
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
                
                // Trigger GUI re-init if accessory slot changed
                if (slot.getIndex() == Inventory.SLOT_ACCESSORY) {
                    com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
                    if (screen != null) {
                        screen.init(window.getWidth(), window.getHeight());
                    }
                }
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
            if (heldStack == null) {
                if (slotStack != null) {
                    int toTake = (int) Math.ceil(slotStack.getCount() / 2.0);
                    heldStack = slotStack.split(toTake);
                    if (slotStack.getCount() <= 0) slot.setStack(null);
                    
                    // Trigger GUI re-init if accessory slot changed (removed)
                    if (slot.getIndex() == Inventory.SLOT_ACCESSORY) {
                        com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
                        if (screen != null) {
                            screen.init(window.getWidth(), window.getHeight());
                        }
                    }
                }
            } else if (slot.isItemValid(heldStack)) {
                if (slotStack == null) {
                    slot.setStack(heldStack.split(1));
                    if (heldStack.getCount() <= 0) heldStack = null;
                    
                    // Trigger GUI re-init if accessory slot changed
                    if (slot.getIndex() == Inventory.SLOT_ACCESSORY) {
                        com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
                        if (screen != null) {
                            screen.init(window.getWidth(), window.getHeight());
                        }
                    }
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

        com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (screen instanceof com.za.zenith.engine.graphics.ui.InventoryScreen invScreen) {
            com.za.zenith.engine.graphics.ui.SlotUI slotUI = invScreen.getSlotAt(mx, my);

            if (slotUI != null) {
                handleInventoryClickOnSlot(window, button, slotUI.getSlot());
            } else {
                // Clicked outside any slots
                boolean handledByDev = false;
                if (player.getMode() == PlayerMode.DEVELOPER) {
                    Item devItem = getDevItemAt(mx, my);
                    if (devItem != null) {
                        handleDevPanelClick(window, mx, my);
                        handledByDev = true;
                    }
                }

                if (!handledByDev && heldStack != null) {
                    dropStack(heldStack, player, GameLoop.getInstance().getWorld(), GameLoop.getInstance().getCamera(), true);
                    heldStack = null;
                }
            }
        }
    }

    private Item getDevItemAt(float mx, float my) {
        com.za.zenith.engine.graphics.ui.renderers.InventoryScreenRenderer invRenderer = GameLoop.getInstance().getRenderer().getUIRenderer().getInventoryScreenRenderer();
        com.za.zenith.engine.graphics.ui.ScrollPanel scroller = invRenderer.getDevScroller();
        
        if (!scroller.isMouseOver(mx, my)) return null;

        com.za.zenith.engine.graphics.ui.Screen screen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (!(screen instanceof com.za.zenith.engine.graphics.ui.PlayerInventoryScreen pScreen)) return null;

        com.za.zenith.engine.graphics.ui.GroupUI devGroup = null;
        for (com.za.zenith.engine.graphics.ui.GroupUI group : pScreen.getGroupsUI()) {
            if ("developer_items".equals(group.getConfig().type)) {
                devGroup = group;
                break;
            }
        }
        if (devGroup == null) return null;

        int cols = devGroup.getConfig().cols > 0 ? devGroup.getConfig().cols : 7;
        int slotSize = (int)(18 * com.za.zenith.engine.graphics.ui.Hotbar.HOTBAR_SCALE);
        int spacing = devGroup.getConfig().spacing;
        int devX = devGroup.getX();
        int startY = devGroup.getY();

        java.util.List<Item> allItems = invRenderer.getFilteredDevItems();
        float offset = scroller.getOffset();

        for (int i = 0; i < allItems.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            int x = devX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing) - (int)offset;
            
            // Only handle clicks on visible items (inside scroller bounds)
            if (my >= scroller.getY() && my <= scroller.getY() + scroller.getHeight()) {
                if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                    return allItems.get(i);
                }
            }
        }
        return null;
    }

    private void handleDevPanelClick(Window window, float mx, float my) {
        Item item = getDevItemAt(mx, my);
        if (item != null) {
            boolean shift = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || window.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
            
            if (shift) {
                Player player = GameLoop.getInstance().getPlayer();
                if (player != null) {
                    player.getInventory().addItem(new ItemStack(item, item.getMaxStackSize()));
                }
            } else {
                // Return logic: if already holding this item, clear it
                if (heldStack != null && heldStack.getItem().getId() == item.getId()) {
                    heldStack = null;
                } else {
                    heldStack = new ItemStack(item, item.getMaxStackSize());
                }
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

    public com.za.zenith.entities.inventory.Slot getHoveredSlot() {
        return hoveredSlot;
    }
    
    public java.util.Set<com.za.zenith.entities.inventory.Slot> getDraggedSlots() {
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
        return miningController.getBreakingProgress();
    }

    public RaycastResult input(Window window, Camera camera, Player player, float deltaTime, com.za.zenith.engine.graphics.Renderer renderer, World world, com.za.zenith.network.GameClient networkClient) {
        miningController.setDependencies(renderer, networkClient);
        
        boolean inventoryOpen = GameLoop.getInstance().isInventoryOpen();
        boolean nappingOpen = GameLoop.getInstance().isNappingOpen();
        boolean paused = GameLoop.getInstance().isPaused();

        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        RaycastResult raycast = Raycast.raycast(world, camera.getPosition(), lookDir);
        ItemStack currentStack = player.getInventory().getSelectedItemStack();
        Item currentItem = currentStack != null ? currentStack.getItem() : null;

        com.za.zenith.world.physics.PhysicsSettings settings = com.za.zenith.world.physics.PhysicsSettings.getInstance();
        com.za.zenith.entities.parkour.ParkourHandler parkour = player.getParkourHandler();

        boolean anyScreen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().isAnyScreenOpen();

        // Блокируем хотбар во время скалывания, инвентаря или любых других экранов (настройки, инспектор)
        if (!nappingOpen && !anyScreen) {
            for (int i = 0; i < 9; i++) {
                if (window.isKeyPressed(GLFW_KEY_1 + i)) {
                    player.getInventory().setSelectedSlot(i);
                    break;
                }
            }
        } else if (inventoryOpen) {
            for (int i = 0; i < 9; i++) {
                boolean pressed = window.isKeyPressed(GLFW_KEY_1 + i);
                if (pressed && !numKeysPressed[i]) {
                    if (hoveredSlot != null) {
                        player.getInventory().swapWithHotbar(hoveredSlot, i);
                    } else if (player.getMode() == PlayerMode.DEVELOPER) {
                        Item devItem = getDevItemAt(currentPos.x, currentPos.y);
                        if (devItem != null) {
                            player.getInventory().copyFromDevPanel(devItem, i);
                        }
                    }
                }
                numKeysPressed[i] = pressed;
            }
        } else {
            // Reset num keys state when neither condition is met
            for (int i = 0; i < 9; i++) numKeysPressed[i] = false;
        }

        if (inventoryOpen || nappingOpen) {
            if (nappingOpen) {
                if (window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1) && !leftMousePressed) {
                    int slotIdx = com.za.zenith.engine.graphics.ui.NappingGUI.getSlotIndexAt(currentPos.x, currentPos.y, window.getWidth(), window.getHeight());
                    if (slotIdx != -1) {
                        com.za.zenith.world.recipes.NappingSession session = GameLoop.getInstance().getNappingSession();
                        session.removePiece(slotIdx);
                        
                        com.za.zenith.world.recipes.NappingRecipe result = session.checkMatch();
                        if (result != null) {
                            com.za.zenith.utils.Logger.info("Napping complete!");
                            
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
                com.za.zenith.engine.graphics.ui.SlotUI slotUI = null;
                com.za.zenith.engine.graphics.ui.Screen active = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
                if (active instanceof com.za.zenith.engine.graphics.ui.InventoryScreen invScreen) {
                    slotUI = invScreen.getSlotAt(currentPos.x, currentPos.y);
                }
                
                com.za.zenith.entities.inventory.Slot newHovered = slotUI != null ? slotUI.getSlot() : null;
                if (newHovered != hoveredSlot) {
                    hoveredSlot = newHovered;
                    
                    // Mouse Tweaks: Shift + Drag quick move
                    if (window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1) && (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || window.isKeyPressed(GLFW_KEY_RIGHT_SHIFT))) {
                        if (slotUI != null && slotUI.getSlot() != null && heldStack == null) {
                            // Use identity hash or similar to distinguish slots with same index in different inventories
                            int slotKey = System.identityHashCode(slotUI.getSlot());
                            if (slotKey != lastQuickMovedSlot) {
                                if (active instanceof com.za.zenith.engine.graphics.ui.InventoryScreen invScreen) {
                                    invScreen.onQuickMove(slotUI, player);
                                    lastQuickMovedSlot = slotKey;
                                }
                            }
                        } else if (slotUI == null && player.getMode() == PlayerMode.DEVELOPER) {
                            Item devItem = getDevItemAt(currentPos.x, currentPos.y);
                            if (devItem != null && devItem.getId() != lastQuickCopiedDevItem) {
                                // Find first free hotbar slot or overwrite if needed? 
                                // Standard creative behavior is to try to add to inventory.
                                player.getInventory().addItem(new ItemStack(devItem, devItem.getMaxStackSize()));
                                lastQuickCopiedDevItem = devItem.getId();
                            }
                        }
                    }

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
        miningController.update(deltaTime);

        Vector2f rotVec = new Vector2f();
        if (firstMouse) {
            previousPos.x = currentPos.x;
            previousPos.y = currentPos.y;
            firstMouse = false;
        } else if (inWindow && !anyScreen && !nappingOpen) {
            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            rotVec.y = (float) -deltaX;
            rotVec.x = (float) -deltaY;
        }
        previousPos.x = currentPos.x;
        previousPos.y = currentPos.y;

        if (!anyScreen && !nappingOpen) {
            float baseSens = 0.002f; // Base sensitivity for 800 DPI
            float currentSens = com.za.zenith.engine.core.SettingsManager.getInstance().getMouseSensitivity() * baseSens;
            float deltaPitch = rotVec.x * currentSens;
            float deltaYaw = rotVec.y * currentSens;

            if (parkour.isRestrictingCamera()) {
                float baseYaw = parkour.getBaseYaw();
                Vector3f currentRot = camera.getRotation();
                
                // Clamp Pitch (X)
                float newPitch = currentRot.x + deltaPitch;
                newPitch = Math.max(-1.4f, Math.min(1.2f, newPitch));
                camera.getRotation().x = newPitch;

                // Clamp Yaw (Y) relative to baseYaw
                float newYaw = currentRot.y + deltaYaw;
                float relativeYaw = newYaw - baseYaw;
                
                // Normalize relativeYaw to -PI to PI
                while (relativeYaw < -Math.PI) relativeYaw += Math.PI * 2;
                while (relativeYaw > Math.PI) relativeYaw -= Math.PI * 2;
                
                float yawLimit = 1.4f; // ~80 degrees
                relativeYaw = Math.max(-yawLimit, Math.min(yawLimit, relativeYaw));
                
                camera.getRotation().y = baseYaw + relativeYaw;
            } else {
                camera.moveRotation(deltaPitch, deltaYaw, 0);
            }
        }

        camera.setOffsets(player.getCameraOffsetX(), player.getCameraOffsetY(), player.getCameraOffsetZ());
        camera.setPitchOffset(player.getCameraPitchOffset());
        camera.setRollOffset(player.getCameraRollOffset());
        camera.setFovOffset(player.getFovOffset());

        Vector2f moveVector = new Vector2f();

        if (!anyScreen && !nappingOpen) {
            if (isActionPressed("move_forward")) moveVector.y = 1;
            if (isActionPressed("move_back")) moveVector.y = -1;
            if (isActionPressed("move_left")) moveVector.x = -1;
            if (isActionPressed("move_right")) moveVector.x = 1;
        }
        
        float moveY = 0;
        boolean spaceDown = isActionPressed("jump");
        boolean spaceNewPress = spaceDown && !spaceKeyPressed;
        spaceKeyPressed = spaceDown;

        if (!anyScreen && !nappingOpen) {
            if (spaceDown) moveY = 1;
        }
        
        boolean shiftPressed = isActionPressed("sneak");
        if (shiftPressed && !anyScreen && !nappingOpen) moveY = -1;
        
        boolean sneaking = shiftPressed && !player.isFlying() && !anyScreen && !nappingOpen;
        player.setSneaking(sneaking);

        boolean inParkour = parkour.isInParkour();

        boolean physicallySneaking = player.isPhysicallySneaking();
        boolean sprinting = isActionPressed("sprint") && !anyScreen && !nappingOpen;
        player.setSprinting(sprinting);
        
        float baseSpeed = player.isFlying() ? settings.flySpeed : (physicallySneaking ? settings.baseMoveSpeed * settings.sneakSpeedMultiplier : settings.baseMoveSpeed);
        if (sprinting && !physicallySneaking) baseSpeed *= (player.isFlying() ? settings.flySprintMultiplier : settings.sprintMultiplier);

        player.setMoving(moveVector.length() > 0);
        if (moveVector.length() > 0 && !inParkour) {
            moveVector.normalize();
            float yaw = -camera.getRotation().y;
            float moveX = (float)Math.sin(yaw) * moveVector.y + (float)Math.cos(yaw) * moveVector.x;
            float moveZ = -(float)Math.cos(yaw) * moveVector.y + (float)Math.sin(yaw) * moveVector.x;
            float targetVx = moveX * baseSpeed;
            float targetVz = moveZ * baseSpeed;
            float accelGain = player.getMode() == PlayerMode.DEVELOPER ? 30.0f : (player.isFlying() ? 24.0f : 18.0f);
            player.applyHorizontalAcceleration((targetVx - player.getVelocity().x) * accelGain * deltaTime, (targetVz - player.getVelocity().z) * accelGain * deltaTime, baseSpeed);
        } else if (!inParkour) {
            float decelGain = player.isFlying() ? 20.0f : 15.0f;
            player.applyHorizontalAcceleration(-player.getVelocity().x * decelGain * deltaTime, -player.getVelocity().z * decelGain * deltaTime, baseSpeed);
        }
        
        if (player.isFlying() && !inParkour) {
            player.addVelocity(0, (moveY * baseSpeed - player.getVelocity().y) * 25.0f * deltaTime, 0);
        }
        
        // Parkour and Jump logic
        if (!anyScreen && !nappingOpen) {
            if (spaceNewPress) {
                if (parkour.isHanging()) {
                    parkour.startClimb(player);
                } else if (!parkour.isClimbing()) {
                    if (player.isOnGround()) {
                        player.jump();
                    } else {
                        parkour.tryLedgeGrab(player, world, lookDir);
                    }
                }
            }

            if (shiftPressed && parkour.isHanging()) {
                // Add a tiny backward impulse when dropping to prevent clipping into the block
                float pushBack = 0.15f;
                float yaw = -camera.getRotation().y;
                player.getVelocity().x = (float) Math.sin(yaw) * -pushBack;
                player.getVelocity().z = (float) Math.cos(yaw) * pushBack;
                
                parkour.cancel(player);
            }
        }
        
        boolean fKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_F);
        if (fKeyCurrentlyPressed && !fKeyPressed && !anyScreen && !nappingOpen) player.setFlying(!player.isFlying());
        fKeyPressed = fKeyCurrentlyPressed;

        boolean f3KeyCurrentlyPressed = isActionPressed("debug_menu");
        if (f3KeyCurrentlyPressed && !f3KeyPressed && !anyScreen && !nappingOpen) {
            boolean visible = !com.za.zenith.engine.core.SettingsManager.getInstance().isDebugOverlayVisible();
            com.za.zenith.engine.core.SettingsManager.getInstance().setDebugOverlayVisible(visible);
            
            PlayerMode newMode = visible ? PlayerMode.DEVELOPER : PlayerMode.SURVIVAL;
            player.setMode(newMode);
            com.za.zenith.utils.Logger.info("Debug HUD: %b, Player mode: %s", visible, newMode);
        }
        f3KeyPressed = f3KeyCurrentlyPressed;

        boolean f9KeyCurrentlyPressed = isActionPressed("live_inspector");
        if (f9KeyCurrentlyPressed && !f9KeyPressed && !anyScreen && !nappingOpen) {
            com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().openScreen(
                new com.za.zenith.engine.graphics.ui.DevInspectorScreen(), window.getWidth(), window.getHeight());
            disableMouseCapture(window);
        }
        f9KeyPressed = f9KeyCurrentlyPressed;
        
        boolean rKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_R);
        if (rKeyCurrentlyPressed && !rKeyPressed && !anyScreen && !nappingOpen) verticalMode = !verticalMode;
        rKeyPressed = rKeyCurrentlyPressed;
        
        boolean gKeyCurrentlyPressed = window.isKeyPressed(GLFW_KEY_G);
        if (gKeyCurrentlyPressed && !gKeyPressed && !anyScreen && !nappingOpen) renderer.toggleFXAA();
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
        
        this.hitEntity = Raycast.raycastEntity(world, camera.getPosition(), lookDir);
        
        if (!anyScreen && !nappingOpen) {
            boolean lm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_1);
            boolean isNewLeftClick = lm && !leftMousePressed;

            if (raycast.isHit()) {
                BlockPos hitPos = raycast.getBlockPos();
                if (miningController.getBreakingBlockPos() != null && !hitPos.equals(miningController.getBreakingBlockPos())) {
                    miningController.stopMining();
                }
            } else {
                if (miningController.getBreakingBlockPos() != null) {
                    miningController.stopMining();
                }
            }

            if (lm) {
                boolean actionConsumed = false;
                if (isNewLeftClick && hitEntity instanceof LivingEntity living) {
                    player.swing();
                    living.takeDamage(2.0f);
                    player.addBlood(0.15f);
                    com.za.zenith.utils.Logger.info("Attacked %s, hands are now bloody", living.getClass().getSimpleName());
                    actionConsumed = true;
                } else if (isNewLeftClick && hitEntity instanceof com.za.zenith.entities.ResourceEntity resource) {
                    if (!player.isSwinging()) {
                        player.interact(PhysicsSettings.getInstance().baseMiningCooldown);
                        player.getInventory().addItem(resource.getStack());
                        resource.setRemoved();
                        com.za.zenith.utils.Logger.info("Picked up resource %s", resource.getStack().getItem().getName());
                    }
                    actionConsumed = true;
                } else if (isNewLeftClick && hitEntity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                    if (!player.isSwinging()) {
                        player.interact(PhysicsSettings.getInstance().baseMiningCooldown);
                        if (player.getInventory().addItem(itemEntity.getStack())) {
                            itemEntity.setRemoved();
                            com.za.zenith.utils.Logger.info("Picked up item %s", itemEntity.getStack().getItem().getName());
                        }
                    }
                    actionConsumed = true;
                }

                if (!actionConsumed && raycast.isHit()) {
                    BlockPos hitPos = raycast.getBlockPos();
                    int blockType = world.getBlock(hitPos).getType();
                    BlockDefinition blockDef = BlockRegistry.getBlock(blockType);
                    
                    float rx = raycast.getHitPoint().x - hitPos.x() - 0.5f;
                    float ry = raycast.getHitPoint().y - hitPos.y();
                    float rz = raycast.getHitPoint().z - hitPos.z() - 0.5f;
                    Vector3f localHit = new Vector3f(rx, ry, rz);

                    if (blockDef.onLeftClick(world, hitPos, player, currentStack, rx + 0.5f, ry, rz + 0.5f, isNewLeftClick)) {
                        leftMousePressed = true;
                        return null; 
                    }

                    if (miningController.getBreakingBlockPos() == null) {
                        miningController.startMining(hitPos, blockDef, world, raycast.getNormal());
                    }

                    miningController.mine(world, player, hitPos, blockType, blockDef, currentStack, currentItem, isNewLeftClick, localHit, raycast.getNormal());
                }
            } else {
                if (raycast.isHit()) {
                    Block block = world.getBlock(raycast.getBlockPos());
                    float rx = raycast.getHitPoint().x - raycast.getBlockPos().x();
                    float ry = raycast.getHitPoint().y - raycast.getBlockPos().y();
                    float rz = raycast.getHitPoint().z - raycast.getBlockPos().z();
                    miningController.renderVisuals(raycast.getBlockPos(), block, new Vector3f(rx, ry, rz), world);
                }
            }
            leftMousePressed = lm;
            
            boolean rm = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_2);
            boolean isNewRightClick = rm && !rightMousePressed;
            
            if (rm) {
                boolean actionConsumed = false;

                // Lootbox Opening Logic
                if (currentStack != null) {
                    com.za.zenith.world.items.component.LootboxComponent lootbox = currentStack.getItem().getComponent(com.za.zenith.world.items.component.LootboxComponent.class);
                    if (lootbox != null) {
                        if (isNewRightClick || lootboxStack != currentStack) {
                            lootboxOpeningTimer = 0;
                            lootboxStack = currentStack;
                            com.za.zenith.utils.Logger.info("Starting to open tactical case: %s", currentStack.getDisplayName());
                        }

                        lootboxOpeningTimer += deltaTime;

                        if (lootboxOpeningTimer >= lootbox.openingTime()) {
                            java.util.List<ItemStack> rewards = com.za.zenith.world.items.loot.LootGenerator.generateFromCase(currentStack);

                            // Remove one case from hand
                            if (currentStack.getCount() > 1) {
                                currentStack.setCount(currentStack.getCount() - 1);
                            } else {
                                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                            }

                            // Add rewards
                            for (ItemStack reward : rewards) {
                                if (!player.getInventory().addItem(reward)) {
                                    dropStack(reward, player, world, camera, true);
                                }
                                com.za.zenith.utils.Logger.info("Unpacked reward: %s", reward.getDisplayName());
                            }

                            lootboxOpeningTimer = 0;
                            lootboxStack = null;
                        }
                        actionConsumed = true;
                    } else {
                        lootboxOpeningTimer = 0;
                        lootboxStack = null;
                    }
                } else {
                    lootboxOpeningTimer = 0;
                    lootboxStack = null;
                }

                // Entity Interaction (RMB Pickup)
                if (!actionConsumed && isNewRightClick) {
                    if (hitEntity instanceof com.za.zenith.entities.ResourceEntity resource) {
                        if (!player.isSwinging()) {
                            float cooldown = resource.getStack().getItem().getInteractionCooldown();
                            player.interact(cooldown);
                            if (player.getInventory().addItem(resource.getStack())) {
                                resource.setRemoved();
                                com.za.zenith.utils.Logger.info("Picked up %s (RMB)", resource.getStack().getItem().getName());
                                actionConsumed = true;
                                placeDelayTimer = PLACE_COOLDOWN;
                            }
                        }
                    } else if (hitEntity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                        if (!player.isSwinging()) {
                            float cooldown = itemEntity.getStack().getItem().getInteractionCooldown();
                            player.interact(cooldown);
                            if (player.getInventory().addItem(itemEntity.getStack())) {
                                itemEntity.setRemoved();
                                com.za.zenith.utils.Logger.info("Picked up %s (RMB)", itemEntity.getStack().getItem().getName());
                                actionConsumed = true;
                                placeDelayTimer = PLACE_COOLDOWN;
                            }
                        }
                    }
                }

                if (!actionConsumed && raycast.isHit() && isNewRightClick) {
                    BlockPos hitPos = raycast.getBlockPos();
                    int hitBlockType = world.getBlock(hitPos).getType();
                    BlockDefinition blockDef = BlockRegistry.getBlock(hitBlockType);
                    
                    float rx = raycast.getHitPoint().x - hitPos.x();
                    float ry = raycast.getHitPoint().y - hitPos.y();
                    float rz = raycast.getHitPoint().z - hitPos.z();

                    if (blockDef != null) {
                        if (blockDef.getCleaningAmount() > 0) {
                            if (blockDef.getCleaningAmount() >= 1.0f) {
                                player.washHands();
                                com.za.zenith.utils.Logger.info("Washed hands");
                            } else {
                                player.addDirt(-blockDef.getCleaningAmount());
                                com.za.zenith.utils.Logger.info("Cleaned hands slightly");
                            }
                            actionConsumed = true;
                        }

                        if (!actionConsumed && blockDef.onUse(world, hitPos, player, currentStack, rx, ry, rz)) {
                            actionConsumed = true;
                            placeDelayTimer = PLACE_COOLDOWN; // Prevent accidental placement on next frame
                        }
                    }
                }

                if (!actionConsumed && isNewRightClick && currentStack != null) {
                    if (currentStack.getCount() >= 2) {
                        java.util.List<com.za.zenith.world.recipes.IRecipe> nappingRecipes = com.za.zenith.world.recipes.RecipeRegistry.getRecipesByType("napping");
                        
                        boolean hasNapping = false;
                        for (com.za.zenith.world.recipes.IRecipe r : nappingRecipes) {
                            com.za.zenith.world.recipes.NappingRecipe nr = (com.za.zenith.world.recipes.NappingRecipe) r;
                            if (nr.isInputValid(currentItem.getIdentifier())) {
                                hasNapping = true;
                                break;
                            }
                        }
                        
                        if (hasNapping) {
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
                                    world.setBlock(pPos, new Block(blockType, (byte)0));
                                    world.setBlock(topPos, new Block(blockType, (byte)1));
                                    
                                    if (networkClient != null && networkClient.isConnected()) {
                                        networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), blockType);
                                        networkClient.sendBlockUpdate(topPos.x(), topPos.y(), topPos.z(), blockType);
                                    }
                                    
                                    ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                                    player.place();
                                    placeDelayTimer = PLACE_COOLDOWN;
                                    actionConsumed = true;
                                }
                            } else {
                                byte meta = calculateMetadata(blockType, normal, raycast.getHitPoint(), camera);
                                world.setBlock(pPos, new Block(blockType, meta));
                                if (networkClient != null && networkClient.isConnected()) networkClient.sendBlockUpdate(pPos.x(), pPos.y(), pPos.z(), blockType);
                                ItemStack newStack = currentStack.getCount() > 1 ? new ItemStack(currentItem, currentStack.getCount() - 1) : null;
                                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                                player.place();
                                placeDelayTimer = PLACE_COOLDOWN;
                                actionConsumed = true;
                            }
                        }
                    }
                }
            } else {
                lootboxOpeningTimer = 0;
                lootboxStack = null;
            }
            rightMousePressed = rm;
        }

        if (!anyScreen && !nappingOpen && player.isSneaking() && raycast.isHit() && currentItem != null && currentItem.isBlock() && !isSpecialInteracting(player, raycast, currentStack)) {
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

    private boolean isSpecialInteracting(Player player, RaycastResult raycast, ItemStack currentStack) {
        if (!player.isSneaking() || !raycast.isHit() || currentStack == null) return false;
        
        Block hitBlock = GameLoop.getInstance().getWorld().getBlock(raycast.getBlockPos());
        BlockDefinition hitDef = BlockRegistry.getBlock(hitBlock.getType());
        if (hitDef == null) return false;
        
        Identifier hitId = hitDef.getIdentifier();
        Item currentItem = currentStack.getItem();
        Identifier itemId = currentItem.getIdentifier();
        Vector3f normal = raycast.getNormal();

        // 1. Unfired Vessel + Straw (Only from top)
        if (hitId.equals(Blocks.UNFIRED_VESSEL.getIdentifier()) && itemId.equals(Items.STRAW.getIdentifier())) {
            return normal.y > 0.5f;
        }
        
        // 2. Pit Kiln (Normal or Burning) + Logs/Fire Starter
        if (hitId.equals(Blocks.PIT_KILN.getIdentifier()) || hitId.equals(Blocks.BURNING_PIT_KILN.getIdentifier())) {
            if (itemId.getPath().contains("log") || itemId.equals(Items.FIRE_STARTER.getIdentifier())) {
                // Возвращаем true для всех сторон, чтобы заблокировать превью блока при шифте
                return true;
            }
        }
        
        return false;
    }
    
    private boolean needsPreview(int type) {
        return com.za.zenith.world.blocks.BlockRegistry.getBlock(type).getPlacementType() != com.za.zenith.world.blocks.PlacementType.DEFAULT;
    }

    private byte calculateMetadata(int type, Vector3f normal, Vector3f hitPoint, Camera camera) {
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(type);
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


