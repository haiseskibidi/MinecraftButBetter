package com.za.zenith.entities.ai;

/**
 * Possible states for a mob's AI.
 */
public enum AIState {
    /** Doing nothing, just standing still. */
    IDLE,
    /** Moving randomly in the world. */
    WANDER,
    /** Moving towards a sound source or last known location. */
    SEARCH,
    /** Actively chasing the target (player). */
    CHASE,
    /** Attacking the target. */
    ATTACK
}


