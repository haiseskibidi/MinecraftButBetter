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
 * Prioritizes active actions (like mining) over passive detection.
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
    private Identifier lastId = Identifier.of("minecraft:default");
    private float transitionFactor = 1.0f;
    private float stateTimer = 0.0f;

    public void update(float deltaTime) {
        State nextState = determineState();
        if (nextState != currentState) {
            lastId = currentId;
            currentState = nextState;
            updateIdentifier();
            transitionFactor = 0.0f;
            stateTimer = 0.0f;
        }

        if (transitionFactor < 1.0f) {
            transitionFactor = Math.min(1.0f, transitionFactor + deltaTime * 8.0f);
        }
        stateTimer += deltaTime;
    }

    private State determineState() {
        var game = GameLoop.getInstance();
        InputManager input = game.getInputManager();
        
        // --- 1. ACTION PRIORITY (If actively mining, use mining crosshair) ---
        if (input.getMiningController().getBreakingBlockPos() != null) return State.MINING;

        // --- 2. DETECTION PRIORITY ---
        RaycastResult ray = game.getHighlightedBlock();
        com.za.minecraft.entities.Entity hitEntity = input.getHitEntity();

        if (hitEntity instanceof LivingEntity) return State.ATTACK;
        if (hitEntity instanceof ResourceEntity || hitEntity instanceof ItemEntity) return State.INTERACT;

        if (ray != null && ray.isHit()) {
            int type = game.getWorld().getBlock(ray.getBlockPos()).getType();
            BlockDefinition def = BlockRegistry.getBlock(type);
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
    public Identifier getLastId() { return lastId; }
    public float getTransitionFactor() { return transitionFactor; }
    public float getStateTimer() { return stateTimer; }
    public State getCurrentState() { return currentState; }
}
