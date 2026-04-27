package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class SquareFunction implements DensityFunction {
    private final DensityFunction argument;

    public SquareFunction(DensityFunction argument) {
        this.argument = argument;
    }

    @Override
    public double compute(DensityContext ctx) {
        double v = argument.compute(ctx);
        return v * v;
    }
}
