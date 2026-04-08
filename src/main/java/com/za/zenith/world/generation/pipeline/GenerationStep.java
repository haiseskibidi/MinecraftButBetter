package com.za.zenith.world.generation.pipeline;

import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.World;

/**
 * Interface for a step in the generation pipeline.
 */
public interface GenerationStep {
    /**
     * Modifies the chunk during the initial terrain generation phase.
     * This is used for placing blocks without checking neighbors (fast).
     */
    void generateTerrain(Chunk chunk);
    
    /**
     * Modifies the chunk during the structure generation phase.
     * This is used for placing things like trees or buildings that might cross chunk borders.
     * It uses the World object to check blocks in neighboring chunks.
     */
    default void generateStructures(World world, Chunk chunk) {}
}


