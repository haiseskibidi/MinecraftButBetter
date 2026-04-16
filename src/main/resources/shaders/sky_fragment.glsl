#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec4 uColor;
uniform int uType; // 0=Texture, 1=Procedural

void main() {
    if (uType == 1) {
        // Procedural Circle with smooth glow
        float dist = distance(fragTexCoord, vec2(0.5));
        
        // Hard core + soft falloff
        float alpha = smoothstep(0.5, 0.2, dist);
        
        // Inner core brightness boost
        vec4 color = uColor;
        color.rgb += smoothstep(0.2, 0.0, dist) * 0.5;
        
        fragColor = color * alpha;
    } else {
        // Texture Mode
        vec4 texColor = texture(uTexture, fragTexCoord);
        if (texColor.a < 0.01) discard;
        fragColor = texColor * uColor;
    }
}
