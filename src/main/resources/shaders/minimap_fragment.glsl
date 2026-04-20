#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform float uTime;
uniform float uPulse;
uniform float uNoiseLevel;
uniform float uTexelSize;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    vec2 uv = fragTexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(uv, center);
    
    // Circular Masking (SDF)
    float mask = smoothstep(0.5, 0.48, dist);
    if (mask < 0.01) discard;
    
    // 1. Sample texture and heights (A channel contains height y/256)
    vec4 texSample = texture(textureSampler, uv);
    float h = texSample.a;
    
    // 2. Toon Outlines (based on height difference)
    float hL = texture(textureSampler, uv + vec2(-uTexelSize, 0.0)).a;
    float hR = texture(textureSampler, uv + vec2( uTexelSize, 0.0)).a;
    float hU = texture(textureSampler, uv + vec2(0.0,  uTexelSize)).a;
    float hD = texture(textureSampler, uv + vec2(0.0, -uTexelSize)).a;
    
    // Sobel-like edge detection for height
    float edge = abs(h - hL) + abs(h - hR) + abs(h - hU) + abs(h - hD);
    float outline = smoothstep(0.005, 0.015, edge); // Threshold for 1-block step
    
    // 3. Slope Cel-Shading
    // Reconstruct normal from height map
    // We use a fixed vertical scale to make slopes visible
    vec3 normal = normalize(vec3((hL - hR) * 10.0, 1.0, (hD - hU) * 10.0));
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.2)); // Directional light from "North-East"
    float diff = dot(normal, lightDir);
    
    // Quantize light to 3 levels (Cel-shading)
    float toon = floor(diff * 3.0 + 0.5) / 3.0;
    toon = clamp(toon, 0.7, 1.1);
    
    vec3 baseColor = texSample.rgb * toon;
    
    // Apply dark outline to edges
    baseColor = mix(baseColor, vec3(0.0, 0.0, 0.0), outline * 0.4);
    
    // 4. Noise Sonar Effect
    if (uNoiseLevel > 0.01) {
        float noiseRadius = uNoiseLevel * 0.45;
        float ringWidth = 0.015;
        
        float wave = fract(uTime * 1.5);
        float currentRadius = noiseRadius * wave;
        float ring = smoothstep(currentRadius + ringWidth, currentRadius, dist) * 
                     smoothstep(currentRadius - ringWidth, currentRadius, dist);
        
        float boundary = smoothstep(noiseRadius + 0.005, noiseRadius, dist) * 
                         smoothstep(noiseRadius - 0.005, noiseRadius, dist);
        
        vec3 sonarColor = vec3(1.0, 1.0, 1.0);
        baseColor = mix(baseColor, sonarColor, ring * 0.4 * (1.0 - wave));
        baseColor = mix(baseColor, sonarColor, boundary * 0.25);
    }

    // Visual Polish: Scanlines and Vignette
    float scanline = sin(uv.y * 500.0 + uTime * 3.0) * 0.02 + 0.98;
    baseColor *= scanline;
    
    float vignette = smoothstep(0.5, 0.3, dist);
    baseColor *= (0.75 + 0.25 * vignette);
    
    fragColor = vec4(baseColor * tintColor.rgb, mask * tintColor.a);
}
