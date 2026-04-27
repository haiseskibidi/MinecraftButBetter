package com.za.zenith.world.generation.density;

import com.za.zenith.utils.SplineInterpolator;
import com.za.zenith.world.generation.SimplexNoise;
import java.util.List;

/**
 * Библиотека стандартных узлов для графа плотности.
 */
public class DensityFunctions {

    public static DensityFunction constant(double value) {
        return ctx -> value;
    }

    public static DensityFunction noise(SimplexNoise noise, double scale, double amp) {
        return ctx -> noise.noise(ctx.x() * scale, ctx.y() * scale, ctx.z() * scale) * amp;
    }

    public static DensityFunction octaveNoise(SimplexNoise noise, int octaves, double persistence, double scale, double amp) {
        return ctx -> noise.octaveNoise(ctx.x(), ctx.y(), ctx.z(), octaves, persistence, scale) * amp;
    }

    public static DensityFunction add(DensityFunction a, DensityFunction b) {
        return ctx -> a.compute(ctx) + b.compute(ctx);
    }

    public static DensityFunction mul(DensityFunction a, DensityFunction b) {
        return ctx -> a.compute(ctx) * b.compute(ctx);
    }

    public static DensityFunction max(DensityFunction a, DensityFunction b) {
        return ctx -> Math.max(a.compute(ctx), b.compute(ctx));
    }

    public static DensityFunction min(DensityFunction a, DensityFunction b) {
        return ctx -> Math.min(a.compute(ctx), b.compute(ctx));
    }

    public static DensityFunction warp(DensityFunction input, DensityFunction warpX, DensityFunction warpY, DensityFunction warpZ) {
        return ctx -> {
            double nx = ctx.x() + warpX.compute(ctx);
            double ny = ctx.y() + warpY.compute(ctx);
            double nz = ctx.z() + warpZ.compute(ctx);
            DensityContext warpedCtx = new DensityContextImpl(
                nx, ny, nz, 
                ctx.continentalness(), ctx.erosion(), ctx.weirdness(), 
                ctx.temperature(), ctx.humidity()
            );
            return input.compute(warpedCtx);
        };
    }

    public static DensityFunction spline(DensityFunction input, List<float[]> points) {
        return ctx -> SplineInterpolator.interpolate(points, (float) input.compute(ctx));
    }

    public static DensityFunction rangeChoice(DensityFunction input, double min, double max, DensityFunction whenInRange, DensityFunction whenOutOfRange) {
        return ctx -> {
            double val = input.compute(ctx);
            return (val >= min && val <= max) ? whenInRange.compute(ctx) : whenOutOfRange.compute(ctx);
        };
    }
    
    public static DensityFunction yGradient(double base, double scale) {
        return ctx -> (base - ctx.y()) / scale;
    }

    /**
     * Создает ступенчатый эффект (террасирование).
     */
    public static DensityFunction terrace(DensityFunction input, double size) {
        return ctx -> {
            double val = input.compute(ctx);
            return Math.floor(val / size) * size;
        };
    }

    /**
     * Динамическое изменение масштаба шума (Shift Noise).
     * Помогает избежать повторяющихся паттернов.
     */
    public static DensityFunction shift(DensityFunction input, DensityFunction shift) {
        return ctx -> {
            double s = shift.compute(ctx);
            DensityContext shiftedCtx = new DensityContextImpl(
                ctx.x() * (1.0 + s), ctx.y(), ctx.z() * (1.0 + s),
                ctx.continentalness(), ctx.erosion(), ctx.weirdness(),
                ctx.temperature(), ctx.humidity()
            );
            return input.compute(shiftedCtx);
        };
    }

    /**
     * Гауссово ядро (для создания цельных, округлых объектов).
     */
    public static DensityFunction gaussian(double centerX, double centerY, double centerZ, double radius, double amp) {
        return ctx -> {
            double dx = ctx.x() - centerX;
            double dy = ctx.y() - centerY;
            double dz = ctx.z() - centerZ;
            double distSq = dx*dx + dy*dy + dz*dz;
            return Math.exp(-distSq / (radius * radius)) * amp;
        };
    }
}
