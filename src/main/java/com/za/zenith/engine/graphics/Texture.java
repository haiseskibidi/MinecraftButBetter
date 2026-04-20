package com.za.zenith.engine.graphics;

import com.za.zenith.utils.Logger;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    private final int textureId;
    private final int width;
    private final int height;
    
    public Texture(String path, boolean flipVertically, boolean useMipmap) {
        try (MemoryStack stack = stackPush()) {
            stbi_set_flip_vertically_on_load(flipVertically);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer image;
            
            boolean isStbAllocated = false;
            
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
                if (image != null) isStbAllocated = true;
            } else {
                // Fallback: загружаем как файл (для разработки)
                image = stbi_load(path, w, h, channels, 4);
                if (image != null) isStbAllocated = true;
            }
            
            if (image == null) {
                Logger.error("Failed to load texture: %s - %s", path, stbi_failure_reason());
                image = generateMissingTexture(stack, w, h);
                isStbAllocated = false;
            }
            
            this.width = w.get(0);
            this.height = h.get(0);
            
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            
            if (useMipmap) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            
            if (useMipmap) {
                glGenerateMipmap(GL_TEXTURE_2D);
            }
            
            if (isStbAllocated) {
                stbi_image_free(image);
            }
            
            Logger.info("Texture loaded: %s (%dx%d)", path, width, height);
        } catch (IOException e) {
            Logger.error("IOException while loading texture: %s", e, path);
            // Instead of throwing, we could also use generateMissingTexture here if we had a stack...
            // But for now, let it fail or handle better.
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }
    
    public Texture(String path) {
        this(path, true, true);
    }

    public Texture(int width, int height, byte[] rgbaData) {
        this.width = width;
        this.height = height;

        ByteBuffer buffer = ByteBuffer.allocateDirect(rgbaData.length);
        buffer.put(rgbaData).flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        Logger.info("Procedural texture created (%dx%d)", width, height);
    }
    
    public Texture() {
        this.width = 2;
        this.height = 2;
        
        byte[] pixels = new byte[width * height * 4];
        // 0,0 - Purple
        pixels[0] = (byte)255; pixels[1] = 0; pixels[2] = (byte)255; pixels[3] = (byte)255;
        // 1,0 - Black
        pixels[4] = 0; pixels[5] = 0; pixels[6] = 0; pixels[7] = (byte)255;
        // 0,1 - Black
        pixels[8] = 0; pixels[9] = 0; pixels[10] = 0; pixels[11] = (byte)255;
        // 1,1 - Purple
        pixels[12] = (byte)255; pixels[13] = 0; pixels[14] = (byte)255; pixels[15] = (byte)255;
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length);
        buffer.put(pixels).flip();
        
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        Logger.info("Missing texture stub created (%dx%d)", width, height);
    }
    
    private ByteBuffer generateMissingTexture(MemoryStack stack, IntBuffer w, IntBuffer h) {
        w.put(0, 2);
        h.put(0, 2);
        
        ByteBuffer buffer = stack.malloc(2 * 2 * 4);
        // Pixel (0,0) - Purple
        buffer.put(0, (byte) 255); buffer.put(1, (byte) 0); buffer.put(2, (byte) 255); buffer.put(3, (byte) 255);
        // Pixel (1,0) - Black
        buffer.put(4, (byte) 0); buffer.put(5, (byte) 0); buffer.put(6, (byte) 0); buffer.put(7, (byte) 255);
        // Pixel (0,1) - Black
        buffer.put(8, (byte) 0); buffer.put(9, (byte) 0); buffer.put(10, (byte) 0); buffer.put(11, (byte) 255);
        // Pixel (1,1) - Purple
        buffer.put(12, (byte) 255); buffer.put(13, (byte) 0); buffer.put(14, (byte) 255); buffer.put(15, (byte) 255);
        
        return buffer;
    }
    
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void update(ByteBuffer data) {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
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


