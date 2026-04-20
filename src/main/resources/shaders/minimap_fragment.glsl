#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform float uTime;
uniform float uPulse;
uniform float uNoiseLevel;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    vec2 uv = fragTexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(uv, center);

    // Circular Masking (SDF)
    float mask = smoothstep(0.5, 0.49, dist);
    if (mask < 0.01) discard;

    vec4 texColor = texture(textureSampler, uv);

    // Noise Sonar Effect
    if (uNoiseLevel > 0.01) {
        float noiseRadius = uNoiseLevel * 0.45; // Max radius slightly inside the map
        float ringWidth = 0.015;

        // Dynamic expanding ring
        float wave = fract(uTime * 1.5);
        float currentRadius = noiseRadius * wave;
        float ring = smoothstep(currentRadius + ringWidth, currentRadius, dist) * 
                     smoothstep(currentRadius - ringWidth, currentRadius, dist);

        // Detection boundary ring (static based on noise)
        float boundary = smoothstep(noiseRadius + 0.005, noiseRadius, dist) * 
                         smoothstep(noiseRadius - 0.005, noiseRadius, dist);

        vec3 sonarColor = vec4(1.0, 1.0, 1.0, 1.0).rgb;
        texColor.rgb = mix(texColor.rgb, sonarColor, ring * 0.5 * (1.0 - wave));
        texColor.rgb = mix(texColor.rgb, sonarColor, boundary * 0.3);
    }

    // Add subtle scanlines
    float scanline = sin(uv.y * 400.0 + uTime * 5.0) * 0.03 + 0.97;
    texColor.rgb *= scanline;
    
    // Add radial darkening at the edges
    float vignette = smoothstep(0.5, 0.35, dist);
    texColor.rgb *= (0.7 + 0.3 * vignette);
    
    // Apply tint and pulse effect
    fragColor = texColor * tintColor;
    fragColor.rgb += uPulse * 0.1;
    fragColor.a *= mask;
}
