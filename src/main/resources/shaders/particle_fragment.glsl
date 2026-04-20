#version 330 core

in vec3 fragTexCoord;
in float fragAlpha;
in vec3 vColor;
in vec3 vNormal;
in float overlayLayer;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform vec3 ambientLight;
uniform vec3 uGrassColor = vec3(0.486, 0.784, 0.314);

#include "include/lighting.glsl"

uniform ZenithLight uLights[8];
uniform int uLightCount;

void main() {
    vec4 texColor = texture(textureSampler, fragTexCoord);
    if (texColor.a < 0.1) discard;

    vec3 baseColor = texColor.rgb;
    
    // Check if we passed an overlay layer
    if (overlayLayer >= 0.0) {
        vec4 overlayTex = texture(textureSampler, vec3(fragTexCoord.xy, overlayLayer));
        if (overlayTex.a > 0.1) {
            // Tint the overlay with vColor and blend it over the base color
            baseColor = mix(baseColor, overlayTex.rgb * vColor, overlayTex.a);
        }
    } else {
        // If no overlay, just tint the base texture directly
        baseColor *= vColor;
    }

    // --- Maximum Performance Lighting ---
    // Remove all dot products and light loops for shards.
    // Just use boosted ambient for a clean, consistent look.
    vec3 lighting = ambientLight * 1.6 + 0.1;

    fragColor = vec4(baseColor * lighting, texColor.a * fragAlpha);
}
