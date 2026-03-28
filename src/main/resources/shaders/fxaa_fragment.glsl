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

vec3 getNormal(vec2 uv, float centerDepth) {
    vec2 texelSize = 1.0 / screenSize;
    float d1 = getDepth(uv + vec2(texelSize.x, 0.0));
    float d2 = getDepth(uv + vec2(0.0, texelSize.y));
    vec3 v1 = vec3(texelSize.x, 0.0, d1 - centerDepth);
    vec3 v2 = vec3(0.0, texelSize.y, d2 - centerDepth);
    return normalize(cross(v1, v2));
}

void main() {
    vec2 texelSize = 1.0 / screenSize;
    vec4 centerSample = texture(screenTexture, fragTexCoord);
    vec3 center = centerSample.rgb;
    float mask = centerSample.a;
    
    // Sample more neighbors for better quality
    vec3 left   = texture(screenTexture, fragTexCoord - vec2(texelSize.x, 0.0)).rgb;
    vec3 right  = texture(screenTexture, fragTexCoord + vec2(texelSize.x, 0.0)).rgb;
    vec3 up     = texture(screenTexture, fragTexCoord - vec2(0.0, texelSize.y)).rgb;
    vec3 down   = texture(screenTexture, fragTexCoord + vec2(0.0, texelSize.y)).rgb;
    
    // Add diagonal samples for better edge detection
    vec3 upLeft    = texture(screenTexture, fragTexCoord - texelSize).rgb;
    vec3 upRight   = texture(screenTexture, fragTexCoord + vec2(texelSize.x, -texelSize.y)).rgb;
    vec3 downLeft  = texture(screenTexture, fragTexCoord + vec2(-texelSize.x, texelSize.y)).rgb;
    vec3 downRight = texture(screenTexture, fragTexCoord + texelSize).rgb;
    
    // Calculate luminance (brightness) for edge detection
    float centerLuma   = dot(center,    vec3(0.299, 0.587, 0.114));
    float leftLuma     = dot(left,      vec3(0.299, 0.587, 0.114));
    float rightLuma    = dot(right,     vec3(0.299, 0.587, 0.114));
    float upLuma       = dot(up,        vec3(0.299, 0.587, 0.114));
    float downLuma     = dot(down,      vec3(0.299, 0.587, 0.114));
    float upLeftLuma   = dot(upLeft,    vec3(0.299, 0.587, 0.114));
    float upRightLuma  = dot(upRight,   vec3(0.299, 0.587, 0.114));
    float downLeftLuma = dot(downLeft,  vec3(0.299, 0.587, 0.114));
    float downRightLuma= dot(downRight, vec3(0.299, 0.587, 0.114));
    
    // Find the minimum and maximum luminance from all samples
    float minLuma = min(centerLuma, min(min(leftLuma, rightLuma), 
                       min(min(upLuma, downLuma), 
                           min(min(upLeftLuma, upRightLuma), min(downLeftLuma, downRightLuma)))));
    float maxLuma = max(centerLuma, max(max(leftLuma, rightLuma), 
                       max(max(upLuma, downLuma), 
                           max(max(upLeftLuma, upRightLuma), max(downLeftLuma, downRightLuma)))));
    
    // Calculate the contrast range
    float range = maxLuma - minLuma;
    
    vec3 blended = center;
    
    // FXAA logic...
    if (range >= max(0.005, maxLuma * 0.01)) {
        float horizontalGrad = abs(leftLuma - centerLuma) + abs(centerLuma - rightLuma);
        float verticalGrad   = abs(upLuma - centerLuma) + abs(centerLuma - downLuma);
        float diagonalGrad1 = abs(upLeftLuma - centerLuma) + abs(centerLuma - downRightLuma);
        float diagonalGrad2 = abs(upRightLuma - centerLuma) + abs(centerLuma - downLeftLuma);
        bool isHorizontal = (horizontalGrad * 1.2) > (verticalGrad + (diagonalGrad1 + diagonalGrad2) * 0.3);
        float directionConfidence = abs(horizontalGrad - verticalGrad) / (horizontalGrad + verticalGrad + 0.001);
        
        if (directionConfidence > 0.5) {
            vec2 direction = isHorizontal ? vec2(0.0, texelSize.y) : vec2(texelSize.x, 0.0);
            vec3 sample1 = texture(screenTexture, fragTexCoord + direction * 0.5).rgb;
            vec3 sample2 = texture(screenTexture, fragTexCoord - direction * 0.5).rgb;
            vec3 sample3 = texture(screenTexture, fragTexCoord + direction * 1.0).rgb;
            vec3 sample4 = texture(screenTexture, fragTexCoord - direction * 1.0).rgb;
            float blendStrength = clamp(range * 3.0, 0.2, 0.7);
            vec3 edgeAverage = (sample1 + sample2 + sample3 + sample4) / 4.0;
            blended = mix(center, edgeAverage, blendStrength);
        } else {
            vec3 boxSamples = (left + right + up + down + upLeft + upRight + downLeft + downRight) / 8.0;
            float blendStrength = clamp(range * 2.0, 0.3, 0.6);
            blended = mix(center, boxSamples, blendStrength);
        }
    }
    
    // --- STYLIZED AAA POST-STACK ---
    float rawDepth = texture(depthTexture, fragTexCoord).r;
    if (rawDepth < 0.99999) {
        float d = LinearizeDepth(rawDepth);
        
        // 1. Stylized Crease AO
        float dL = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(-texelSize.x, 0.0)).r);
        float dR = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2( texelSize.x, 0.0)).r);
        float dU = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(0.0,  texelSize.y)).r);
        float dD = LinearizeDepth(texture(depthTexture, fragTexCoord + vec2(0.0, -texelSize.y)).r);
        
        float averageDepth = (dL + dR + dU + dD) * 0.25;
        float diff = d - averageDepth;
        
        if (diff > 0.05) {
            float ao = smoothstep(0.05, 0.5, diff);
            blended *= mix(1.0, 0.6, ao);
        }
        
        // 2. Atmospheric Fog
        vec3 skyColor = vec3(0.6, 0.8, 1.0); 
        float fogFactor = smoothstep(50.0, 250.0, d);
        blended = mix(blended, skyColor, fogFactor);
    }
    
    // 3. Vibrance
    float luma = dot(blended, vec3(0.299, 0.587, 0.114));
    float maxColor = max(blended.r, max(blended.g, blended.b));
    float minColor = min(blended.r, min(blended.g, blended.b));
    float sat = maxColor - minColor;
    blended = mix(vec3(luma), blended, 1.0 + (0.4 * (1.0 - sat)));

    // 4. Vignette
    vec2 uv = fragTexCoord * (1.0 - fragTexCoord.yx);
    float vig = uv.x * uv.y * 15.0; 
    vig = pow(vig, 0.15); 
    blended *= vig;
    
    fragColor = vec4(blended, 1.0);
}
