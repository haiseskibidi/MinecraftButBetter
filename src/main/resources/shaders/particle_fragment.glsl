#version 330 core

in vec3 fragTexCoord;
in float fragAlpha;
in vec3 vColor;
in vec3 vNormal;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);
    if (texColor.a < 0.1) discard;

    // Smart Multi-Material Tinting: 
    // Just multiply the texture by the per-instance color.
    // If the color is (1,1,1), nothing changes. 
    // If it's biome green, the texture gets tinted.
    vec3 baseColor = texColor.rgb * vColor;

    // Standard Lighting
    float diff = max(dot(normalize(vNormal), -lightDirection), 0.0);
    vec3 lighting = ambientLight + lightColor * diff;

    fragColor = vec4(baseColor * lighting, texColor.a * fragAlpha);
}
