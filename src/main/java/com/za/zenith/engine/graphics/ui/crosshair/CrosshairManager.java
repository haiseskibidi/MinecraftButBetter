package com.za.zenith.engine.graphics.ui.crosshair;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.input.InputManager;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.physics.RaycastResult;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.entities.ResourceEntity;
import com.za.zenith.entities.ItemEntity;
import com.za.zenith.entities.LivingEntity;

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
    private Identifier currentId = Identifier.of("zenith:default");
    private Identifier lastId = Identifier.of("zenith:default");
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
        com.za.zenith.entities.Entity hitEntity = input.getHitEntity();

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
            case INTERACT: currentId = Identifier.of("zenith:interact"); break;
            case ATTACK: currentId = Identifier.of("zenith:attack"); break;
            case MINING: currentId = Identifier.of("zenith:mining"); break;
            default: currentId = Identifier.of("zenith:default"); break;
        }
    }

    public Identifier getCurrentId() { return currentId; }
    public Identifier getLastId() { return lastId; }
    public float getTransitionFactor() { return transitionFactor; }
    public float getStateTimer() { return stateTimer; }
    public State getCurrentState() { return currentState; }
}


