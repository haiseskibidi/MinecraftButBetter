package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.SimplexNoise;
import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;

public class NoiseFunction implements DensityFunction {
    private final SimplexNoise noise;
    private final double xzScale;
    private final double yScale;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    private final double amplitude;

    public NoiseFunction(long seed, double xzScale, double yScale, int octaves, double persistence, double lacunarity, double amplitude) {
        this.noise = new SimplexNoise(seed);
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.octaves = octaves;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
        this.amplitude = amplitude;
    }

    @Override
    public double compute(DensityContext ctx) {
        if (octaves <= 1) {
            if (yScale == 0.0) {
                return noise.noise(ctx.x() * xzScale, ctx.z() * xzScale) * amplitude;
            }
            return noise.noise(ctx.x() * xzScale, ctx.y() * yScale, ctx.z() * xzScale) * amplitude;
        } else {
            if (yScale == 0.0) {
                return noise.octaveNoise(ctx.x() * xzScale, ctx.z() * xzScale, 0, octaves, persistence, 1.0, lacunarity) * amplitude;
            }
            return noise.octaveNoise(ctx.x() * xzScale, ctx.y() * yScale, ctx.z() * xzScale, octaves, persistence, 1.0, lacunarity) * amplitude;
        }
    }
}
