package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class ConstantFunction implements DensityFunction {
    private final double value;

    public ConstantFunction(double value) {
        this.value = value;
    }

    @Override
    public double compute(DensityContext ctx) {
        return value;
    }
}
