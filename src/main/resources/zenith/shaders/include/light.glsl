#version 150

#define zenith_LIGHT_POWER   (0.6)
#define zenith_AMBIENT_LIGHT (0.4)

vec4 zenith_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    lightDir0 = normalize(lightDir0);
    lightDir1 = normalize(lightDir1);
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, (light0 + light1) * zenith_LIGHT_POWER + zenith_AMBIENT_LIGHT);
    return vec4(color.rgb * lightAccum, color.a);
}

vec4 zenith_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
