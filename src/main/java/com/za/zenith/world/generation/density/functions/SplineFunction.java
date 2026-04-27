package com.za.zenith.world.generation.density.functions;

import com.za.zenith.world.generation.density.DensityFunction;

import com.za.zenith.world.generation.density.DensityContext;
import com.za.zenith.world.generation.density.DensityFunction;
import com.za.zenith.utils.SplineInterpolator;

import java.util.List;

public class SplineFunction implements DensityFunction {
    public enum Coordinate {
        CONTINENTALNESS, EROSION, WEIRDNESS, TEMPERATURE, HUMIDITY, Y
    }

    private final Coordinate coordinate;
    private final List<float[]> points; // Pairs of [input, output]

    public SplineFunction(Coordinate coordinate, List<float[]> points) {
        this.coordinate = coordinate;
        this.points = points;
    }

    @Override
    public double compute(DensityContext ctx) {
        float input = switch (coordinate) {
            case CONTINENTALNESS -> ctx.continentalness();
            case EROSION -> ctx.erosion();
            case WEIRDNESS -> ctx.weirdness();
            case TEMPERATURE -> ctx.temperature();
            case HUMIDITY -> ctx.humidity();
            case Y -> (float) ctx.y();
        };

        return SplineInterpolator.interpolate(points, input);
    }
}
