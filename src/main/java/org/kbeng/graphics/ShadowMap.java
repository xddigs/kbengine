package org.kbeng.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * ShadowMap provides shadow map capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class ShadowMap {
    private final int width;
    private final int height;
    private final int framebuffer;
    private final int depthTexture;

    /**
     * Creates a new {@code ShadowMap} instance.
     * @param width the {@code int} supplied as {@code width}
     * @param height the {@code int} supplied as {@code height}
     */
    public ShadowMap(int width, int height) {
        this.width = width;
        this.height = height;

        framebuffer = glGenFramebuffers();
        depthTexture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, depthTexture);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height,
                0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Shadow framebuffer is incomplete");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Binds this object to the active runtime context.
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Unbinds this object from the active runtime context.
     * @param windowWidth the {@code int} supplied as {@code windowWidth}
     * @param windowHeight the {@code int} supplied as {@code windowHeight}
     */
    public void unbind(int windowWidth, int windowHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight);
    }

    /**
     * Returns the depth texture.
     * @return {@code int}; the depth texture
     */
    public int getDepthTexture() {
        return depthTexture;
    }

    /**
     * Returns the framebuffer.
     * @return {@code int}; the framebuffer
     */
    public int getFramebuffer() {
        return framebuffer;
    }

    /**
     * Returns the width.
     * @return {@code int}; the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * @return {@code int}; the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteFramebuffers(framebuffer);
        glDeleteTextures(depthTexture);
    }
}
