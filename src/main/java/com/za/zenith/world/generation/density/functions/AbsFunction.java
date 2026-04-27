package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class AbsFunction implements DensityFunction {
    private final DensityFunction argument;

    public AbsFunction(DensityFunction argument) {
        this.argument = argument;
    }

    @Override
    public double compute(DensityContext ctx) {
        return Math.abs(argument.compute(ctx));
    }
}
