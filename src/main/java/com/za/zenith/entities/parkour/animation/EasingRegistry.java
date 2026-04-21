package com.za.zenith.entities.parkour.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.utils.Logger;
import com.za.zenith.utils.math.EasingFunctions;
import com.za.zenith.world.DataLoader;

import java.util.*;

/**
 * Data-Driven Easing Registry.
 * Bakes JSON formulas and Beziers into LUTs for O(1) performance.
 */
public class EasingRegistry {
    private static final int LUT_SIZE = 256;
    private static final Map<String, float[]> registry = new HashMap<>();

    public static void init() {
        reload();
    }

    public static void reload() {
        registry.clear();
        JsonObject root = DataLoader.loadJson("zenith/registry/easings.json");
        if (root == null || root.size() == 0) {
            Logger.warn("EasingRegistry: No easings found in JSON, using defaults");
            bakeDefaultEasings();
            return;
        }

        for (String name : root.keySet()) {
            try {
                JsonObject config = root.getAsJsonObject(name);
                String type = config.get("type").getAsString();
                float[] lut = new float[LUT_SIZE];

                switch (type.toLowerCase()) {
                    case "builtin" -> bakeBuiltin(lut, name);
                    case "expression" -> bakeExpression(lut, config.get("formula").getAsString());
                    case "bezier" -> bakeBezier(lut, config.getAsJsonArray("points"));
                }
                registry.put(name, lut);
            } catch (Exception e) {
                Logger.error("Failed to bake easing '%s': %s", name, e.getMessage());
            }
        }
        Logger.info("EasingRegistry: Baked %d easings", registry.size());
    }

    private static void bakeBuiltin(float[] lut, String name) {
        for (int i = 0; i < LUT_SIZE; i++) {
            float t = i / (float)(LUT_SIZE - 1);
            lut[i] = EasingFunctions.evaluateBuiltin(name, t);
        }
    }

    private static void bakeBezier(float[] lut, JsonArray points) {
        float x1 = points.get(0).getAsFloat();
        float y1 = points.get(1).getAsFloat();
        float x2 = points.get(2).getAsFloat();
        float y2 = points.get(3).getAsFloat();

        for (int i = 0; i < LUT_SIZE; i++) {
            float targetX = i / (float)(LUT_SIZE - 1);
            // Solve for t that gives targetX using binary search (simple enough for baking)
            float t = findBezierT(targetX, x1, x2);
            lut[i] = sampleBezierY(t, y1, y2);
        }
    }

    private static float findBezierT(float x, float x1, float x2) {
        float low = 0, high = 1;
        for (int i = 0; i < 14; i++) {
            float mid = (low + high) / 2;
            float midX = 3 * (1-mid)*(1-mid)*mid*x1 + 3*(1-mid)*mid*mid*x2 + mid*mid*mid;
            if (midX < x) low = mid; else high = mid;
        }
        return (low + high) / 2;
    }

    private static float sampleBezierY(float t, float y1, float y2) {
        return 3 * (1-t)*(1-t)*t*y1 + 3*(1-t)*t*t*y2 + t*t*t;
    }

    private static void bakeExpression(float[] lut, String formula) {
        // Simple manual parsing for 't', basic ops, and common functions
        // Using a tiny recursive descent for baking
        for (int i = 0; i < LUT_SIZE; i++) {
            float t = i / (float)(LUT_SIZE - 1);
            lut[i] = new ExpressionEvaluator(formula, t).evaluate();
        }
    }

    private static void bakeDefaultEasings() {
        List<String> defaults = List.of("linear", "sine", "quad_out", "smootherstep");
        for (String d : defaults) {
            float[] lut = new float[LUT_SIZE];
            bakeBuiltin(lut, d);
            registry.put(d, lut);
        }
    }

    public static float evaluate(String name, float t) {
        float[] lut = registry.get(name);
        if (lut == null) return t; // Fallback to linear

        t = Math.clamp(t, 0, 1);
        float indexF = t * (LUT_SIZE - 1);
        int index = (int) indexF;
        if (index >= LUT_SIZE - 1) return lut[LUT_SIZE - 1];

        float fraction = indexF - index;
        return lut[index] + (lut[index + 1] - lut[index]) * fraction; // Linear interpolation in LUT
    }

    public static Set<String> getKeys() {
        return registry.keySet();
    }

    public static List<String> getEasings() {
        return new ArrayList<>(registry.keySet());
    }

    /**
     * Tiny Expression Evaluator for baking formulas.
     */
    private static class ExpressionEvaluator {
        private final String formula;
        private final float tValue;
        private int pos = -1, ch;

        ExpressionEvaluator(String formula, float t) {
            this.formula = formula;
            this.tValue = t;
        }

        void nextChar() { ch = (++pos < formula.length()) ? formula.charAt(pos) : -1; }
        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) { nextChar(); return true; }
            return false;
        }

        float evaluate() {
            nextChar();
            return parseExpression();
        }

        float parseExpression() {
            float x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        float parseTerm() {
            float x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }

        float parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            float x;
            int startPos = this.pos;
            if (eat('(')) { x = parseExpression(); eat(')'); }
            else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Float.parseFloat(formula.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z' || ch == '_') {
                while (ch >= 'a' && ch <= 'z' || ch == '_') nextChar();
                String name = formula.substring(startPos, this.pos);
                if (name.equals("t")) x = tValue;
                else if (name.equals("pi")) x = (float) Math.PI;
                else {
                    x = parseFactor();
                    if (name.equals("sin")) x = (float) Math.sin(x);
                    else if (name.equals("cos")) x = (float) Math.cos(x);
                    else if (name.equals("sqrt")) x = (float) Math.sqrt(x);
                    else if (name.equals("abs")) x = Math.abs(x);
                    else throw new RuntimeException("Unknown function: " + name);
                }
            } else throw new RuntimeException("Unexpected: " + (char)ch);
            if (eat('^')) x = (float) Math.pow(x, parseFactor());
            return x;
        }
    }
}
