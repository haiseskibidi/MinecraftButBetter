package com.za.zenith.world.generation.density;

/**
 * Контекст плотности. Возвращен к record для стабильности JIT-компилятора.
 */
public record DensityContextImpl(double x, double y, double z, float continentalness, float erosion, float weirdness, float temperature, float humidity) implements DensityContext {
}
