package com.za.zenith.world.generation.density;

import com.google.gson.JsonElement;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.generation.BiomeGenerator;
import com.za.zenith.world.generation.density.DensityFunction;

public class NoiseRouter {
    private final BiomeGenerator biomeGenerator;
    private final DensityFunction finalDensityFunction;
    
    // Fallback if no json is defined
    private final com.za.zenith.world.generation.SimplexNoise fallbackNoise3D;
    private final com.za.zenith.world.generation.SimplexNoise fallbackDetailNoise;
    private final com.za.zenith.world.generation.SimplexNoise fallbackOverhangNoise;

    public NoiseRouter(long seed, BiomeGenerator biomeGenerator) {
        this.biomeGenerator = biomeGenerator;
        
        JsonElement rootJson = DensityFunctionRegistry.get(Identifier.of("zenith:final_density"));
        if (rootJson != null) {
            DensityFunctionParser parser = new DensityFunctionParser(seed);
            this.finalDensityFunction = parser.parse(rootJson);
            
            this.fallbackNoise3D = null;
            this.fallbackDetailNoise = null;
            this.fallbackOverhangNoise = null;
        } else {
            com.za.zenith.utils.Logger.warn("zenith:final_density function not found. Using fallback hardcoded noise.");
            this.finalDensityFunction = null;
            this.fallbackNoise3D = new com.za.zenith.world.generation.SimplexNoise(seed + 100);
            this.fallbackDetailNoise = new com.za.zenith.world.generation.SimplexNoise(seed + 200);
            this.fallbackOverhangNoise = new com.za.zenith.world.generation.SimplexNoise(seed + 300);
        }
    }

    public double getDensity(DensityContext ctx) {
        if (finalDensityFunction != null) {
            return finalDensityFunction.compute(ctx);
        }
        
        // Fallback logic
        double offset = biomeGenerator.getBaseHeight(ctx.continentalness());
        double factor = biomeGenerator.getTerrainFactor(ctx.erosion());
        double weirdness = ctx.weirdness();
        
        double density = offset - ctx.y();
        double terrainShape = fallbackNoise3D.octaveNoise(ctx.x(), ctx.y() * 0.5, ctx.z(), 4, 0.5, 0.005);
        
        if (weirdness > 0.5 && factor > 1.0) {
            double jagged = (1.0 - Math.abs(fallbackNoise3D.octaveNoise(ctx.x(), ctx.z(), 3, 0.5, 0.01))) * 30.0;
            density += jagged * ((weirdness - 0.5) * 2.0);
        }

        double caves = fallbackOverhangNoise.octaveNoise(ctx.x(), ctx.y(), ctx.z(), 3, 0.5, 0.02);
        density += (terrainShape * factor * 45.0);
        
        if (caves > 0.6) {
            density -= (caves - 0.6) * 40.0;
        }

        double micro = fallbackDetailNoise.noise(ctx.x() * 0.05, ctx.y() * 0.05, ctx.z() * 0.05) * 3.0;
        density += micro;

        return density;
    }
}
