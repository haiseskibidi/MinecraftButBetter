package com.za.zenith.world.generation.pipeline;

import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.World;
import java.util.ArrayList;
import java.util.List;

public class GenerationPipeline {
    private final List<GenerationStep> steps = new ArrayList<>();
    
    public void addStep(GenerationStep step) {
        steps.add(step);
    }
    
    public void executeTerrainGeneration(Chunk chunk) {
        for (GenerationStep step : steps) {
            step.generateTerrain(chunk);
        }
    }
    
    public void executeStructureGeneration(World world, Chunk chunk) {
        for (GenerationStep step : steps) {
            step.generateStructures(world, chunk);
        }
    }
}


