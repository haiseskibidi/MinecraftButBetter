#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord;

uniform vec4 scale;
uniform vec4 position_offset;
uniform float uRotation = 0.0;

out vec2 fragTexCoord;

void main() {
    float cosR = cos(uRotation);
    float sinR = sin(uRotation);
    mat2 rot = mat2(cosR, -sinR, sinR, cosR);
    
    vec2 rotatedPos = rot * position;
    vec2 scaledPos = rotatedPos * scale.xy + position_offset.xy;
    
    fragTexCoord = texCoord;
    gl_Position = vec4(scaledPos, 0.0, 1.0);
}
