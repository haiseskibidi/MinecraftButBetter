#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord;

uniform vec4 scale;    // scaleX, scaleY, unused, unused
uniform vec4 position_offset; // offsetX, offsetY, unused, unused
uniform vec4 uvOffset; // uvX, uvY, unused, unused
uniform vec4 uvScale;  // uvScaleX, uvScaleY, unused, unused

out vec2 fragTexCoord;

void main() {
    // Применяем масштаб и смещение к вершинам
    vec2 scaledPos = position * scale.xy + position_offset.xy;
    
    // Применяем UV смещение и масштаб
    fragTexCoord = texCoord * uvScale.xy + uvOffset.xy;
    
    gl_Position = vec4(scaledPos, 0.0, 1.0);
}