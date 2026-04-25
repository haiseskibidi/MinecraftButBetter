package com.za.zenith.engine.graphics;

import com.za.zenith.utils.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Shader {
    private final int programId;
    
    public Shader(String vertexPath, String fragmentPath) {
        String vertexSource = resolveIncludes(loadShaderSource(vertexPath), vertexPath);
        String fragmentSource = resolveIncludes(loadShaderSource(fragmentPath), fragmentPath);
        
        int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
        
        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);
        
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            Logger.error("Shader program linking failed: %s", glGetProgramInfoLog(programId));
            throw new RuntimeException("Shader program linking failed");
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        Logger.info("Shader program created successfully");
    }

    private String resolveIncludes(String source, String currentPath) {
        StringBuilder sb = new StringBuilder();
        String[] lines = source.split("\n");
        
        for (String line : lines) {
            if (line.trim().startsWith("#include")) {
                String includePath = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                // Путь считается относительно src/main/resources/shaders/
                String fullPath = "src/main/resources/shaders/" + includePath;
                sb.append(resolveIncludes(loadShaderSource(fullPath), fullPath)).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
    
    private String loadShaderSource(String path) {
        try {
            // Убираем "src/main/resources/" из пути для ClassLoader
            String resourcePath = path.replace("src/main/resources/", "");
            
            // Пробуем загрузить как ресурс из ClassPath (для JAR)
            var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                return new String(inputStream.readAllBytes());
            }
            
            // Fallback: загружаем как файл (для разработки)
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            Logger.error("Failed to load shader: %s", e, path);
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }
    
    private int compileShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            Logger.error("Shader compilation failed: %s", glGetShaderInfoLog(shader));
            throw new RuntimeException("Shader compilation failed");
        }
        
        return shader;
    }
    
    public void use() {
        glUseProgram(programId);
    }
    
    public void setMatrix4f(String name, Matrix4f matrix) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getUniformLocation(name), false, buffer);
        }
    }
    
    public void setVector3f(String name, Vector3f vector) {
        glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z);
    }
    
    public void setVector4f(String name, Vector4f vector) {
        glUniform4f(getUniformLocation(name), vector.x, vector.y, vector.z, vector.w);
    }

    public void setVector4fv(String name, float[] values) {
        glUniform4fv(getUniformLocation(name), values);
    }

    public void setFloatArray(String name, float[] values) {
        glUniform1fv(getUniformLocation(name), values);
    }
    
    public void setVector2f(String name, Vector2f vector) {
        glUniform2f(getUniformLocation(name), vector.x, vector.y);
    }
    
    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }
    
    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }
    
    public void setBoolean(String name, boolean value) {
        glUniform1i(getUniformLocation(name), value ? 1 : 0);
    }
    
    public void setUniform(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }
    
    public void setUniform(String name, Matrix4f matrix) {
        setMatrix4f(name, matrix);
    }
    
    private final java.util.Map<String, int[]> lightUniformCache = new java.util.HashMap<>();
    private static final int LIGHT_STRUCT_SIZE = 7; // type, pos, dir, color, intensity, radius, spot

    public void setLights(String arrayName, java.util.List<com.za.zenith.world.lighting.LightSource> lights) {
        int count = Math.min(lights.size(), com.za.zenith.world.lighting.LightManager.MAX_DYNAMIC_LIGHTS);
        setInt("uLightCount", count);
        
        int[] locations = lightUniformCache.get(arrayName);
        if (locations == null) {
            locations = new int[com.za.zenith.world.lighting.LightManager.MAX_DYNAMIC_LIGHTS * LIGHT_STRUCT_SIZE];
            for (int i = 0; i < com.za.zenith.world.lighting.LightManager.MAX_DYNAMIC_LIGHTS; i++) {
                String prefix = arrayName + "[" + i + "].";
                locations[i * LIGHT_STRUCT_SIZE + 0] = glGetUniformLocation(programId, prefix + "type");
                locations[i * LIGHT_STRUCT_SIZE + 1] = glGetUniformLocation(programId, prefix + "position");
                locations[i * LIGHT_STRUCT_SIZE + 2] = glGetUniformLocation(programId, prefix + "direction");
                locations[i * LIGHT_STRUCT_SIZE + 3] = glGetUniformLocation(programId, prefix + "color");
                locations[i * LIGHT_STRUCT_SIZE + 4] = glGetUniformLocation(programId, prefix + "intensity");
                locations[i * LIGHT_STRUCT_SIZE + 5] = glGetUniformLocation(programId, prefix + "radius");
                locations[i * LIGHT_STRUCT_SIZE + 6] = glGetUniformLocation(programId, prefix + "spotAngle");
            }
            lightUniformCache.put(arrayName, locations);
        }

        for (int i = 0; i < count; i++) {
            com.za.zenith.world.lighting.LightSource light = lights.get(i);
            int base = i * LIGHT_STRUCT_SIZE;
            glUniform1i(locations[base + 0], light.data.type.ordinal());
            glUniform3f(locations[base + 1], light.position.x, light.position.y, light.position.z);
            glUniform3f(locations[base + 2], light.direction.x, light.direction.y, light.direction.z);
            glUniform3f(locations[base + 3], light.data.color.x, light.data.color.y, light.data.color.z);
            glUniform1f(locations[base + 4], light.data.intensity);
            glUniform1f(locations[base + 5], light.data.radius);
            glUniform1f(locations[base + 6], (float)Math.cos(Math.toRadians(light.data.spotAngle)));
        }
    }

    public static void unbind() {
        glUseProgram(0);
    }
    
    private final java.util.Map<String, Integer> uniformLocationCache = new java.util.HashMap<>();
    
    private int getUniformLocation(String name) {
        Integer location = uniformLocationCache.get(name);
        if (location == null) {
            location = glGetUniformLocation(programId, name);
            uniformLocationCache.put(name, location);
        }
        return location;
    }
    
    public void cleanup() {
        glDeleteProgram(programId);
    }
}


