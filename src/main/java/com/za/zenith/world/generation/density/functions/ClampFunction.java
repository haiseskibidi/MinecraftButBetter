package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class ClampFunction implements DensityFunction {
    private final DensityFunction argument;
    private final double min;
    private final double max;

    public ClampFunction(DensityFunction argument, double min, double max) {
        this.argument = argument;
        this.min = min;
        this.max = max;
    }

    @Override
    public double compute(DensityContext ctx) {
        double v = argument.compute(ctx);
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
