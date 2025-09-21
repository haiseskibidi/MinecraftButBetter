#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D screenTexture;
uniform vec2 screenSize;

void main() {
    vec2 texelSize = 1.0 / screenSize;
    
    // Sample more neighbors for better quality
    vec3 center = texture(screenTexture, fragTexCoord).rgb;
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
    
    // Very aggressive FXAA - extremely low threshold to catch all edges
    if (range < max(0.005, maxLuma * 0.01)) {
        fragColor = vec4(center, 1.0);
        return;
    }
    
    // Better edge direction detection - fix for forward/backward movement moire
    float horizontalGrad = abs(leftLuma - centerLuma) + abs(centerLuma - rightLuma);
    float verticalGrad   = abs(upLuma - centerLuma) + abs(centerLuma - downLuma);
    
    // Add diagonal gradients for better detection
    float diagonalGrad1 = abs(upLeftLuma - centerLuma) + abs(centerLuma - downRightLuma);
    float diagonalGrad2 = abs(upRightLuma - centerLuma) + abs(centerLuma - downLeftLuma);
    
    // Improved direction detection with bias towards vertical processing for movement artifacts
    bool isHorizontal = (horizontalGrad * 1.2) > (verticalGrad + (diagonalGrad1 + diagonalGrad2) * 0.3);
    
    // Check if edge direction is ambiguous (similar gradients in both directions)
    float directionConfidence = abs(horizontalGrad - verticalGrad) / (horizontalGrad + verticalGrad + 0.001);
    
    vec3 blended;
    
    if (directionConfidence > 0.5) {
        // Strong directional edge - use directional sampling
        vec2 direction = isHorizontal ? vec2(0.0, texelSize.y) : vec2(texelSize.x, 0.0);
        
        vec3 sample1 = texture(screenTexture, fragTexCoord + direction * 0.5).rgb;
        vec3 sample2 = texture(screenTexture, fragTexCoord - direction * 0.5).rgb;
        vec3 sample3 = texture(screenTexture, fragTexCoord + direction * 1.0).rgb;
        vec3 sample4 = texture(screenTexture, fragTexCoord - direction * 1.0).rgb;
        
        float blendStrength = clamp(range * 3.0, 0.2, 0.7);
        vec3 edgeAverage = (sample1 + sample2 + sample3 + sample4) / 4.0;
        blended = mix(center, edgeAverage, blendStrength);
    } else {
        // Ambiguous edge direction - use box filter for stability during movement
        vec3 boxSamples = (left + right + up + down + upLeft + upRight + downLeft + downRight) / 8.0;
        float blendStrength = clamp(range * 2.0, 0.3, 0.6);
        blended = mix(center, boxSamples, blendStrength);
    }
    
    fragColor = vec4(blended, 1.0);
}
