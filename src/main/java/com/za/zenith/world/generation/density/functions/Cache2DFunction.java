package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

/**
 * Узел кэширования 2D-значений. 
 * Если X и Z не изменились, возвращает предыдущий результат.
 * Идеально подходит для Continentalness, Erosion и других параметров климата.
 */
public class Cache2DFunction implements DensityFunction {
    private final DensityFunction wrapped;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private double lastResult;

    public Cache2DFunction(DensityFunction wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double compute(DensityContext ctx) {
        double x = ctx.x();
        double z = ctx.z();
        
        if (x == lastX && z == lastZ) {
            return lastResult;
        }
        
        lastX = x;
        lastZ = z;
        lastResult = wrapped.compute(ctx);
        return lastResult;
    }
}
