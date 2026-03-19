package com.za.minecraft.world.generation;

import com.za.minecraft.world.World;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.generation.pipeline.GenerationPipeline;
import com.za.minecraft.world.generation.pipeline.steps.CityLayoutStep;
import com.za.minecraft.world.generation.pipeline.steps.BuildingGeneratorStep;

public class TerrainGenerator {
    private final GenerationPipeline pipeline;
    
    public TerrainGenerator(long seed) {
        this.pipeline = new GenerationPipeline();
        
        // Регистрируем шаги пайплайна
        this.pipeline.addStep(new CityLayoutStep(seed));
        this.pipeline.addStep(new BuildingGeneratorStep(seed));
        this.pipeline.addStep(new com.za.minecraft.world.generation.pipeline.steps.OvergrowthStep(seed));
    }
    
    public void generateTerrain(Chunk chunk) {
        pipeline.executeTerrainGeneration(chunk);
    }
    
    public void generateStructures(World world, Chunk chunk) {
        pipeline.executeStructureGeneration(world, chunk);
    }
}
