package com.za.zenith.world.generation;

import com.za.zenith.world.World;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.generation.pipeline.GenerationPipeline;
import com.za.zenith.world.generation.pipeline.steps.CityLayoutStep;
import com.za.zenith.world.generation.pipeline.steps.BuildingGeneratorStep;
import com.za.zenith.world.generation.pipeline.steps.OvergrowthStep;
import com.za.zenith.world.generation.pipeline.steps.ScavengeDecorationStep;

public class TerrainGenerator {
    private final GenerationPipeline pipeline;
    
    public TerrainGenerator(long seed) {
        this.pipeline = new GenerationPipeline();
        
        // Регистрируем шаги пайплайна
        this.pipeline.addStep(new CityLayoutStep(seed));
        this.pipeline.addStep(new BuildingGeneratorStep(seed));
        this.pipeline.addStep(new OvergrowthStep(seed));
        this.pipeline.addStep(new ScavengeDecorationStep(seed));
    }
    
    public void generateTerrain(Chunk chunk) {
        pipeline.executeTerrainGeneration(chunk);
    }
    
    public void generateStructures(World world, Chunk chunk) {
        pipeline.executeStructureGeneration(world, chunk);
    }
}


