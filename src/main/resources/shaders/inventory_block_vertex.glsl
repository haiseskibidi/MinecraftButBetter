#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 texCoordOrPackedTex;
layout(location = 2) in vec3 normalOrPackedLayers;
layout(location = 3) in vec4 blockTypeOrPackedBlock;
layout(location = 4) in vec4 neighborOrPackedLight;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform bool uIsCompressed;

out vec4 fragTexCoord;
out vec3 fragNormal;
out float blockType;
out float neighborData;

void main() {
    vec4 finalTexCoord;
    vec3 finalNormal;
    float finalBlockType;
    float finalNeighborData;

    if (uIsCompressed) {
        uint packedTex = floatBitsToUint(texCoordOrPackedTex.x);
        finalTexCoord = vec4(float(packedTex & 0xFFFFu) / 65535.0, float((packedTex >> 16) & 0xFFFFu) / 65535.0, 0.0, -1.0);

        uint packedLayers = floatBitsToUint(normalOrPackedLayers.x);
        finalTexCoord.z = float(packedLayers & 0xFFFu);
        finalTexCoord.w = float((packedLayers >> 12) & 0xFFFu) - 1.0;
        
        uint nIndex = (packedLayers >> 24) & 0x7u;
        vec3 faceNormals[6] = vec3[](vec3(0,0,1), vec3(0,0,-1), vec3(1,0,0), vec3(-1,0,0), vec3(0,1,0), vec3(0,-1,0));
        if (nIndex < 6u) finalNormal = faceNormals[nIndex];
        else finalNormal = vec3(0, 1, 0);

        uint packedBlock = floatBitsToUint(blockTypeOrPackedBlock.x);
        int bType = int(packedBlock & 0xFFFFu);
        if ((bType & 0x8000) != 0) bType |= 0xFFFF0000;
        finalBlockType = float(bType);
        finalNeighborData = float((packedBlock >> 16) & 0x3Fu);
    } else {
        finalTexCoord = texCoordOrPackedTex;
        finalNormal = normalOrPackedLayers;
        finalBlockType = blockTypeOrPackedBlock.x;
        finalNeighborData = neighborOrPackedLight.x;
    }

    fragTexCoord = finalTexCoord;
    
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    fragNormal = normalize(normalMatrix * finalNormal);
    
    blockType = finalBlockType;
    neighborData = finalNeighborData;
    gl_Position = projection * view * model * vec4(position, 1.0);
}
