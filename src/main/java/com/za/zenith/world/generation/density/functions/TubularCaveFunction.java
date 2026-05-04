package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class TubularCaveFunction implements DensityFunction {
    private final DensityFunction thickness;
    private final DensityFunction noise1;
    private final DensityFunction noise2;

    public TubularCaveFunction(DensityFunction thickness, DensityFunction noise1, DensityFunction noise2) {
        this.thickness = thickness;
        this.noise1 = noise1;
        this.noise2 = noise2;
    }

    @Override
    public double compute(DensityContext ctx) {
        double t = thickness.compute(ctx);
        // Minimum viable tunnel thickness to prevent closed-off micro-caves
        // Inlined logic: max(t, 0.1) if we want it guaranteed, but let's trust the JSON first.
        if (t <= 0) return -1.0; 

        double n1 = noise1.compute(ctx);
        double n2 = noise2.compute(ctx);
        
        // Tubular math: Thickness - sum of absolute noise values
        // If (abs(n1) + abs(n2)) < t, we are INSIDE the cave (positive density)
        // We multiply by a large factor in JSON to carve effectively.
        return t - (Math.abs(n1) + Math.abs(n2));
    }
}