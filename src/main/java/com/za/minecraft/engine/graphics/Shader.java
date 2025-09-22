package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
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
        String vertexSource = loadShaderSource(vertexPath);
        String fragmentSource = loadShaderSource(fragmentPath);
        
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
    
    public void setVector2f(String name, Vector2f vector) {
        glUniform2f(getUniformLocation(name), vector.x, vector.y);
    }
    
    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }
    
    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }
    
    public void setUniform(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }
    
    public void setUniform(String name, Matrix4f matrix) {
        setMatrix4f(name, matrix);
    }
    
    public static void unbind() {
        glUseProgram(0);
    }
    
    private int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }
    
    public void cleanup() {
        glDeleteProgram(programId);
    }
}
