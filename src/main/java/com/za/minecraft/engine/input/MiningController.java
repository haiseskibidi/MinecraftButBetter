package com.za.minecraft.engine.input;

import com.za.minecraft.engine.graphics.Renderer;
import com.za.minecraft.entities.Player;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.MiningSettings;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.physics.AABB;
import com.za.minecraft.world.physics.VoxelShape;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MiningController {
    private Renderer renderer;
    private GameClient networkClient;

    private BlockPos breakingBlockPos = null;
    private float breakingProgress = 0.0f;
    private float hitPulse = 0.0f; // 0 to 1 pulse on hit
    private float blockAccumulatedDamage = 0.0f;
    private float hitCooldownTimer = 0.0f;
    private float wobbleTimer = 0.0f;
    private Vector3f currentWeakSpot = new Vector3f(0.5f);
    private Vector3f currentNormal = new Vector3f(0, 1, 0);
    private final List<Vector3f> hitHistory = new ArrayList<>();
    
    private float breakDelayTimer = 0.0f;

    public MiningController() {
    }

    public void setDependencies(Renderer renderer, GameClient networkClient) {
        this.renderer = renderer;
        this.networkClient = networkClient;
    }

    public void update(float deltaTime) {
        breakDelayTimer = Math.max(0, breakDelayTimer - deltaTime);
        hitCooldownTimer = Math.max(0, hitCooldownTimer - deltaTime);
        hitPulse = Math.max(0, hitPulse - deltaTime * 5.0f); // Fast decay (200ms)
        if (breakingBlockPos != null) {
            wobbleTimer += deltaTime;
        }
    }

    public void startMining(BlockPos hitPos, BlockDefinition blockDef, World world, Vector3f normal) {
        breakingBlockPos = hitPos;
        breakingProgress = 0.0f;
        blockAccumulatedDamage = 0.0f;
        wobbleTimer = 1.0f; // Prevent animation leaking to new blocks
        hitHistory.clear();
        this.currentNormal.set(normal);
        currentWeakSpot = generateRandomWeakSpot(blockDef.getShape(world.getBlock(hitPos).getMetadata()), normal);
        
        // Сразу передаем цвет в рендерер, чтобы не было желтой вспышки в начале
        if (renderer != null) {
            renderer.setBreakingBlock(hitPos, world.getBlock(hitPos), 0.0f, 1.0f, new Vector3f(0.5f), currentWeakSpot, blockDef.getMiningSettings().weakSpotColor(), hitHistory);
        }
    }

    public void stopMining() {
        if (breakingBlockPos != null) {
            breakingBlockPos = null;
            breakingProgress = 0.0f;
            blockAccumulatedDamage = 0.0f;
            if (renderer != null) renderer.setBreakingBlock(null, null, 0.0f, 0.0f, null, null, null, null);
        }
    }

    public void reset() {
        stopMining();
        wobbleTimer = 0.0f;
        hitHistory.clear();
    }

    public void mine(World world, Player player, BlockPos hitPos, int blockType, BlockDefinition blockDef, ItemStack currentStack, Item currentItem, boolean isNewLeftClick, Vector3f localHit, Vector3f normal) {
        float hardness = blockDef.getHardness();
        if (hardness < 0) return; // Unbreakable

        // Если сменилась грань (нормаль), перегенерируем слабую точку на новой грани
        if (normal != null && normal.distanceSquared(currentNormal) > 0.01f) {
            currentNormal.set(normal);
            currentWeakSpot = generateRandomWeakSpot(blockDef.getShape(world.getBlock(hitPos).getMetadata()), normal);
        }

        float maxHealth = hardness * 10.0f;
        boolean isInstantBreak = maxHealth <= 0.1f;
        boolean shouldHit = false;

        float interval = blockDef.getInteractionCooldown();
        if (currentItem != null) {
            com.za.minecraft.world.items.component.ToolComponent tool = currentItem.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
            if (tool != null) interval = tool.attackInterval();
        }
        interval /= Math.max(0.1f, player.getMiningSpeedMultiplier());

        if (isInstantBreak) {
            // Instant break: Always respect breakDelayTimer, NO bypass with clicks
            if (breakDelayTimer <= 0.0f) {
                breakingProgress = 1.0f;
                blockAccumulatedDamage = maxHealth;
                shouldHit = true;
            }
        } else {
            // Re-calculate interval for standard blocks if needed, 
            // but usually we want to stay consistent with tool speeds
            if (hitCooldownTimer <= 0.0f) {
                float miningDamage = (currentItem != null) ?
                    currentItem.getMiningSpeed(blockType) :
                    Items.HAND.getMiningSpeed(blockType);

                MiningSettings mSettings = blockDef.getMiningSettings();
                if (mSettings.strategy().equals("weak_spots")) {
                    float dist = localHit.distance(currentWeakSpot);
                    if (dist < mSettings.precision()) {
                        blockAccumulatedDamage += miningDamage;
                        hitHistory.add(new Vector3f(currentWeakSpot));
                        // Перегенерируем на ТОЙ ЖЕ грани, используя честную нормаль
                        currentWeakSpot = generateRandomWeakSpot(blockDef.getShape(world.getBlock(hitPos).getMetadata()), currentNormal); 
                        com.za.minecraft.utils.Logger.info("Weak spot HIT!");
                    } else {
                        blockAccumulatedDamage += miningDamage * mSettings.missMultiplier();
                    }
                } else {
                    blockAccumulatedDamage += miningDamage;
                }

                breakingProgress = Math.min(1.0f, blockAccumulatedDamage / maxHealth);

                hitCooldownTimer = interval;
                wobbleTimer = 0.0f;
                shouldHit = true;

                player.setContinuousNoise(Math.min(0.4f, 0.15f + hardness * 0.1f));
                player.addNoise(hardness * 0.05f);
            }
        }

        if (shouldHit) {
            if (isInstantBreak) {
                player.interact(interval); // Sync animation duration with physics interval
            } else {
                player.swing(hitCooldownTimer > 0 ? hitCooldownTimer : interval);
            }
            hitPulse = 1.0f;
        }
            
        if (renderer != null && breakingBlockPos != null) {
            renderer.setBreakingBlock(hitPos, world.getBlock(hitPos), breakingProgress, wobbleTimer, localHit, currentWeakSpot, blockDef.getMiningSettings().weakSpotColor(), hitHistory);
        }

        if (breakingProgress >= 1.0f) {
            if (hitPos != null) {
                if (currentItem == null) {
                    if (blockDef.getSoilingAmount() > 0) {
                        player.addDirt(blockDef.getSoilingAmount());
                    }
                }
                if (world.onBlockBreak(hitPos, player)) {
                    java.util.List<com.za.minecraft.world.blocks.DropRule> rules = blockDef.getDropRules();
                    
                    if (!rules.isEmpty()) {
                        String currentToolStr = "none";
                        if (currentItem != null && currentItem.isTool()) {
                            com.za.minecraft.world.items.component.ToolComponent tool = currentItem.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
                            if (tool != null) currentToolStr = tool.type().name().toLowerCase();
                        }
                        
                        for (com.za.minecraft.world.blocks.DropRule rule : rules) {
                            if (rule.requiredToolType().equalsIgnoreCase("none") || rule.requiredToolType().equalsIgnoreCase(currentToolStr)) {
                                if (Math.random() <= rule.chance()) {
                                    Item itemToGive = com.za.minecraft.world.items.ItemRegistry.getItem(com.za.minecraft.utils.Identifier.of(rule.dropItemIdentifier()));
                                    if (itemToGive != null) {
                                        Vector3f dropPos = new Vector3f(hitPos.x() + 0.5f, hitPos.y() + 0.5f, hitPos.z() + 0.5f);
                                        com.za.minecraft.entities.ItemEntity drop = new com.za.minecraft.entities.ItemEntity(dropPos, new ItemStack(itemToGive));
                                        drop.getVelocity().set((float)Math.random() * 0.2f - 0.1f, 0.2f, (float)Math.random() * 0.2f - 0.1f);
                                        drop.setAngularVelocity(new Vector3f((float)(Math.random() - 0.5) * 10f, (float)(Math.random() - 0.5) * 10f, (float)(Math.random() - 0.5) * 10f));
                                        world.spawnEntity(drop);
                                    }
                                }
                            }
                        }
                    } else {
                        String dropId = blockDef.getDropItem();
                        float chance = blockDef.getDropChance();
                        if (Math.random() <= chance) {
                            Item itemToGive = (dropId != null) ? com.za.minecraft.world.items.ItemRegistry.getItem(com.za.minecraft.utils.Identifier.of(dropId)) : com.za.minecraft.world.items.ItemRegistry.getItem(blockDef.getIdentifier());
                            if (itemToGive != null) {
                                Vector3f dropPos = new Vector3f(hitPos.x() + 0.5f, hitPos.y() + 0.5f, hitPos.z() + 0.5f);
                                com.za.minecraft.entities.ItemEntity drop = new com.za.minecraft.entities.ItemEntity(dropPos, new ItemStack(itemToGive));   
                                drop.getVelocity().set((float)Math.random() * 0.2f - 0.1f, 0.2f, (float)Math.random() * 0.2f - 0.1f);
                                drop.setAngularVelocity(new Vector3f((float)(Math.random() - 0.5) * 10f, (float)(Math.random() - 0.5) * 10f, (float)(Math.random() - 0.5) * 10f));
                                world.spawnEntity(drop);
                            }
                        }
                    }

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

                    world.destroyBlock(hitPos, player);
                    if (networkClient != null && networkClient.isConnected()) {
                        networkClient.sendBlockUpdate(hitPos.x(), hitPos.y(), hitPos.z(), Blocks.AIR.getId());
                    }
                } else {
                    breakingProgress = 0;
                }
            }

            stopMining();
            breakDelayTimer = interval; // Use global interval instead of BREAK_COOLDOWN
            hitCooldownTimer = interval;
            if (currentStack != null && currentItem.isTool()) {
                com.za.minecraft.world.items.component.ToolComponent tool = currentItem.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
                if (tool != null && (tool.isEffectiveAgainstAll() || tool.type().name().equalsIgnoreCase(blockDef.getRequiredTool()))) {
                    currentStack.setDurability(currentStack.getDurability() - 1);
                    if (currentStack.getDurability() <= 0) player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                }
            }
        }
    }

    public void renderVisuals(BlockPos hitPos, Block block, Vector3f localHit) {
        if (breakingBlockPos != null && hitPos != null && hitPos.equals(breakingBlockPos)) {
            com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType());
            renderer.setBreakingBlock(hitPos, block, breakingProgress, wobbleTimer, localHit, currentWeakSpot, def.getMiningSettings().weakSpotColor(), hitHistory);
        }
    }

    public BlockPos getBreakingBlockPos() { return breakingBlockPos; }
    
    public float getBreakingProgress() { return breakingProgress; }

    public float getHitPulse() { return hitPulse; }

    private Vector3f generateRandomWeakSpot(VoxelShape shape, Vector3f normal) {
        if (shape == null || shape.getBoxes().isEmpty()) return new Vector3f(0, 0.5f, 0);

        AABB box = shape.getBoxes().get(0);
        Vector3f min = box.getMin();
        Vector3f max = box.getMax();

        float padding = 0.15f;

        Vector3f spot = new Vector3f();
        if (normal.x != 0) {
            spot.x = normal.x > 0 ? max.x : min.x;
            spot.y = min.y + padding + (float) Math.random() * (max.y - min.y - 2 * padding);
            spot.z = min.z + padding + (float) Math.random() * (max.z - min.z - 2 * padding);
        } else if (normal.y != 0) {
            spot.y = normal.y > 0 ? max.y : min.y;
            spot.x = min.x + padding + (float) Math.random() * (max.x - min.x - 2 * padding);
            spot.z = min.z + padding + (float) Math.random() * (max.z - min.z - 2 * padding);
        } else if (normal.z != 0) {
            spot.z = normal.z > 0 ? max.z : min.z;
            spot.x = min.x + padding + (float) Math.random() * (max.x - min.x - 2 * padding);
            spot.y = min.y + padding + (float) Math.random() * (max.y - min.y - 2 * padding);
        } else {
            spot.x = min.x + (float) Math.random() * (max.x - min.x);
            spot.y = min.y + (float) Math.random() * (max.y - min.y);
            spot.z = min.z + (float) Math.random() * (max.z - min.z);
            if (Math.abs(normal.x) > 0.5f) spot.x = normal.x > 0 ? max.x : min.x;
            if (Math.abs(normal.y) > 0.5f) spot.y = normal.y > 0 ? max.y : min.y;
            if (Math.abs(normal.z) > 0.5f) spot.z = normal.z > 0 ? max.z : min.z;
        }

        spot.x -= 0.5f;
        spot.z -= 0.5f;

        return spot;
    }
}