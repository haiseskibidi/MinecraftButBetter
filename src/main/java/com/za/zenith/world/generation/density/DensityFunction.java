package com.za.zenith.world.generation.density;

/**
 * Базовый интерфейс для всех узлов графа плотности.
 */
@FunctionalInterface
public interface DensityFunction {
    double compute(DensityContext context);

    default DensityFunction add(DensityFunction other) {
        return ctx -> this.compute(ctx) + other.compute(ctx);
    }

    default DensityFunction mul(DensityFunction other) {
        return ctx -> this.compute(ctx) * other.compute(ctx);
    }
    
    default DensityFunction clamp(double min, double max) {
        return ctx -> Math.clamp(this.compute(ctx), min, max);
    }
}
