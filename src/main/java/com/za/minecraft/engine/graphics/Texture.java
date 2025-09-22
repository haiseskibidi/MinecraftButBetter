package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    private final int textureId;
    private final int width;
    private final int height;
    
    public Texture(String path) {
        try (MemoryStack stack = stackPush()) {
            stbi_set_flip_vertically_on_load(true);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer image;
            
            // Пробуем загрузить как ресурс из ClassPath (для JAR)
            String resourcePath = path.replace("src/main/resources/", "");
            var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                byte[] imageData = inputStream.readAllBytes();
                ByteBuffer imageBuffer = stack.malloc(imageData.length);
                imageBuffer.put(imageData);
                imageBuffer.flip();
                image = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
                inputStream.close();
            } else {
                // Fallback: загружаем как файл (для разработки)
                image = stbi_load(path, w, h, channels, 4);
            }
            
            if (image == null) {
                Logger.error("Failed to load texture: %s - %s", path, stbi_failure_reason());
                image = createWhiteTexture(stack, w, h);
            }
            
            this.width = w.get(0);
            this.height = h.get(0);
            
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
            
            stbi_image_free(image);
            
            Logger.info("Texture loaded: %s (%dx%d)", path, width, height);
        } catch (IOException e) {
            Logger.error("IOException while loading texture: %s", e, path);
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }
    
    public Texture() {
        this.width = 16;
        this.height = 16;
        
        byte[] pixels = new byte[width * height * 4];
        for (int i = 0; i < pixels.length; i += 4) {
            pixels[i] = (byte) 255;     // R
            pixels[i + 1] = (byte) 255; // G
            pixels[i + 2] = (byte) 255; // B
            pixels[i + 3] = (byte) 255; // A
        }
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length);
        buffer.put(pixels).flip();
        
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        Logger.info("White texture created (%dx%d)", width, height);
    }
    
    private ByteBuffer createWhiteTexture(MemoryStack stack, IntBuffer w, IntBuffer h) {
        w.put(0, 16);
        h.put(0, 16);
        
        ByteBuffer buffer = stack.malloc(16 * 16 * 4);
        for (int i = 0; i < 16 * 16 * 4; i += 4) {
            buffer.put(i, (byte) 255);
            buffer.put(i + 1, (byte) 255);
            buffer.put(i + 2, (byte) 255);
            buffer.put(i + 3, (byte) 255);
        }
        
        return buffer;
    }
    
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    public void cleanup() {
        glDeleteTextures(textureId);
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}
