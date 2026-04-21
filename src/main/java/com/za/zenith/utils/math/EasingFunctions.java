package com.za.zenith.utils.math;

/**
 * Standard easing functions for built-in types.
 */
public class EasingFunctions {
    public static float linear(float t) { return t; }
    public static float sine(float t) { return (float) Math.sin(t * Math.PI / 2.0); }
    public static float sine_pi(float t) { return (float) Math.sin(t * Math.PI); }
    public static float smoothstep(float t) { return t * t * (3 - 2 * t); }
    public static float smootherstep(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    public static float quad_in(float t) { return t * t; }
    public static float quad_out(float t) { return 1.0f - (1.0f - t) * (1.0f - t); }
    public static float quad_in_out(float t) { return t < 0.5f ? 2.0f * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 2.0f) / 2.0f; }
    public static float cubic_in(float t) { return t * t * t; }
    public static float cubic_out(float t) { return 1.0f - (float) Math.pow(1.0f - t, 3.0f); }
    public static float cubic_in_out(float t) { return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f; }
    public static float back_in(float t) { return 2.70158f * t * t * t - 1.70158f * t * t; }
    public static float back_out(float t) { return 1.0f + 2.70158f * (float) Math.pow(t - 1.0f, 3.0f) + 1.70158f * (float) Math.pow(t - 1.0f, 2.0f); }
    public static float elastic_out(float t) { return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI) / 3) + 1); }
    public static float bounce_out(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (t < 1 / d1) return n1 * t * t;
        else if (t < 2 / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
        else if (t < 2.5 / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        else return n1 * (t -= 2.625f / d1) * t + 0.984375f;
    }

    public static float evaluateBuiltin(String name, float t) {
        return switch (name.toLowerCase()) {
            case "linear" -> linear(t);
            case "sine" -> sine(t);
            case "sine_pi" -> sine_pi(t);
            case "smoothstep" -> smoothstep(t);
            case "smootherstep" -> smootherstep(t);
            case "quad_in" -> quad_in(t);
            case "quad_out" -> quad_out(t);
            case "quad_in_out" -> quad_in_out(t);
            case "cubic_in" -> cubic_in(t);
            case "cubic_out" -> cubic_out(t);
            case "cubic_in_out" -> cubic_in_out(t);
            case "back_in" -> back_in(t);
            case "back_out" -> back_out(t);
            case "elastic_out" -> elastic_out(t);
            case "bounce_out" -> bounce_out(t);
            default -> t;
        };
    }
}
