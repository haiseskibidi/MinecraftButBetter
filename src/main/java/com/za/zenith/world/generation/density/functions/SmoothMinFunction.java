package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class SmoothMinFunction implements DensityFunction {
    private final DensityFunction arg1;
    private final DensityFunction arg2;
    private final double k;

    public SmoothMinFunction(DensityFunction arg1, DensityFunction arg2, double k) {
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.k = Math.max(0.0001, k); // Prevent division by zero
    }

    @Override
    public double compute(DensityContext ctx) {
        double a = arg1.compute(ctx);
        double b = arg2.compute(ctx);
        double h = Math.max(0.0, Math.min(1.0, 0.5 + 0.5 * (b - a) / k));
        return a * h + b * (1.0 - h) - k * h * (1.0 - h);
    }
}