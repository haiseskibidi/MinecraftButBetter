#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D screenTexture;
uniform sampler2D depthTexture;
uniform vec2 screenSize;

float near = 0.01; 
float far  = 1000.0; 

float LinearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0; 
    return (2.0 * near * far) / (far + near - z * (far - near));    
}

float getDepth(vec2 uv) {
    return LinearizeDepth(texture(depthTexture, uv).r);
}

// Reconstruct world-space normal from depth derivatives
vec3 getNormal(vec2 uv, float centerDepth) {
    vec2 texelSize = 1.0 / screenSize;
    
    float d1 = getDepth(uv + vec2(texelSize.x, 0.0));
    float d2 = getDepth(uv + vec2(0.0, texelSize.y));
    
    vec3 v1 = vec3(texelSize.x, 0.0, d1 - centerDepth);
    vec3 v2 = vec3(0.0, texelSize.y, d2 - centerDepth);
    
    return normalize(cross(v1, v2));
}

void main() {
    vec4 texSample = texture(screenTexture, fragTexCoord);
    vec3 color = texSample.rgb;
    
    float rawDepth = texture(depthTexture, fragTexCoord).r;
    float d = LinearizeDepth(rawDepth);
    
    if (rawDepth < 0.99999) {
        vec2 texelSize = 1.0 / screenSize;
        
        // 1. Stylized Crease AO (Ambient Occlusion Lite)
        // We look for pixels that are significantly deeper than their surroundings
        float dL = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(-texelSize.x, 0.0)).r);
        float dR = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2( texelSize.x, 0.0)).r);
        float dU = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(0.0,  texelSize.y)).r);
        float dD = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(0.0, -texelSize.y)).r);
        
        float averageDepth = (dL + dR + dU + dD) * 0.25;
        float diff = d - averageDepth;
        
        // If center is deeper than average, it's a corner/crease
        if (diff > 0.05) {
            float ao = smoothstep(0.05, 0.5, diff);
            color *= mix(1.0, 0.6, ao); // Mellow darkening in corners
        }
        
        // 2. Atmospheric Fog
        vec3 skyColor = vec3(0.6, 0.8, 1.0); 
        float fogFactor = smoothstep(50.0, 250.0, d);
        color = mix(color, skyColor, fogFactor);
    }
    
    // 3. Vibrance (Intelligent Saturation)
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    float maxColor = max(color.r, max(color.g, color.b));
    float minColor = min(color.r, min(color.g, color.b));
    float sat = maxColor - minColor;
    color = mix(vec3(luma), color, 1.0 + (0.4 * (1.0 - sat))); 

    // 4. Vignette
    vec2 uv = fragTexCoord * (1.0 - fragTexCoord.yx);
    float vig = uv.x * uv.y * 15.0; 
    vig = pow(vig, 0.15); 
    color *= vig;

    fragColor = vec4(color, 1.0);
}
