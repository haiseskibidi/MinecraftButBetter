#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform float uTime;
uniform float uPulse;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    vec2 uv = fragTexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(uv, center);
    
    // Circular Masking (SDF)
    float mask = smoothstep(0.5, 0.49, dist);
    if (mask < 0.01) discard;
    
    vec4 texColor = texture(textureSampler, uv);
    
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
