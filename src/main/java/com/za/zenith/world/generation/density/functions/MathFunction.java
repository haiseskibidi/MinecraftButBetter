package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class MathFunction implements DensityFunction {
    public enum Type { ADD, MUL, MAX, MIN }

    private final Type type;
    private final DensityFunction arg1;
    private final DensityFunction arg2;

    public MathFunction(Type type, DensityFunction arg1, DensityFunction arg2) {
        this.type = type;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public double compute(DensityContext ctx) {
        double v1 = arg1.compute(ctx);
        double v2 = arg2.compute(ctx);
        return switch (type) {
            case ADD -> v1 + v2;
            case MUL -> v1 * v2;
            case MAX -> Math.max(v1, v2);
            case MIN -> Math.min(v1, v2);
        };
    }
}
