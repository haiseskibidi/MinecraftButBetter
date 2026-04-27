package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class TerraceFunction implements DensityFunction {
    private final DensityFunction argument;
    private final double stepSize;
    private final double smoothScale;

    public TerraceFunction(DensityFunction argument, double stepSize, double smoothScale) {
        this.argument = argument;
        this.stepSize = stepSize;
        this.smoothScale = smoothScale;
    }

    @Override
    public double compute(DensityContext ctx) {
        double v = argument.compute(ctx);
        
        // Математика террасирования (превращаем плавный градиент в лестницу со сглаженными краями)
        double stepped = Math.floor(v / stepSize) * stepSize;
        double fraction = (v - stepped) / stepSize;
        
        // Сглаживаем ступеньку
        double t = Math.pow(fraction, smoothScale); 
        
        return stepped + t * stepSize;
    }
}
