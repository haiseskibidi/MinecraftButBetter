package com.za.minecraft.engine.core;

import com.za.minecraft.utils.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;
    
    private long windowHandle;
    private int width;
    private int height;
    private String title;
    private boolean vSync;
    
    public Window(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
    }
    
    public Window(String title) {
        this(title, DEFAULT_WIDTH, DEFAULT_HEIGHT, true);
    }
    
    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_FOCUSED, GL_TRUE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GL_TRUE);
        glfwWindowHint(GLFW_FLOATING, GL_TRUE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GL_FALSE);
        
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        glfwMakeContextCurrent(windowHandle);
        
        if (vSync) {
            glfwSwapInterval(1);
        }
        
        glfwShowWindow(windowHandle);
        
        // Принудительно получаем фокус и выводим окно поверх всех других
        glfwFocusWindow(windowHandle);
        glfwRequestWindowAttention(windowHandle);
        
        // Дополнительная попытка получить фокус через небольшую задержку
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        glfwFocusWindow(windowHandle);
        
        GL.createCapabilities();
        
        glClearColor(0.6f, 0.8f, 1.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        Logger.info("OpenGL Version: %s", glGetString(GL_VERSION));
        Logger.info("Window initialized: %dx%d", width, height);
    }
    
    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }
    
    public void cleanup() {
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }
    
    public long getWindowHandle() {
        return windowHandle;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public float getAspectRatio() {
        return (float) width / height;
    }
    
    public void setTitle(String title) {
        glfwSetWindowTitle(windowHandle, title);
    }
    
    public boolean isMouseButtonPressed(int button) {
        return glfwGetMouseButton(windowHandle, button) == GLFW_PRESS;
    }
}
