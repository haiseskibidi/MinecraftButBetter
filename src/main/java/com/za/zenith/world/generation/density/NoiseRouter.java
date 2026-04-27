package com.za.zenith.world.generation.density;

import com.google.gson.JsonElement;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.generation.BiomeGenerator;
import com.za.zenith.world.generation.density.DensityFunction;

public class NoiseRouter {
    private final BiomeGenerator biomeGenerator;
    private final DensityFunction finalDensityFunction;

    public NoiseRouter(long seed, BiomeGenerator biomeGenerator) {
        this.biomeGenerator = biomeGenerator;
        
        JsonElement rootJson = DensityFunctionRegistry.get(Identifier.of("zenith:final_density"));
        if (rootJson != null) {
            DensityFunctionParser parser = new DensityFunctionParser(seed);
            this.finalDensityFunction = parser.parse(rootJson);
        } else {
            throw new RuntimeException("CRITICAL: zenith:final_density function not found in registry! World generation cannot continue.");
        }
    }

    public double getDensity(DensityContext ctx) {
        return finalDensityFunction.compute(ctx);
    }
}
