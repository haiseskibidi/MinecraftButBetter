package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class YGradientFunction implements DensityFunction {
    private final double topY;
    private final double bottomY;
    private final double topValue;
    private final double bottomValue;

    public YGradientFunction(double bottomY, double topY, double bottomValue, double topValue) {
        this.bottomY = bottomY;
        this.topY = topY;
        this.bottomValue = bottomValue;
        this.topValue = topValue;
    }

    @Override
    public double compute(DensityContext ctx) {
        double y = ctx.y();
        if (y <= bottomY) return bottomValue;
        if (y >= topY) return topValue;
        
        double t = (y - bottomY) / (topY - bottomY);
        return bottomValue + t * (topValue - bottomValue);
    }
}
