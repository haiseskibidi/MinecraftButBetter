// Stylized AAA Lighting: Cinematic Survival Toon-shading

vec3 calculateLighting(vec3 normal, vec3 lightDir, vec3 lightCol, vec3 ambient) {
    float diffuse = max(dot(normal, -lightDir), 0.0);
    
    // Weighted toon-shading steps
    float toonDiffuse = smoothstep(0.15, 0.25, diffuse) * 0.6 + smoothstep(0.6, 0.7, diffuse) * 0.4;

    // Moody bluish-grey ambient
    vec3 toonAmbient = ambient * vec3(0.85, 0.88, 0.95);
    
    return toonAmbient + lightCol * toonDiffuse;
}
