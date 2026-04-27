package com.za.zenith.world.generation.density;

/**
 * Реализация контекста плотности.
 */
public record DensityContextImpl(
    double x, 
    double y, 
    double z,
    float continentalness,
    float erosion,
    float weirdness,
    float temperature,
    float humidity
) implements DensityContext {}
