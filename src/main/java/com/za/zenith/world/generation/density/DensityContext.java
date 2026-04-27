package com.za.zenith.world.generation.density;

/**
 * Контекст для вычисления плотности. Хранит координаты и кэшированные параметры климата.
 */
public interface DensityContext {
    double x();
    double y();
    double z();
    
    // Параметры климата
    float continentalness();
    float erosion();
    float weirdness();
    float temperature();
    float humidity();
}
