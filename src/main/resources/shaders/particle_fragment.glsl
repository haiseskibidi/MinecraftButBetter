#version 330 core

in vec3 fragTexCoord;
in vec3 fragNormal;
in vec3 vLocalNormal;
in vec4 vGridInfo; // gx, gy, gz, gridSize
in vec2 vTexData; // layerIndex, randomSeed
in float vIsTinted;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;

// Simple hash for randomizing UVs
float hash(float n) { return fract(sin(n) * 43758.5453123); }

void main() {
    // QUALITY FIX: No more downscaling UVs. 
    // Each shard face shows a different part of the material texture.
    float seed = vTexData.y;
    vec2 randomOffset = vec2(hash(seed), hash(seed + 0.5));
    
    // Maintain 1:1 pixel density by using fract on original UVs + offset
    vec2 finalUV = fract(fragTexCoord.xy + randomOffset);
    
    float layer = vTexData.x;
    vec4 texColor = texture(textureSampler, vec3(finalUV, layer));
    if (texColor.a < 0.1) discard; // Lower threshold for sharper shards

    // Apply Biome Tint (for leaves/grass)
    vec3 baseColor = texColor.rgb;
    if (vIsTinted > 0.5) {
        baseColor *= vec3(0.486, 0.784, 0.314); 
    }

    // Standard Diffuse Lighting
    // For procedural shards, we use the normal passed from vertex shader.
    // Since culling is off, we use absolute dot product for two-sided lighting.
    vec3 normal = normalize(fragNormal);
    float diff = abs(dot(normal, -lightDirection));
    vec3 lighting = ambientLight + lightColor * diff;

    fragColor = vec4(baseColor * lighting, texColor.a);
}
