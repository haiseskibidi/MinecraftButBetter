package com.za.zenith.world.generation.density;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.generation.density.functions.*;
import com.za.zenith.world.generation.density.DensityFunction;

import java.util.ArrayList;
import java.util.List;

public class DensityFunctionParser {
    private final long seed;

    public DensityFunctionParser(long seed) {
        this.seed = seed;
    }

    public DensityFunction parse(JsonElement el) {
        if (el.isJsonPrimitive()) {
            return new ConstantFunction(el.getAsDouble());
        }

        if (el.isJsonArray()) {
            throw new IllegalArgumentException("Cannot parse JSON array as density function");
        }

        JsonObject obj = el.getAsJsonObject();
        String type = obj.get("type").getAsString();

        return switch (type) {
            case "zenith:add" -> new MathFunction(MathFunction.Type.ADD, parse(obj.get("argument1")), parse(obj.get("argument2")));
            case "zenith:mul" -> new MathFunction(MathFunction.Type.MUL, parse(obj.get("argument1")), parse(obj.get("argument2")));
            case "zenith:max" -> new MathFunction(MathFunction.Type.MAX, parse(obj.get("argument1")), parse(obj.get("argument2")));
            case "zenith:min" -> new MathFunction(MathFunction.Type.MIN, parse(obj.get("argument1")), parse(obj.get("argument2")));
            case "zenith:abs" -> new AbsFunction(parse(obj.get("argument")));
            case "zenith:square" -> new SquareFunction(parse(obj.get("argument")));
            case "zenith:clamp" -> new ClampFunction(parse(obj.get("argument")), obj.get("min").getAsDouble(), obj.get("max").getAsDouble());
            case "zenith:terrace" -> new TerraceFunction(parse(obj.get("argument")), obj.get("step_size").getAsDouble(), obj.has("smooth_scale") ? obj.get("smooth_scale").getAsDouble() : 1.0);
            case "zenith:cache2d" -> new Cache2DFunction(parse(obj.get("argument")));
            case "zenith:y_clamped_gradient" -> new YGradientFunction(obj.get("bottom_y").getAsDouble(), obj.get("top_y").getAsDouble(), obj.get("bottom_value").getAsDouble(), obj.get("top_value").getAsDouble());
            case "zenith:spline" -> {
                SplineFunction.Coordinate coord = SplineFunction.Coordinate.valueOf(obj.get("coordinate").getAsString().replace("zenith:", "").toUpperCase());
                List<float[]> points = new ArrayList<>();
                JsonArray ptsArr = obj.getAsJsonArray("points");
                for (JsonElement pEl : ptsArr) {
                    JsonArray pArr = pEl.getAsJsonArray();
                    points.add(new float[]{pArr.get(0).getAsFloat(), pArr.get(1).getAsFloat()});
                }
                yield new SplineFunction(coord, points);
            }
            case "zenith:noise" -> {
                // Determine a unique seed offset for this noise
                long noiseSeed = seed + obj.get("noise").getAsString().hashCode();
                double xzScale = obj.has("xz_scale") ? obj.get("xz_scale").getAsDouble() : 1.0;
                double yScale = obj.has("y_scale") ? obj.get("y_scale").getAsDouble() : 0.0;
                int octaves = obj.has("octaves") ? obj.get("octaves").getAsInt() : 1;
                double persistence = obj.has("persistence") ? obj.get("persistence").getAsDouble() : 0.5;
                double lacunarity = obj.has("lacunarity") ? obj.get("lacunarity").getAsDouble() : 2.0;
                double amplitude = obj.has("amplitude") ? obj.get("amplitude").getAsDouble() : 1.0;
                yield new NoiseFunction(noiseSeed, xzScale, yScale, octaves, persistence, lacunarity, amplitude);
            }
            default -> throw new IllegalArgumentException("Unknown density function type: " + type);
        };
    }
}
