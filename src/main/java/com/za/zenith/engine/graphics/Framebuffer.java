package com.za.zenith.engine.graphics;

import com.za.zenith.utils.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public class Framebuffer {
    private int framebufferId;
    private int colorTextureId;
    private int depthTextureId;
    private int width;
    private int height;
    private final int samples;

    public Framebuffer(int width, int height) {
        this(width, height, 1);
    }

    public Framebuffer(int width, int height, int samples) {
        this.width = width;
        this.height = height;
        this.samples = samples;
        create();
    }

    private void create() {
        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

        if (samples > 1) {
            // Multisampled Color texture
            colorTextureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorTextureId);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGBA8, width, height, true);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, colorTextureId, 0);

            // Multisampled Depth texture
            depthTextureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, depthTextureId);
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_DEPTH_COMPONENT24, width, height, true);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D_MULTISAMPLE, depthTextureId, 0);
        } else {
            // Standard Color texture
            colorTextureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0);

            // Standard Depth texture
            depthTextureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, depthTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);
        }

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            Logger.error("Framebuffer is not complete! Status: " + glCheckFramebufferStatus(GL_FRAMEBUFFER));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        if (samples > 1) glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

        Logger.info("Framebuffer created: %dx%d (Samples: %d)", width, height, samples);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resolveTo(Framebuffer target) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.framebufferId);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target.framebufferId);
        glBlitFramebuffer(0, 0, width, height, 0, 0, target.width, target.height,
                GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindColorTexture() {
        if (samples > 1) {
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorTextureId);
        } else {
            glBindTexture(GL_TEXTURE_2D, colorTextureId);
        }
    }

    public int getColorTextureId() {
        return colorTextureId;
    }

    public int getDepthTextureId() {
        return depthTextureId;
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
        glDeleteTextures(depthTextureId);
    }

    public int getSamples() {
        return samples;
    }
}
