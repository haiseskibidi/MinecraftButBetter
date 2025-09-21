package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class Framebuffer {
    private int framebufferId;
    private int colorTextureId;
    private int depthRenderbufferId;
    private int width;
    private int height;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        create();
    }

    private void create() {
        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

        colorTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0);

        depthRenderbufferId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbufferId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbufferId);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            Logger.error("Framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        Logger.info("Framebuffer created: %dx%d", width, height);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindColorTexture() {
        glBindTexture(GL_TEXTURE_2D, colorTextureId);
    }

    public int getColorTextureId() {
        return colorTextureId;
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        
        cleanup();
        width = newWidth;
        height = newHeight;
        create();
    }

    public void cleanup() {
        glDeleteFramebuffers(framebufferId);
        glDeleteTextures(colorTextureId);
        glDeleteRenderbuffers(depthRenderbufferId);
    }
}
