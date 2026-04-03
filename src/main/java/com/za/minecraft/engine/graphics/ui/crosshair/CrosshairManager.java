package com.za.minecraft.engine.graphics.ui.crosshair;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.input.InputManager;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.physics.RaycastResult;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.entities.ResourceEntity;
import com.za.minecraft.entities.ItemEntity;
import com.za.minecraft.entities.LivingEntity;

/**
 * Manages the current state and selection of the crosshair.
 */
public class CrosshairManager {
    public enum State {
        DEFAULT,
        INTERACT,
        ATTACK,
        MINING
    }

    private State currentState = State.DEFAULT;
    private Identifier currentId = Identifier.of("minecraft:default");
    private float stateTransition = 0.0f; // For animations if needed

    public void update(float deltaTime) {
        State nextState = determineState();
        if (nextState != currentState) {
            currentState = nextState;
            updateIdentifier();
        }
    }

    private State determineState() {
        InputManager input = GameLoop.getInstance().getInputManager();
        RaycastResult ray = GameLoop.getInstance().getHighlightedBlock();
        com.za.minecraft.entities.Entity hitEntity = input.getHitEntity();

        // 1. ACTIVE ACTION PRIORITY
        // If we are already mining, don't switch crosshair even if we look at a cow or a chest
        if (input.getMiningController().getBreakingBlockPos() != null) return State.MINING;

        // 2. Attack Priority (Entities)
        if (hitEntity instanceof LivingEntity) return State.ATTACK;

        // 3. Interact Priority (Entities)
        if (hitEntity instanceof ResourceEntity || hitEntity instanceof ItemEntity) return State.INTERACT;

        // 4. Interact Priority (Blocks)
        if (ray != null && ray.isHit()) {
            BlockDefinition def = BlockRegistry.getBlock(ray.getBlock().getType());
            if (def != null && def.hasOnUse()) return State.INTERACT;
        }

        return State.DEFAULT;
    }

    private void updateIdentifier() {
        switch (currentState) {
            case INTERACT: currentId = Identifier.of("minecraft:interact"); break;
            case ATTACK: currentId = Identifier.of("minecraft:attack"); break;
            case MINING: currentId = Identifier.of("minecraft:mining"); break;
            default: currentId = Identifier.of("minecraft:default"); break;
        }
    }

    public Identifier getCurrentId() { return currentId; }
    public State getCurrentState() { return currentState; }
}
