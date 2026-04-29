#version 330 core

#include "include/global_data.glsl"

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;

out vec2 fragTexCoord;

uniform vec3 uOffset; // Position on the sky sphere
uniform float uScale;

void main() {
    fragTexCoord = texCoord;
    
    // Transform the celestial body's direction into view space
    // mat3(gView) removes translation, leaving only rotation
    vec3 viewDir = mat3(gView) * normalize(uOffset);
    
    // In view space, the camera is at (0,0,0) looking at -Z.
    // The quad 'position' is in the XY plane, which is exactly the plane facing the camera.
    // We place the center at viewDir * 100 and add the quad offsets.
    vec3 viewPos = (viewDir * 100.0) + (position * uScale);
    
    gl_Position = gProjection * vec4(viewPos, 1.0);
    
    // Force depth to the maximum value so it's always behind objects
    gl_Position.z = gl_Position.w; 
}
